package io.quarkus.amazon.dynamodb.panache;

import io.quarkus.amazon.dynamodb.panache.common.DynamoDbEntity;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbEntity
@DynamoDbBean
public class DynamoDbEntityEntity extends PanacheDynamoDbEntity {

}
