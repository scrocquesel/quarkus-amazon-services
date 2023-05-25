package io.quarkus.amazon.dynamodb.panache.common.binder;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import io.quarkus.panacheql.internal.HqlLexer;
import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class PanacheQlQueryBinder {
    public static QueryConditional bindQuery(Class<?> clazz, String query, Object[] params) {
        //shorthand query
        if (params.length == 1 && query.indexOf('?') == -1) {
            // TODO : collect with Jandex known indexed field, if query matches one, use a key, otherwise build an expression
            if(params[0] == null) {
                return QueryConditional.keyEqualTo(builder -> builder.partitionValue(AttributeValues.nullAttributeValue()));
            }
            else if (params[0] instanceof String){
                return QueryConditional.keyEqualTo(builder -> builder.partitionValue(AttributeValues.stringValue((String)params[0])));
            }            
        }

        //classic query
        Map<String, Object> parameterMaps = new HashMap<>();
        for (int i = 1; i <= params.length; i++) {
            String bindParamsKey = "?" + i;
            parameterMaps.put(bindParamsKey, params[i - 1]);
        }

        return prepareQuery(query, parameterMaps);
    }

    public static QueryConditional bindQuery(Class<?> clazz, String query, Map<String, Object> params) {

        Map<String, Object> parameterMaps = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String bindParamsKey = ":" + entry.getKey();
            parameterMaps.put(bindParamsKey, entry.getValue());
        }

        return prepareQuery(query, parameterMaps);
    }

    private static QueryConditional prepareQuery(String query, Map<String, Object> parameterMaps) {
        HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HqlParser parser = new HqlParser(tokens);
        HqlParser.PredicateContext predicate = parser.predicate();
        HqlParserBaseVisitor<QueryConditional> visitor = new DynamoDBParserVisitor(parameterMaps);
        
        return predicate.accept(visitor);
    }
}
