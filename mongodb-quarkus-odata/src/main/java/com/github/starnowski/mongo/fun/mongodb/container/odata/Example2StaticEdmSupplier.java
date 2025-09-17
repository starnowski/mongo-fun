package com.github.starnowski.mongo.fun.mongodb.container.odata;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;

@ApplicationScoped
public class Example2StaticEdmSupplier {

    private final Edm EDM;


    public Example2StaticEdmSupplier() {
        this.EDM = new EdmProviderImpl(new Example2StaticEdmProvider());
    }

    public Edm get() {
        return this.EDM;
    }
}