package it.itsincom.webdev2023.persistence.repository;

import com.mongodb.client.model.Filters;
import it.itsincom.webdev2023.persistence.model.OrderDetailMongo;
import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.persistence.model.ProductDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class OrderMongoRepository {

    @Inject
    MongoClient mongoClient;

    private MongoCollection<Document> getOrdersCollection() {
        MongoDatabase database = mongoClient.getDatabase("pasticceria");
        return database.getCollection("orders");
    }

    // Metodo per convertire i dettagli in una lista di documenti
    private List<Document> convertProductDetailsToList(Map<String, ProductDetail> details) {
        List<Document> detailsList = new ArrayList<>();
        for (Map.Entry<String, ProductDetail> entry : details.entrySet()) {
            Document detailDoc = new Document()
                    .append("productId", entry.getKey())
                    .append("quantity", entry.getValue().getQuantity())
                    .append("price", entry.getValue().getPrice());
            detailsList.add(detailDoc);
        }
        return detailsList;
    }

    public void save(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = new Document()
                .append("userEmail", order.getUserEmail())
                .append("details", convertProductDetailsToList(order.getDetails()))  // Usa la lista convertita
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", convertStringToDate(order.getOrderDate()))
                .append("deliverDate", convertStringToDate(order.getDeliverDate()))
                .append("status", order.getStatus());

        collection.insertOne(document);
    }


    public String getCurrentDateTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public OrderMongo findById(String orderId) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = collection.find(Filters.eq("_id", new ObjectId(orderId))).first();

        if (document != null) {
            OrderMongo order = new OrderMongo();
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getString(convertStringToDate("orderDate")));
            order.setDeliverDate(document.getString("deliverDate"));
            order.setStatus(document.getString("status"));

            // Adatta la lettura dei dettagli come una lista
            List<Document> detailsList = (List<Document>) document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();
            for (Document detailDoc : detailsList) {
                String productId = detailDoc.getString("productId");
                ProductDetail detail = new ProductDetail();
                detail.setQuantity(detailDoc.getInteger("quantity"));
                detail.setPrice(detailDoc.getDouble("price"));
                detailsMap.put(productId, detail);
            }
            order.setDetails(detailsMap);  // Popola la mappa con i dettagli

            return order;
        }

        return null;
    }

    public static Date convertStringToDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return sdf.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertDateToString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public void update(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document updatedOrder = new Document()
                .append("userEmail", order.getUserEmail())
                .append("details", order.getDetails())
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", convertStringToDate(order.getOrderDate()))
                .append("deliverDate", convertStringToDate(order.getDeliverDate()))
                .append("status", order.getStatus());

        collection.updateOne(
                new Document("_id", order.getId()),
                new Document("$set", updatedOrder)
        );
    }

    public void delete(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        collection.deleteOne(new Document("_id", order.getId()));
    }

    public boolean exists(ObjectId id) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = collection.find(new Document("_id", id)).first();
        return document != null;
    }

    public List<OrderMongo> findByUserEmail(String email) {
        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();
        for (Document document : collection.find(Filters.eq("userEmail", email))) {
            OrderMongo order = new OrderMongo();
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getString(convertStringToDate("orderDate")));
            order.setDeliverDate(document.getString(convertStringToDate("deliverDate")));
            order.setStatus(document.getString("status"));

            // Adatta la lettura dei dettagli come una lista
            List<Document> detailsList = (List<Document>) document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();
            for (Document detailDoc : detailsList) {
                String productId = detailDoc.getString("productId");
                ProductDetail detail = new ProductDetail();
                detail.setQuantity(detailDoc.getInteger("quantity"));
                detail.setPrice(detailDoc.getDouble("price"));
                detailsMap.put(productId, detail);
            }
            order.setDetails(detailsMap);

            orders.add(order);
        }
        return orders;
    }

}
