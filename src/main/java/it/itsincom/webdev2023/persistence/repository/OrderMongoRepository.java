package it.itsincom.webdev2023.persistence.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.persistence.model.ProductDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

@ApplicationScoped
public class OrderMongoRepository {

    @Inject
    MongoClient mongoClient;

    @Inject
    ProductRepository productRepository;

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
                .append("comment", order.getComment())
                .append("details", convertProductDetailsToList(order.getDetails()))
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", order.getOrderDate())
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());

        collection.insertOne(document);
    }

    public List<OrderMongo> findAll() {
        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();
        FindIterable<Document> findIterable = collection.find();

        for (Document document : findIterable) {
            OrderMongo order = new OrderMongo();

            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setComment(document.getString("comment"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getDate("orderDate"));
            order.setDeliverDate(document.getDate("deliverDate"));
            order.setStatus(document.getString("status"));

            List<Document> detailsList = (List<Document>) document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();

            for (Document detailDoc : detailsList) {
                String productId = detailDoc.getString("productId");

                if (productId != null && !productId.isEmpty()) {
                    try {
                        Product product = productRepository.findProductById(Integer.parseInt(productId));
                        if (product != null) {
                            ProductDetail detail = new ProductDetail();
                            detail.setQuantity(detailDoc.getInteger("quantity"));
                            detail.setPrice(detailDoc.getDouble("price"));
                            detail.setName(product.getName());
                            detailsMap.put(productId, detail);
                        }
                    } catch (Exception e) {
                        System.err.println("Errore nel recupero del prodotto per productId: " + productId);
                    }
                } else {
                    System.err.println("productId mancante o vuoto per un prodotto.");
                }
            }
            order.setDetails(detailsMap);

            orders.add(order);
        }

        return orders;
    }

    public OrderMongo findById(ObjectId id) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = collection.find(new Document("_id", id)).first();
        if (document == null) {
            return null;
        }
        OrderMongo order = new OrderMongo();
        order.setId(document.getObjectId("_id"));
        order.setUserEmail(document.getString("userEmail"));
        order.setComment(document.getString("comment"));
        order.setTotalPrice(document.getDouble("totalPrice"));
        order.setOrderDate(document.getDate(("orderDate")));
        order.setDeliverDate(document.getDate("deliverDate"));
        order.setStatus(document.getString("status"));

        List<Document> detailsList = (List<Document>) document.get("details");
        Map<String, ProductDetail> detailsMap = new HashMap<>();
        for (Document detailDoc : detailsList) {
            String productId = detailDoc.getString("productId");

            if (productId != null && !productId.isEmpty()) {
                try {
                    Product product = productRepository.findProductById(Integer.parseInt(productId));
                    if (product != null) {
                        ProductDetail detail = new ProductDetail();
                        detail.setQuantity(detailDoc.getInteger("quantity"));
                        detail.setPrice(detailDoc.getDouble("price"));
                        detailsMap.put(productId, detail);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Errore nel parsing del productId: " + productId);
                }
            } else {
                System.err.println("productId mancante o vuoto per un prodotto.");
            }
        }

        order.setDetails(detailsMap);

        return order;
    }

    public void update(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = new Document()
                .append("userEmail", order.getUserEmail())
                .append("comment", order.getComment())
                .append("details", convertProductDetailsToList(order.getDetails()))
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", order.getOrderDate())
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());

        collection.updateOne(new Document("_id", order.getId()), new Document("$set", document));
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
        for (Document document : collection.find(eq("userEmail", email))) {
            OrderMongo order = new OrderMongo();
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setComment(document.getString("comment"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getDate(("orderDate")));
            order.setDeliverDate(document.getDate("deliverDate"));
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

    public OrderMongo findLatestDeliveryDate() {
        MongoCollection<Document> collection = getOrdersCollection();

        Document document = collection
                .find()
                .sort(Sorts.descending("deliverDate"))
                .first();

        if (document == null) {
            return null;
        }

        OrderMongo order = new OrderMongo();
        order.setId(document.getObjectId("_id"));
        order.setUserEmail(document.getString("userEmail"));
        order.setComment(document.getString("comment"));
        order.setTotalPrice(document.getDouble("totalPrice"));
        order.setOrderDate(document.getDate("orderDate"));
        order.setDeliverDate(document.getDate("deliverDate"));
        order.setStatus(document.getString("status"));

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

        return order;
    }

    public String dateTimeFormatter(Date date) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'alle ore' HH.mm");
        String formattedDate = dateTime.format(formatter);
        System.out.println(formattedDate);
        return formattedDate;
    }
}
