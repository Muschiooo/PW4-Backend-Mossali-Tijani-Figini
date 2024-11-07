package it.itsincom.webdev2023.persistence.repository;

import com.mongodb.client.model.Filters;
import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.persistence.model.ProductDetail;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

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
                .append("orderDate", order.getOrderDate())
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());

        collection.insertOne(document);
    }

    public List<OrderMongo> findAll() {
        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();

        for (Document document : collection.find()) {
            OrderMongo order = new OrderMongo();
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getDate("orderDate"));
            order.setDeliverDate(document.getDate("deliverDate"));
            order.setStatus(document.getString("status"));

            // Debug: Aggiungi un log per vedere la struttura completa del documento
            System.out.println("Documento MongoDB: " + document.toJson()); // Aggiungi questo per vedere tutto il documento

            // Verifica se 'details' è presente e trattalo di conseguenza
            Object detailsObject = document.get("details");
            Map<String, ProductDetail> productDetailsMap = new HashMap<>();

            if (detailsObject != null) {
                // Se 'details' è una lista
                if (detailsObject instanceof List) {
                    List<Document> detailsList = (List<Document>) detailsObject;
                    System.out.println("Tipo di 'details' identificato come lista: " + detailsList.getClass()); // Verifica il tipo di 'details'

                    // Cicla su ogni elemento nella lista
                    for (Document detailDoc : detailsList) {
                        String productId = detailDoc.getString("productId"); // Assicurati che `productId` sia presente
                        if (productId != null) {
                            ProductDetail detail = new ProductDetail();
                            detail.setQuantity(detailDoc.getInteger("quantity"));
                            detail.setPrice(detailDoc.getDouble("price"));

                            // Se 'name' è null, non settarlo
                            String name = detailDoc.getString("name");
                            if (name != null) {
                                detail.setName(name);
                            }

                            productDetailsMap.put(productId, detail);
                        }
                    }
                } else {
                    // Se 'details' è un tipo diverso (es. Map o altro)
                    System.out.println("Campo details trovato ma non è una lista: " + detailsObject.getClass());
                }
            } else {
                // Se 'details' non esiste
                System.out.println("Campo details non trovato nel documento");
            }

            order.setDetails(productDetailsMap);  // Popola la mappa con i dettagli
            orders.add(order);
        }

        return orders;
    }

    public OrderMongo findById(String orderId) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = collection.find(Filters.eq("_id", new ObjectId(orderId))).first();

        if (document != null) {
            OrderMongo order = new OrderMongo();
            order.setId(document.getObjectId("_id"));
            order.setUserEmail(document.getString("userEmail"));
            order.setTotalPrice(document.getDouble("totalPrice"));
            order.setOrderDate(document.getDate("orderDate"));
            order.setDeliverDate(document.getDate("deliverDate"));
            order.setStatus(document.getString("status"));

            // Leggi i dettagli dell'ordine
            Object detailsObj = document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();

            // Verifica se details è una lista o un singolo documento
            if (detailsObj instanceof List) {
                // Se details è una lista, trattalo come tale
                List<Document> detailsList = (List<Document>) detailsObj;
                for (Document detailDoc : detailsList) {
                    String productId = detailDoc.getString("productId");
                    ProductDetail detail = new ProductDetail();
                    detail.setQuantity(detailDoc.getInteger("quantity"));
                    detail.setPrice(detailDoc.getDouble("price"));
                    detailsMap.put(productId, detail);
                }
            } else if (detailsObj instanceof Document) {
                // Se details è un singolo document, trattalo come tale
                Document detailDoc = (Document) detailsObj;
                String productId = detailDoc.getString("productId");
                ProductDetail detail = new ProductDetail();
                detail.setQuantity(detailDoc.getInteger("quantity"));
                detail.setPrice(detailDoc.getDouble("price"));
                detailsMap.put(productId, detail);
            } else {
                // Gestisci il caso in cui details non è né una lista né un document
                throw new IllegalStateException("Unexpected structure for details");
            }

            order.setDetails(detailsMap);  // Popola la mappa con i dettagli

            return order;
        }

        return null;
    }

    public void update(OrderMongo order) {
        // Converte la mappa dei dettagli in una lista di Document
        List<Document> detailsList = new ArrayList<>();
        for (ProductDetail detail : order.getDetails().values()) {
            Document detailDoc = new Document();
            detailDoc.append("name", detail.getName());  // Assicurati che il nome del campo corrisponda a quello nel DB
            detailDoc.append("quantity", detail.getQuantity());
            detailDoc.append("price", detail.getPrice());
            detailsList.add(detailDoc);
        }

        // Crea il documento di aggiornamento
        Document updatedOrder = new Document()
                .append("userEmail", order.getUserEmail())
                .append("details", detailsList)  // Usa la lista di dettagli
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", order.getOrderDate())  // Assicurati di usare la funzione corretta per la conversione
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());

        // Esegui l'aggiornamento dell'ordine nel database
        MongoCollection<Document> collection = getOrdersCollection();
        collection.updateOne(
                new Document("_id", order.getId()),  // Cerca l'ordine per ID
                new Document("$set", updatedOrder)   // Imposta i nuovi valori
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

}
