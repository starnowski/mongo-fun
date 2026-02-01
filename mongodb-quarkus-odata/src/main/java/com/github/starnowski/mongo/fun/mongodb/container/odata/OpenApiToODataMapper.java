package com.github.starnowski.mongo.fun.mongodb.container.odata;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiToODataMapper {

  public record ODataProperty(
      String name, String reference, String type, boolean isCollection, ODataType object) {
    public boolean isObjectType() {
      return object != null;
    }
  }

  public record ODataType(Map<String, ODataProperty> properties) {}

  public record OpenApiToODataMapperResult(
      Map<String, String> mainEntityProperties, ODataType mainEntity) {}

  public OpenApiToODataMapperResult returnOpenApiToODataConfiguration(
      String openApiSpec, String schemaRef) throws Exception {
    // 1. Load OpenAPI spec
    OpenAPI openAPI =
        new OpenAPIV3Parser()
            .readContents(Files.readString(Path.of(openApiSpec)), null, null)
            .getOpenAPI();
    if (openAPI == null) {
      throw new IllegalArgumentException("Invalid OpenAPI spec");
    }

    // 2. Get schema by ref (e.g. "#/components/schemas/MyType")
    String schemaName = schemaRef.replace("#/components/schemas/", "");
    Schema<?> oasSchema = openAPI.getComponents().getSchemas().get(schemaName);
    Map<String, String> mainEntityProperties = new HashMap<>();
    enrichWithTypeDefinitions(oasSchema, mainEntityProperties);
    ODataType odataType = new ODataType(new HashMap<>());
    enrichWithTypeDefinitions(oasSchema, odataType);

    return new OpenApiToODataMapperResult(
        mainEntityProperties.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
        odataType);
  }

  private ODataProperty enrichWithTypeDefinitions(Schema<?> schema, ODataType oDataType) {
    if (schema == null) return null;

    // Handle polymorphic schemas
    if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
      Map<String, Object> merged = new LinkedHashMap<>();
      for (Schema<?> subSchema : schema.getAllOf()) {
        enrichWithTypeDefinitions(subSchema, oDataType);
        // TODO
      }
      // TODO
      return null;
    }

    if ((schema.getOneOf() != null && !schema.getOneOf().isEmpty())
        || (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty())) {

      List<Schema> options = schema.getOneOf() != null ? schema.getOneOf() : schema.getAnyOf();
      for (Schema<?> option : options) {
        try {
          enrichWithTypeDefinitions(option, oDataType);
          // TODO
        } catch (Exception ignored) {
          // try next option
        }
      }
      // fallback
      // TODO
      return null;
    }

    String type = schema.getType();
    String format = schema.getFormat();

    if (schema.getProperties() != null) {
      schema
          .getProperties()
          .forEach(
              (k, v) -> {
                ODataType nestedOdataDataType = new ODataType(new HashMap<>());
                ODataProperty odata = enrichWithTypeDefinitions(v, nestedOdataDataType);
                // TODO
                oDataType.properties.put(
                    k, new ODataProperty(k, k, odata.type, odata.isCollection, odata.object));
              });
      String odataType = mapToODataType(type, format);
      return new ODataProperty(null, null, odataType, false, oDataType);
    } else if (schema.getItems() != null) {
      if ("array".equals(schema.getType())) {
        ODataProperty odata = enrichWithTypeDefinitions(schema.getItems(), oDataType);
        return new ODataProperty(
            null, null, "Collection(%s)".formatted(odata.type), true, odata.object());
      }
    } else {
      String mappedType = mapToODataType(type, format);
      return new ODataProperty(null, null, mappedType, false, null);
    }
    return null;
  }

  private String enrichWithTypeDefinitions(
      Schema<?> schema, Map<String, String> mainEntityProperties) {
    if (schema == null) return null;

    // Handle polymorphic schemas
    if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
      Map<String, Object> merged = new LinkedHashMap<>();
      for (Schema<?> subSchema : schema.getAllOf()) {
        enrichWithTypeDefinitions(subSchema, mainEntityProperties);
        // TODO
      }
      // TODO
      return null;
    }

    if ((schema.getOneOf() != null && !schema.getOneOf().isEmpty())
        || (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty())) {

      List<Schema> options = schema.getOneOf() != null ? schema.getOneOf() : schema.getAnyOf();
      for (Schema<?> option : options) {
        try {
          enrichWithTypeDefinitions(option, mainEntityProperties);
          // TODO
        } catch (Exception ignored) {
          // try next option
        }
      }
      // fallback
      // TODO
      return null;
    }

    String type = schema.getType();
    String format = schema.getFormat();

    if (schema.getProperties() != null) {
      Map<String, Object> newMap = new LinkedHashMap<>();
      schema
          .getProperties()
          .forEach(
              (k, v) -> {
                mainEntityProperties.put(k, enrichWithTypeDefinitions(v, mainEntityProperties));
              });
      // TODO
    } else if (schema.getItems() != null) {
      if ("array".equals(schema.getType())) {
        return "Collection(%s)"
            .formatted(enrichWithTypeDefinitions(schema.getItems(), mainEntityProperties));
      }
    } else {
      return mapToODataType(type, format);
    }
    return null;
  }

  /**
   * Maps OpenAPI type/format to OData type.
   *
   * @param type OpenAPI type (e.g., "string", "integer", "number", "boolean", "array", "object")
   * @param format OpenAPI format (e.g., "date-time", "uuid", "int32", may be null)
   * @return OData type name (e.g., "Edm.String", "Edm.Int32", "Edm.DateTimeOffset")
   */
  public static String mapToODataType(String type, String format) {
    if (type == null) {
      return "Edm.String"; // default fallback
    }

    switch (type) {
      case "string":
        if (format == null) return "Edm.String";
        return switch (format) {
          case "byte" -> "Edm.Binary";
          case "binary" -> "Edm.Stream";
          case "date" -> "Edm.Date";
          case "date-time" -> "Edm.DateTimeOffset";
          case "uuid" -> "Edm.Guid";
          case "password" -> "Edm.String";
          default -> "Edm.String"; // e.g., email, uri, hostname
        };

      case "number":
        if (format == null) return "Edm.Decimal";
        return switch (format) {
          case "float" -> "Edm.Single";
          case "double" -> "Edm.Double";
          default -> "Edm.Decimal";
        };

      case "integer":
        if (format == null) return "Edm.Int64"; // safest fallback
        if (format.equals("int32")) {
          return "Edm.Int32";
        }
        return "Edm.Int64";

      case "boolean":
        return "Edm.Boolean";

      case "array":
        // In OData, arrays are collections of types, but we don’t know item type here
        return "Collection(Edm.String)";

      case "object":
        return "Edm.ComplexType";

      default:
        return "Edm.String"; // generic fallback
    }
  }
}
