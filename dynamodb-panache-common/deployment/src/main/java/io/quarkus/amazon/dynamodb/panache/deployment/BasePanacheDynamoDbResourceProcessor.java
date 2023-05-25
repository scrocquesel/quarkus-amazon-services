package io.quarkus.amazon.dynamodb.panache.deployment;

import static io.quarkus.deployment.util.JandexUtil.resolveTypeParameters;
import static io.quarkus.panache.common.deployment.PanacheConstants.META_INF_PANACHE_ARCHIVE_MARKER;
import static org.jboss.jandex.DotName.createSimple;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;

import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

public abstract class BasePanacheDynamoDbResourceProcessor {
    public static final DotName DYNAMODB_PARTITION_KEY = createSimple(DynamoDbPartitionKey.class.getName());
    public static final DotName DYNAMODB_ENTITY = createSimple(
            io.quarkus.amazon.dynamodb.panache.common.DynamoDbEntity.class.getName());

    @BuildStep
    public void buildImperative(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(PanacheMethodCustomizerBuildItem::getMethodCustomizer).collect(Collectors.toList());

        MetamodelInfo modelInfo = new MetamodelInfo();
        processTypes(index, transformers, reflectiveClass, reflectiveHierarchy, getImperativeTypeBundle(),
                createRepositoryEnhancer(index, methodCustomizers),
                createEntityEnhancer(index, methodCustomizers, modelInfo),
                modelInfo, requireClientProducer);
    }

    protected abstract PanacheEntityEnhancer createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers, MetamodelInfo modelInfo);

    protected abstract PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers);

    @BuildStep
    protected PanacheEntityClassesBuildItem findEntityClasses(List<PanacheDynamoDbEntityClassBuildItem> entityClasses) {
        if (!entityClasses.isEmpty()) {
            Set<String> ret = new HashSet<>();
            for (PanacheDynamoDbEntityClassBuildItem entityClass : entityClasses) {
                ret.add(entityClass.get().name().toString());
            }
            return new PanacheEntityClassesBuildItem(ret);
        }
        return null;
    }

    protected abstract DynamoDbTypeBundle getImperativeTypeBundle();

    protected void processEntities(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            PanacheEntityEnhancer entityEnhancer, DynamoDbTypeBundle typeBundle,
            MetamodelInfo modelInfo, BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheDynamoDbEntity if we ask for subtypes of PanacheDynamoDbEntityBase
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownSubclasses(typeBundle.entityBase().dotName())) {
            if (classInfo.name().equals(typeBundle.entity().dotName())) {
                continue;
            }
            if (modelClasses.add(classInfo.name().toString()))
                modelInfo.addEntityModel(createEntityModel(classInfo));
        }
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownSubclasses(typeBundle.entity().dotName())) {
            if (modelClasses.add(classInfo.name().toString()))
                modelInfo.addEntityModel(createEntityModel(classInfo));
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, entityEnhancer));

            //register for reflection entity classes
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(modelClass).fields().methods().build());
        }

        replaceFieldAccesses(transformers, modelInfo);

        if (!modelClasses.isEmpty()) {
            requireClientProducer.produce(typeBundle.clientRequirement());
        }
    }

    private void replaceFieldAccesses(BuildProducer<BytecodeTransformerBuildItem> transformers, MetamodelInfo modelInfo) {
        Set<String> entitiesWithPublicFields = modelInfo.getEntitiesWithPublicFields();
        if (entitiesWithPublicFields.isEmpty()) {
            // There are no public fields to be accessed in the first place.
            return;
        }

        Set<String> entityClassNamesInternal = new HashSet<>();
        for (String entityClassName : entitiesWithPublicFields) {
            entityClassNamesInternal.add(entityClassName.replace(".", "/"));
        }

        PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelInfo);
        QuarkusClassLoader tccl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        Set<String> produced = new HashSet<>();

        for (ClassPathElement i : tccl.getElementsWithResource(META_INF_PANACHE_ARCHIVE_MARKER)) {
            for (String res : i.getProvidedResources()) {
                if (res.endsWith(".class")) {
                    String cn = res.replace("/", ".").substring(0, res.length() - 6);
                    if (produced.contains(cn)) {
                        continue;
                    }
                    produced.add(cn);
                    transformers.produce(
                            new BytecodeTransformerBuildItem(cn, panacheFieldAccessEnhancer, entityClassNamesInternal));
                }
            }
        }
    }

    private EntityModel createEntityModel(ClassInfo classInfo) {
        EntityModel entityModel = new EntityModel(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
        }
        return entityModel;
    }

    protected void processRepositories(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            PanacheRepositoryEnhancer repositoryEnhancer, TypeBundle typeBundle) {

        Set<String> daoClasses = new HashSet<>();
        Set<Type> daoTypeParameters = new HashSet<>();
        DotName dotName = typeBundle.repositoryBase().dotName();
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownImplementors(dotName)) {
            // Skip PanacheDynamoDbRepository and abstract repositories
            if (classInfo.name().equals(typeBundle.repository().dotName()) || repositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    resolveTypeParameters(classInfo.name(), typeBundle.repositoryBase().dotName(), index.getComputingIndex()));
        }
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownImplementors(typeBundle.repository().dotName())) {
            if (repositoryEnhancer.skipRepository(classInfo)) {
                continue;
            }
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    resolveTypeParameters(classInfo.name(), typeBundle.repositoryBase().dotName(), index.getComputingIndex()));
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, repositoryEnhancer));
        }

        for (Type parameterType : daoTypeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder().type(parameterType).build());
        }
    }

    protected void processTypes(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            DynamoDbTypeBundle typeBundle, PanacheRepositoryEnhancer repositoryEnhancer,
            PanacheEntityEnhancer entityEnhancer, MetamodelInfo modelInfo,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {
        processRepositories(index, transformers, reflectiveHierarchy,
                repositoryEnhancer, typeBundle);
        processEntities(index, transformers, reflectiveClass,
                entityEnhancer, typeBundle, modelInfo, requireClientProducer);
    }

    @BuildStep
    protected ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @DynamoDbPartitionKey) when extending PanacheMongoEntity
        for (AnnotationInstance annotationInstance : index.getComputingIndex().getAnnotations(DYNAMODB_PARTITION_KEY)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getComputingIndex(), info,
                    getImperativeTypeBundle().entity().dotName())) {
                BuildException be = new BuildException("You provide a DynamoDb partition key via @DynamoDbPartitionKey inside '"
                        + info.name() +
                        "' but one is already provided by PanacheDynamoDbEntity, " +
                        "your class should extend PanacheDynamoDbEntityBase instead, or use the id provided by PanacheDynamoDbEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }
}
