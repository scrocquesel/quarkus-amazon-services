package io.quarkus.amazon.dynamodb.panache.runtime;

import static io.quarkus.amazon.dynamodb.panache.runtime.JavaDynamoDbOperations.INSTANCE;

import io.quarkus.panache.common.impl.GenerateBridge;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public interface PanacheDynamoDbRepositoryBase<Entity, Id> {

    /**
     * Allow to access the underlying DynamoDb Enhanced table
     */
    @GenerateBridge
    default DynamoDbTable<Entity> table() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying DynamoDb Enhanced client
     */
    @GenerateBridge
    default DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        throw INSTANCE.implementationInjectionMissing();
    }
}
