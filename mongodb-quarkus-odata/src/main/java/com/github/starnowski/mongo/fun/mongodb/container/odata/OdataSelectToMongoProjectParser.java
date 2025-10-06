package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.mongodb.client.model.Projections;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class OdataSelectToMongoProjectParser {

    public static Bson buildProjection(SelectOption selectOption) {
        if (selectOption == null || selectOption.getSelectItems().isEmpty()) {
            return Projections.include(); // no projection → all fields
        }

        List<String> fields = new ArrayList<>();
        for (SelectItem item : selectOption.getSelectItems()) {
            if (item.isStar()) {
                return Projections.include(); // * means all fields
            }
            //TODO test complex
            String propertyName = item.getResourcePath().getUriResourceParts()
                    .stream()
                    .filter(p -> p instanceof UriResourcePrimitiveProperty)
                    .map(p -> ((UriResourcePrimitiveProperty) p).getProperty().getName())
                    .findFirst()
                    .orElse(null);

            if (propertyName != null) {
                fields.add(propertyName);
            }
        }

        return Projections.include(fields);
    }

}
