package com.github.starnowski.mongo.fun.mongodb.container.odata;

import java.util.*;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;

public class GenericEdmProvider extends CsdlAbstractEdmProvider {

  public static final String NAMESPACE = "MyService";
  // --- Container ---
  public static final String CONTAINER_NAME = "Container";

  private final List<CsdlSchema> schemas;
  private final FullQualifiedName container;

  public GenericEdmProvider(
      GenericEdmProviderProperties genericEdmProviderProperties,
      OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig) {
    this.schemas = prepareSchemas(genericEdmProviderProperties, odataConfig);
    this.container = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
  }

  public record GenericEdmProviderProperties(String entityTypeName, String entitySetName) {}

  @Override
  public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
    return getSchemas().get(0).getEntityType(entityTypeName.getName());
  }

  private final Map<String, String> typesPaths = new HashMap<>();

  private String addCsdlComplexType(
      String path,
      OpenApiToODataMapper.ODataType oDataType,
      String typePrefix,
      List<CsdlComplexType> types,
      int level) {
    String typeName = NAMESPACE + "." + typePrefix + "Type" + level;
    CsdlComplexType csdlComplexType =
        new CsdlComplexType()
            .setName(typeName)
            .setProperties(
                oDataType.properties().entrySet().stream()
                    .map(
                        entry -> {
                          String type;
                          if (entry.getValue().object() != null) {
                            type =
                                addCsdlComplexType(
                                    path + "." + entry.getKey(),
                                    entry.getValue().object(),
                                    entry.getKey(),
                                    types,
                                    level + 1);
                          } else {
                            type = entry.getValue().type();
                          }
                          return new CsdlProperty()
                              .setName(entry.getKey())
                              .setType(type)
                              .setCollection(entry.getValue().isCollection());
                        })
                    .toList());
    types.add(csdlComplexType);
    typesPaths.put(typeName, path);
    return typeName;
  }

  private List<CsdlSchema> prepareSchemas(
      GenericEdmProviderProperties genericEdmProviderProperties,
      OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig) {
    List<CsdlSchema> schemas = new ArrayList<>();
    List<CsdlComplexType> types = new ArrayList<>();
    // --- Define Example2 entity type ---
    CsdlEntityType example2Type =
        new CsdlEntityType()
            .setName(genericEdmProviderProperties.entityTypeName())
            .setProperties(
                //
                // odataConfig.mainEntityProperties().entrySet().stream().map(
                //                                entry -> new
                // CsdlProperty().setName(entry.getKey())
                //                                        .setType(entry.getValue())
                //                        ).toList()
                odataConfig.mainEntity().properties().entrySet().stream()
                    .map(
                        entry -> {
                          String type;
                          if (entry.getValue().object() != null) {
                            type =
                                addCsdlComplexType(
                                    entry.getKey(),
                                    entry.getValue().object(),
                                    entry.getKey(),
                                    types,
                                    1);
                          } else {
                            type = entry.getValue().type();
                          }
                          return new CsdlProperty()
                              .setName(entry.getKey())
                              .setType(type)
                              .setCollection(entry.getValue().isCollection());
                        })
                    .toList())
        // TODO set _id guid
        //                .setKey(Collections.singletonList(new
        // CsdlPropertyRef().setName("plainString")))
        ;

    // --- Define entity set ---
    CsdlEntitySet entitySet =
        new CsdlEntitySet()
            .setName(genericEdmProviderProperties.entitySetName()) // 👈 matches URL /examples2
            .setType(
                new FullQualifiedName(NAMESPACE, genericEdmProviderProperties.entityTypeName()));

    // --- Define entity container ---
    CsdlEntityContainer container =
        new CsdlEntityContainer()
            .setName(CONTAINER_NAME)
            .setEntitySets(Collections.singletonList(entitySet));

    // --- Define types

    // --- Define schema ---
    CsdlSchema schema =
        new CsdlSchema()
            .setNamespace(NAMESPACE)
            .setEntityTypes(Collections.singletonList(example2Type))
            .setComplexTypes(types)
            .setEntityContainer(container);

    schemas.add(schema);
    return schemas;
  }

  @Override
  public List<CsdlSchema> getSchemas() throws ODataException {
    return schemas;
  }

  @Override
  public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName)
      throws ODataException {
    return getSchemas().get(0).getEntityContainer().getEntitySet(entitySetName);
  }

  @Override
  public CsdlEntityContainer getEntityContainer() {
    try {
      return getSchemas().get(0).getEntityContainer();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName)
      throws ODataException {
    if (entityContainerName == null || entityContainerName.equals(container)) {
      return new CsdlEntityContainerInfo().setContainerName(container);
    }
    return null;
  }

  @Override
  public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) throws ODataException {
    return getSchemas().get(0).getComplexType(complexTypeName.getFullQualifiedNameAsString());
  }
}
