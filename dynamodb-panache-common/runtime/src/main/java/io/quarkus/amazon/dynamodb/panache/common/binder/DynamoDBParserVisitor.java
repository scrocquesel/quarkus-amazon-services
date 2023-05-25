package io.quarkus.amazon.dynamodb.panache.common.binder;

import java.util.Map;

import io.quarkus.panacheql.internal.HqlParserBaseVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

// TODO : collect with Jandex known indexed field, translate the first field that matches an index to a key, the rest will be convert to an expression
public class DynamoDBParserVisitor extends HqlParserBaseVisitor<QueryConditional> {

    public DynamoDBParserVisitor(Map<String, Object> parameterMaps) {
    }

    @Override
    protected QueryConditional defaultResult() {
        return QueryConditional.keyEqualTo(builder -> builder.partitionValue("test"));
    }

    @Override
    protected QueryConditional aggregateResult(QueryConditional aggregate, QueryConditional nextResult) {
        return aggregate;
    }

}
