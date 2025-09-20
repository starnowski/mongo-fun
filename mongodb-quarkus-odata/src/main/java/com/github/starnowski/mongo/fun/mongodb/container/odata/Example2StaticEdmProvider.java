package com.github.starnowski.mongo.fun.mongodb.container.odata;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Example2StaticEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "MyService";

    // --- Entity Type & Entity Set names ---
    public static final String ENTITY_TYPE_NAME = "Example2";    // PascalCase for type
    public static final String ENTITY_SET_NAME = "examples2";    // plural, matches URL segment
    public static final FullQualifiedName FQN_EXAMPLE2 = new FullQualifiedName(NAMESPACE, ENTITY_TYPE_NAME);

    // --- Container ---
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private final List<CsdlSchema> schemas;

    public Example2StaticEdmProvider() {
        this.schemas = prepareSchemas();
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        return getSchemas().get(0).getEntityType(entityTypeName.getName());
    }

    private List<CsdlSchema> prepareSchemas() {
        List<CsdlSchema> schemas = new ArrayList<>();

        // --- Define Example2 entity type ---
        CsdlEntityType example2Type = new CsdlEntityType()
                .setName(ENTITY_TYPE_NAME)
                .setProperties(Arrays.asList(
                        new CsdlProperty().setName("plainString")
                                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("smallInteger")
                                .setType(EdmPrimitiveTypeKind.Single.getFullQualifiedName())
                ))
                //TODO set _id guid
//                .setKey(Collections.singletonList(new CsdlPropertyRef().setName("plainString")))
                ;

        // --- Define entity set ---
        CsdlEntitySet entitySet = new CsdlEntitySet()
                .setName(ENTITY_SET_NAME)      // 👈 matches URL /examples2
                .setType(FQN_EXAMPLE2);

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
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            return new CsdlEntityContainerInfo()
                    .setContainerName(CONTAINER);
        }
        return null;
    }
}
