package io.quarkus.amazon.dynamodb.panache.common.runtime;

import jakarta.enterprise.inject.Default;

import io.quarkus.amazon.dynamodb.panache.common.DynamoDbEntity;
import io.quarkus.arc.Arc;

public final class BeanUtils {

    private BeanUtils() {
    }

    public static <T> T clientFromArc(DynamoDbEntity entity,
            Class<T> clientClass) {
        T dynamoEnhancedClient = Arc.container()
                .instance(clientClass, Default.Literal.INSTANCE)
                .get();
        return dynamoEnhancedClient;
    }
}
