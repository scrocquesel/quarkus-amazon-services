package io.quarkus.amazon.dynamodb.panache.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheDynamoDbResourceProcessor extends BasePanacheDynamoDbResourceProcessor {
    private static final String FEATURE = "amazon-dynamodb-panache";
    public static final ImperativeTypeBundle IMPERATIVE_TYPE_BUNDLE = new ImperativeTypeBundle();

    protected ImperativeTypeBundle getImperativeTypeBundle() {
        return IMPERATIVE_TYPE_BUNDLE;
    }

    @Override
    public PanacheEntityEnhancer createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers, MetamodelInfo modelInfo) {
        return new PanacheDynamoDbEntityEnhancer(index.getIndex(), methodCustomizers, getImperativeTypeBundle(), modelInfo);
    }

    @Override
    public PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new PanacheDynamoDbRepositoryEnhancer(index.getIndex(), getImperativeTypeBundle());
    }

    @BuildStep
    protected FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FEATURE);
    }
}
