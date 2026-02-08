package com.github.starnowski.mongo.fun.mongodb.container.odata;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;

@ApplicationScoped
public class Example2StaticEdmSupplier {

  private final Edm EDM;

  public Example2StaticEdmSupplier() throws Exception {
    OpenApiToODataMapper openApiToODataMapper = new OpenApiToODataMapper();
    OpenApiToODataMapper.OpenApiToODataMapperResult odataConfig =
        openApiToODataMapper.returnOpenApiToODataConfiguration(
            "src/main/resources/example2_openapi.yaml", "Example2");
    GenericEdmProvider provider =
        new GenericEdmProvider(
            new GenericEdmProvider.GenericEdmProviderProperties("Example2", "examples2"),
            odataConfig);
    this.EDM = new EdmProviderImpl(provider);

    // Serialize to string for comparison
    OData odata = OData.newInstance();
    ODataSerializer serializer = odata.createSerializer(ContentType.APPLICATION_XML);
    ServiceMetadata serviceMetadata = odata.createServiceMetadata(provider, new ArrayList<>());
    SerializerResult result = serializer.metadataDocument(serviceMetadata);

    String xml = new String(result.getContent().readAllBytes(), StandardCharsets.UTF_8);
    System.out.println("<edm>");
    System.out.println(xml);
    System.out.println("</edm>");
  }

  public Edm get() {
    return this.EDM;
  }
}
