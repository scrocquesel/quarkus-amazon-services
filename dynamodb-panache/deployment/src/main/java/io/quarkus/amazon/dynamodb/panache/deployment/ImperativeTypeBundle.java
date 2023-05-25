package io.quarkus.amazon.dynamodb.panache.deployment;

import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.dynamodb.panache.PanacheDynamoDbEntity;
import io.quarkus.amazon.dynamodb.panache.PanacheDynamoDbEntityBase;
import io.quarkus.amazon.dynamodb.panache.PanacheQuery;
import io.quarkus.amazon.dynamodb.panache.runtime.JavaDynamoDbOperations;
import io.quarkus.amazon.dynamodb.panache.runtime.PanacheDynamoDbRepository;
import io.quarkus.amazon.dynamodb.panache.runtime.PanacheDynamoDbRepositoryBase;
import io.quarkus.panache.common.deployment.ByteCodeType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ImperativeTypeBundle implements DynamoDbTypeBundle {

    private static final DotName DYNAMODB_CLIENT = DotName.createSimple(DynamoDbClient.class.getName());

    @Override
    public ByteCodeType entity() {
        return new ByteCodeType(PanacheDynamoDbEntity.class);
    }

    @Override
    public ByteCodeType entityBase() {
        return new ByteCodeType(PanacheDynamoDbEntityBase.class);
    }

    @Override
    public ByteCodeType operations() {
        return new ByteCodeType(JavaDynamoDbOperations.class);
    }

    @Override
    public ByteCodeType queryType() {
        return new ByteCodeType(PanacheQuery.class);
    }

    @Override
    public ByteCodeType repository() {
        return new ByteCodeType(PanacheDynamoDbRepository.class);
    }

    @Override
    public ByteCodeType repositoryBase() {
        return new ByteCodeType(PanacheDynamoDbRepositoryBase.class);
    }

    @Override
    public RequireAmazonClientBuildItem clientRequirement() {
        return new RequireAmazonClientBuildItem(Optional.of(DYNAMODB_CLIENT), Optional.empty());
    }
}
