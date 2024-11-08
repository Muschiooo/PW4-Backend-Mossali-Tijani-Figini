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
                .append("details", convertProductDetailsToList(order.getDetails()))  // Usa la lista convertita
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", order.getOrderDate())
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());

        collection.insertOne(document);
    }

    public List<OrderMongo> findAll() {
        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();
        FindIterable<Document> findIterable = collection.find();  // Trova tutti i documenti nella collection

        for (Document document : findIterable) {
            OrderMongo order = new OrderMongo();

            // Impostazione dei campi principali dell'ordine
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getDate("orderDate"));
            order.setDeliverDate(document.getDate("deliverDate"));
            order.setStatus(document.getString("status"));

            // Recupero dei dettagli dell'ordine come lista
            List<Document> detailsList = (List<Document>) document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();

            // Iterazione sui dettagli dell'ordine
            for (Document detailDoc : detailsList) {
                String productId = detailDoc.getString("productId");

                // Verifica che productId non sia null o vuoto
                if (productId != null && !productId.isEmpty()) {
                    try {
                        // Recupera il prodotto dal repository usando il productId
                        Product product = productRepository.findProductById(Integer.parseInt(productId));
                        if (product != null) {
                            // Se il prodotto è trovato, crea un ProductDetail
                            ProductDetail detail = new ProductDetail();
                            detail.setQuantity(detailDoc.getInteger("quantity"));
                            detail.setPrice(detailDoc.getDouble("price"));
                            detail.setName(product.getName());  // Aggiungi il nome del prodotto
                            detailsMap.put(productId, detail);  // Aggiungi il dettaglio all'elenco
                        }
                    } catch (Exception e) {
                        // Gestisci eventuali errori nel recupero del prodotto
                        System.err.println("Errore nel recupero del prodotto per productId: " + productId);
                    }
                } else {
                    // Gestisci il caso in cui productId è null o vuoto
                    System.err.println("productId mancante o vuoto per un prodotto.");
                }
            }

            // Impostazione dei dettagli nell'ordine
            order.setDetails(detailsMap);

            // Aggiungi l'ordine alla lista
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
        order.setTotalPrice(document.getDouble("totalPrice"));
        order.setOrderDate(document.getDate(("orderDate")));
        order.setDeliverDate(document.getDate("deliverDate"));
        order.setStatus(document.getString("status"));

        // Adatta la lettura dei dettagli come una lista
        List<Document> detailsList = (List<Document>) document.get("details");
        Map<String, ProductDetail> detailsMap = new HashMap<>();
        for (Document detailDoc : detailsList) {
            String productId = detailDoc.getString("productId");

            // Verifica che il productId sia valido prima di utilizzarlo
            if (productId != null && !productId.isEmpty()) {
                try {
                    // Prosegui con il recupero del prodotto dal repository
                    Product product = productRepository.findProductById(Integer.parseInt(productId));
                    if (product != null) {
                        ProductDetail detail = new ProductDetail();
                        detail.setQuantity(detailDoc.getInteger("quantity"));
                        detail.setPrice(detailDoc.getDouble("price"));
                        detailsMap.put(productId, detail);
                    }
                } catch (NumberFormatException e) {
                    // Gestisci il caso in cui productId non può essere convertito in intero
                    System.err.println("Errore nel parsing del productId: " + productId);
                }
            } else {
                // Gestisci il caso in cui productId è null o vuoto
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

        // Trova l'ordine con il deliverDate più recente ordinando in ordine decrescente
        Document document = collection
                .find()
                .sort(Sorts.descending("deliverDate"))
                .first();  // Prende solo il primo risultato

        // Se non esiste alcun ordine, ritorna null
        if (document == null) {
            return null;
        }

        // Creazione dell'oggetto OrderMongo basato sui campi del documento
        OrderMongo order = new OrderMongo();
        order.setId(document.getObjectId("_id"));
        order.setUserEmail(document.getString("userEmail"));
        order.setTotalPrice(document.getDouble("totalPrice"));
        order.setOrderDate(document.getDate("orderDate"));
        order.setDeliverDate(document.getDate("deliverDate"));
        order.setStatus(document.getString("status"));

        // Estrazione e mappatura dei dettagli dell'ordine
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
