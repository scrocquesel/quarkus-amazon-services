package io.quarkus.amazon.dynamodb.panache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamoDbEntity {
    /**
     * The name of the table (if not set the name of the entity class will be used)
     */
    String table() default "";
}
