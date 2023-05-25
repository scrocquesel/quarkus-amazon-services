package io.quarkus.amazon.dynamodb.panache.deployment;

import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.panache.common.deployment.TypeBundle;

public interface DynamoDbTypeBundle extends TypeBundle {

    RequireAmazonClientBuildItem clientRequirement();

}
