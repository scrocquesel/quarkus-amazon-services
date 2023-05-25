package io.quarkus.amazon.dynamodb.panache.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor;

public class PanacheDynamoDbRepositoryEnhancer extends PanacheRepositoryEnhancer {
    private final TypeBundle typeBundle;

    public PanacheDynamoDbRepositoryEnhancer(IndexView index, TypeBundle typeBundle) {
        super(index);
        this.typeBundle = typeBundle;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRepositoryClassOperationGenerationVisitor(className, outputClassVisitor, indexView, typeBundle);
    }

}
