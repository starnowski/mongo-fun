package com.github.starnowski.mongo.fun.mongodb.container.odata;

import static com.github.starnowski.mongo.fun.mongodb.container.odata.MongoFilterVisitor.ODATA_MEMBER_PROPERTY;

import com.mongodb.client.model.Sorts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.bson.conversions.Bson;

public class OdataOrderToMongoSortParser {

  public static Bson parseOrder(UriInfo uriInfo, Edm edm)
      throws ODataApplicationException, ExpressionVisitException {
    List<OrderByItem> orderByItems =
        uriInfo.getOrderByOption() != null
            ? uriInfo.getOrderByOption().getOrders()
            : Collections.emptyList();
    return buildSort(orderByItems, edm);
  }

  private static Bson buildSort(List<OrderByItem> orderByItems, Edm edm)
      throws ExpressionVisitException, ODataApplicationException {
    List<Bson> sortList = new ArrayList<>();
    for (OrderByItem item : orderByItems) {
      String path = extractMongoPath(item, edm); // e.g., "address.city"
      Bson sortBson = item.isDescending() ? Sorts.descending(path) : Sorts.ascending(path);
      sortList.add(sortBson);
    }
    return Sorts.orderBy(sortList);
  }

  private static String extractMongoPath(OrderByItem item, Edm edm)
      throws ExpressionVisitException, ODataApplicationException {
    Bson value = item.getExpression().accept(new MongoFilterVisitor(edm));
    return value.toBsonDocument().getString(ODATA_MEMBER_PROPERTY).getValue();
  }
}
