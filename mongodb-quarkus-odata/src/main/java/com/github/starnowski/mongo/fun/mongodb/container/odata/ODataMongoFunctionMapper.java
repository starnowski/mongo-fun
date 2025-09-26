package com.github.starnowski.mongo.fun.mongodb.container.odata;

import java.util.HashMap;
import java.util.Map;

public class ODataMongoFunctionMapper {

    private static final Map<String, String> ZERO_ARGUMENT_FUNCTION_MAP = new HashMap<>();
    private static final Map<String, String> ONE_ARGUMENT_FUNCTION_MAP = new HashMap<>();
    private static final Map<String, String> ONE_ARGUMENT_FUNCTION_MAP_EXPR_REQUIRED = new HashMap<>();

    static {
        // String functions
        ONE_ARGUMENT_FUNCTION_MAP_EXPR_REQUIRED.put("length", "$strLenCP");
        ONE_ARGUMENT_FUNCTION_MAP.put("tolower", "$toLower");
        ONE_ARGUMENT_FUNCTION_MAP.put("toupper", "$toUpper");
//        ONE_ARGUMENT_FUNCTION_MAP.put("trim", "$trim");

        // Date/Time functions
        ONE_ARGUMENT_FUNCTION_MAP.put("year", "$year");
        ONE_ARGUMENT_FUNCTION_MAP.put("month", "$month");
        ONE_ARGUMENT_FUNCTION_MAP.put("day", "$dayOfMonth");
        ONE_ARGUMENT_FUNCTION_MAP.put("hour", "$hour");
        ONE_ARGUMENT_FUNCTION_MAP.put("minute", "$minute");
        ONE_ARGUMENT_FUNCTION_MAP.put("second", "$second");


        // Math
        ONE_ARGUMENT_FUNCTION_MAP.put("round", "$round");
        ONE_ARGUMENT_FUNCTION_MAP.put("floor", "$floor");
        ONE_ARGUMENT_FUNCTION_MAP.put("ceiling", "$ceil");

        // Date constants
        ZERO_ARGUMENT_FUNCTION_MAP.put("mindatetime", "ISODate(\"0001-01-01T00:00:00Z\")");
        ZERO_ARGUMENT_FUNCTION_MAP.put("maxdatetime", "ISODate(\"9999-12-31T23:59:59.999Z\")");
        ZERO_ARGUMENT_FUNCTION_MAP.put("now", "$$NOW");
    }

    public static String toOneArgumentMongoOperator(String odataFunction) {
        return ONE_ARGUMENT_FUNCTION_MAP.getOrDefault(odataFunction.toLowerCase(), null);
    }

    public static String toOneArgumentMongoOperatorExprRequired(String odataFunction) {
        return ONE_ARGUMENT_FUNCTION_MAP_EXPR_REQUIRED.getOrDefault(odataFunction.toLowerCase(), null);
    }

    // Example usage
//    public static void main(String[] args) {
//        System.out.println("OData length -> " + toMongoOperator("length"));
//        System.out.println("OData year -> " + toMongoOperator("year"));
//        System.out.println("OData now -> " + toMongoOperator("now"));
//        System.out.println("OData geo.length -> " + toMongoOperator("geo.length"));
//    }
}
