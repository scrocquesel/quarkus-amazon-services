package io.quarkus.amazon.dynamodb.panache.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a regular Panache entity class.
 */
public final class PanacheDynamoDbEntityClassBuildItem extends MultiBuildItem {

    private final ClassInfo entityClass;

    public PanacheDynamoDbEntityClassBuildItem(ClassInfo entityClass) {
        this.entityClass = entityClass;
    }

    public ClassInfo get() {
        return entityClass;
    }

}
