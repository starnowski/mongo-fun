package com.github.starnowski.mongo.fun.mongodb.container.odata;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GenericEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "MyService";
    // --- Container ---
    public static final String CONTAINER_NAME = "Container";

    private final List<CsdlSchema> schemas;
    private final FullQualifiedName container;

    public GenericEdmProvider(GenericEdmProviderProperties genericEdmProviderProperties, OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig) {
        this.schemas = prepareSchemas(genericEdmProviderProperties, odataConfig);
        this.container = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
    }

    public record GenericEdmProviderProperties(String entityTypeName, String entitySetName){}

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        return getSchemas().get(0).getEntityType(entityTypeName.getName());
    }

    private List<CsdlSchema> prepareSchemas(GenericEdmProviderProperties genericEdmProviderProperties, OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig) {
        List<CsdlSchema> schemas = new ArrayList<>();

        // --- Define Example2 entity type ---
        CsdlEntityType example2Type = new CsdlEntityType()
                .setName(genericEdmProviderProperties.entityTypeName())
                .setProperties(
//                        odataConfig.mainEntityProperties().entrySet().stream().map(
//                                entry -> new CsdlProperty().setName(entry.getKey())
//                                        .setType(entry.getValue())
//                        ).toList()
                        odataConfig.mainEntity().properties().entrySet().stream().map(entry -> new CsdlProperty().setName(entry.getKey()).setType(entry.getValue().type()).setCollection(entry.getValue().isCollection())).toList()

                )
                //TODO set _id guid
//                .setKey(Collections.singletonList(new CsdlPropertyRef().setName("plainString")))
                ;

        // --- Define entity set ---
        CsdlEntitySet entitySet = new CsdlEntitySet()
                .setName(genericEdmProviderProperties.entitySetName())      // 👈 matches URL /examples2
                .setType(new FullQualifiedName(NAMESPACE, genericEdmProviderProperties.entityTypeName()));

        // --- Define entity container ---
        CsdlEntityContainer container = new CsdlEntityContainer()
                .setName(CONTAINER_NAME)
                .setEntitySets(Collections.singletonList(entitySet));

        // --- Define schema ---
        CsdlSchema schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(Collections.singletonList(example2Type))
                .setEntityContainer(container);

        schemas.add(schema);
        return schemas;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        return schemas;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
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
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        if (entityContainerName == null || entityContainerName.equals(container)) {
            return new CsdlEntityContainerInfo()
                    .setContainerName(container);
        }
        return null;
    }
}
