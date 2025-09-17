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
    public static final String ENTITY_TYPE_NAME = "Example2";   // entity type
    public static final String ENTITY_SET_NAME = "examples2";
    public static final FullQualifiedName FQN_EXAMPLE2 = new FullQualifiedName(NAMESPACE, ENTITY_TYPE_NAME);
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        List<CsdlSchema> schemas = new ArrayList<>();

        // Define Example2 entity
        CsdlEntityType example2Type = new CsdlEntityType()
                .setName(ENTITY_TYPE_NAME)
                .setProperties(Arrays.asList(
                        new CsdlProperty().setName("plainString").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
//                        ,
//                        new CsdlProperty().setName("age").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
//                        ,
//                        new CsdlProperty().setName("city").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                ))
                .setKey(Collections.singletonList(new CsdlPropertyRef().setName("plainString"))); // key on "name"

        // Define entity set
        CsdlEntitySet entitySet = new CsdlEntitySet()
                .setName(ENTITY_SET_NAME)
                .setType(FQN_EXAMPLE2);

        // Define entity container
        CsdlEntityContainer container = new CsdlEntityContainer()
                .setName(CONTAINER_NAME)
                .setEntitySets(Collections.singletonList(entitySet));

        CsdlSchema schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(Collections.singletonList(example2Type))
                .setEntityContainer(container);

        schemas.add(schema);
        return schemas;
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
