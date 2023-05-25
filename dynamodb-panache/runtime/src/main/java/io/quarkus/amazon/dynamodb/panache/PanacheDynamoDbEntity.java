package io.quarkus.amazon.dynamodb.panache;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

public abstract class PanacheDynamoDbEntity extends PanacheDynamoDbEntityBase {
    private String id;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
