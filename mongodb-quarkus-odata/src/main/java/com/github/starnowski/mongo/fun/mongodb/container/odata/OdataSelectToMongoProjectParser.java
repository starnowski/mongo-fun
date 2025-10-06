package com.github.starnowski.mongo.fun.mongodb.container.odata;

import com.mongodb.client.model.Projections;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.SelectItem;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            if (!item.getResourcePath().getUriResourceParts()
                    .stream().allMatch(
                            p -> p instanceof UriResourceProperty
                    )) {
                throw new RuntimeException("Invalid select parameter " + item.getResourcePath());
            }
            String propertyName = item.getResourcePath().getUriResourceParts()
                    .stream()
                    .map(p -> ((UriResourceProperty) p).getProperty().getName())
                    .collect(Collectors.joining("."));
            fields.add(propertyName);
        }

        return Projections.include(fields);
    }

}
