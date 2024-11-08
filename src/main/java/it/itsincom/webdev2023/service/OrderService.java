package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.*;
import it.itsincom.webdev2023.persistence.repository.OrderMongoRepository;
import it.itsincom.webdev2023.persistence.repository.ProductRepository;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bson.types.ObjectId;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OrderService {
    @Inject
    OrderMongoRepository orderMongoRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    ProductRepository productRepository;
    @Inject
    MailService mailService;

    @Transactional
    public boolean createOrder(OrderMongo order) throws SQLException {
        double totalPrice = 0.0;

        // Imposta la data di creazione dell'ordine
        order.setOrderDate(new java.util.Date());

        // Calcola la data di consegna (3 giorni dopo la data di creazione)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(order.getOrderDate());
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 3);  // Aggiungi 3 giorni
        java.util.Date proposedDeliveryDate = calendar.getTime();

        // Verifica se l'orario di consegna è già occupato
        proposedDeliveryDate = checkDeliveryDate(proposedDeliveryDate);

        // Imposta la data di consegna nell'ordine
        order.setDeliverDate(proposedDeliveryDate);

        for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
            String productId = entry.getKey();
            int requestedQuantity = entry.getValue().getQuantity();  // Corretto per usare il getter

            // Trova il prodotto tramite l'ID del prodotto
            Product product = productRepository.findProductById(Integer.parseInt(productId));

            if (product == null || product.getStock() < requestedQuantity) {
                System.out.println("Product not available or insufficient stock for product ID: " + productId);
                return false;
            }

            // Crea un dettaglio ordine per MongoDB
            ProductDetail orderDetail = new ProductDetail();
            orderDetail.setName(product.getName());  // Assegna il nome del prodotto
            orderDetail.setPrice(product.getPrice());  // Assegna il prezzo del prodotto
            orderDetail.setQuantity(requestedQuantity);  // Imposta la quantità

            // Aggiungi il dettaglio ordine all'ordine MongoDB
            order.addOrderDetail(productId, orderDetail);  // Usa productId come chiave

            totalPrice += product.getPrice() * requestedQuantity;
        }

        order.setTotalPrice(totalPrice);  // Imposta il prezzo totale
        order.setStatus("pending");  // Imposta lo stato dell'ordine
        orderMongoRepository.save(order);  // Salva l'ordine nel database MongoDB

        User client = userRepository.getUserByEmail(order.getUserEmail());

        String mailText = "Ciao " + client.getName() + ",\n"
                + "Abbiamo ricevuto il tuo ordine! Qui ci sono i dettagli:\n"
                + "Data di creazione: " + orderMongoRepository.dateTimeFormatter(order.getOrderDate()) + "\n"
                + "Data di ritiro: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "\n"
                + "Prezzo totale: " + Math.round(order.getTotalPrice() * 100.0) /100.0 + "€\n"
                + "Stato: " + order.getStatus() + "\n"
                + "Sarai avvisato quando l'ordine verrà accettato e preparato.\n"
                + "Grazie per aver scelto Pasticceria C'est la Vie!";

        mailService.sendVerificationEmail(client.getEmail(), client.getName(), mailText);

        User admin = userRepository.getAdmin();

        String mailTextAdmin = "Ciao " + admin.getName() + ",\n"
                + "Un nuovo ordine è stato effettuato! Qui ci sono i dettagli:\n"
                + "Data di creazione: " + orderMongoRepository.dateTimeFormatter(order.getOrderDate()) + "\n"
                + "Data di ritiro: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "€\n"
                + "Prezzo totale: " + Math.round(order.getTotalPrice() * 100.0) /100.0 + "\n"
                + "Stato: " + order.getStatus() + "\n"
                + "Controlla la dashboard per accettare l'ordine.\n";

        mailService.sendVerificationEmail(admin.getEmail(), admin.getName(), mailTextAdmin);

        return true;
    }

    public java.util.Date checkDeliveryDate(java.util.Date proposedDate) {
        // Recupera l'ultimo ordine dal database ordinato per data di consegna più recente
        OrderMongo lastOrder = orderMongoRepository.findLatestDeliveryDate();

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // Se esiste un ultimo ordine con data di consegna
        if (lastOrder != null && lastOrder.getDeliverDate() != null) {
            java.util.Date lastDeliveryDate = lastOrder.getDeliverDate();

            // Imposta il calendario alla data dell'ultimo ordine
            calendar.setTime(lastDeliveryDate);
            // Aggiungi 10 minuti
            calendar.add(java.util.Calendar.MINUTE, 10);

            // Se la nuova data proposta è prima della nuova data calcolata, aggiorna a quest'ultima
            if (proposedDate.before(calendar.getTime())) {
                proposedDate = calendar.getTime();
            }
        } else {
            // Se non ci sono ordini precedenti, imposta il calendario alla data proposta
            calendar.setTime(proposedDate);
        }

        // Arrotonda ai 10 minuti più vicini successivi
        int minutes = calendar.get(java.util.Calendar.MINUTE);
        int remainder = minutes % 10;
        if (remainder != 0) {
            calendar.add(java.util.Calendar.MINUTE, 10 - remainder);
        }

        // Ritorna la nuova data di consegna proposta
        return calendar.getTime();
    }


    // Funzione di validazione dell'ObjectId
    private boolean isValidObjectId(String id) {
        return id != null && id.length() == 24 && id.matches("[a-fA-F0-9]{24}");
    }

    @Transactional
    public boolean acceptOrder(String id) throws SQLException {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo order = orderMongoRepository.findById(new ObjectId(id));
        if (order == null) {
            return false;  // Ordine non trovato
        }

        if (order.getStatus().equals("accepted")) {
            return false;  // L'ordine è già "accepted", nessun aggiornamento necessario
        }

        order.setStatus("accepted");
        orderMongoRepository.update(order);  // Aggiorna lo stato dell'ordine

        User client = userRepository.getUserByEmail(order.getUserEmail());

        String mailText = "Ciao " + client.getName() + ",\n"
                + "Il tuo ordine è stato accettato! Qui ci sono i dettagli:\n"
                + "Data di creazione: " + order.getOrderDate() + "\n"
                + "Data di ritiro: " + order.getDeliverDate() + "\n"
                + "Prezzo totale: " + order.getTotalPrice() + "\n"
                + "Stato: " + order.getStatus() + "\n"
                + "Grazie per aver scelto Pasticceria C'est la Vie!";

        mailService.sendVerificationEmail(client.getEmail(), client.getName(), mailText);
        return true;
    }

    @Transactional
    public boolean deliverOrder(String id) throws SQLException {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo order = orderMongoRepository.findById(new ObjectId(id));
        if (order == null) {
            return false;  // Ordine non trovato
        }

        if (order.getStatus().equals("delivered")) {
            return false;  // L'ordine è già "delivered", nessun aggiornamento necessario
        }

        order.setStatus("delivered");
        orderMongoRepository.update(order);
        User client = userRepository.getUserByEmail(order.getUserEmail());

        String mailText = "Ciao " + client.getName() + ",\n"
                + "Il tuo ordine è stato ritirato!\n"
                + "Grazie ancora per aver scelto Pasticceria C'est la Vie!\n"
                + "Speriamo di vederti presto!";

        mailService.sendVerificationEmail(client.getEmail(), client.getName(), mailText);

        return true;
    }


    @Transactional
    public boolean updateOrder(String id, OrderMongo updatedOrder) {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo existingOrder = orderMongoRepository.findById(new ObjectId(id));
        if (existingOrder == null) {
            return false;
        }
        existingOrder.setUserEmail(updatedOrder.getUserEmail());
        existingOrder.setDetails(updatedOrder.getDetails());
        existingOrder.setTotalPrice(updatedOrder.getTotalPrice());
        existingOrder.setOrderDate(updatedOrder.getOrderDate());
        existingOrder.setDeliverDate(updatedOrder.getDeliverDate());
        existingOrder.setStatus(updatedOrder.getStatus());
        orderMongoRepository.update(existingOrder);
        return true;
    }

    @Transactional
    public boolean deleteOrder(String id) {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo order = orderMongoRepository.findById(new ObjectId(id));
        if (order == null) {
            return false;
        }

        orderMongoRepository.delete(order);
        return true;
    }

    public List<OrderMongo> getAllOrders() {
        return orderMongoRepository.findAll();
    }

    public OrderMongo getOrderById(ObjectId orderId) {
        OrderMongo order = orderMongoRepository.findById(orderId);  // Recupera l'ordine dal DB

        if (order != null) {
            // Itera attraverso i dettagli dell'ordine per ottenere i nomi dei prodotti
            for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
                String productId = entry.getKey();
                ProductDetail productDetail = entry.getValue();

                // Recupera il prodotto dal database (assumendo che tu abbia un repository per i prodotti)
                Product product = productRepository.findProductById(Integer.parseInt(productId));  // Metodo per ottenere il prodotto

                if (product != null) {
                    // Aggiungi il nome del prodotto al dettaglio dell'ordine
                    productDetail.setName(product.getName());
                }

                // Stampa il nome del prodotto (solo per debugging, puoi rimuoverlo dopo)
                System.out.println("Product Name: " + productDetail.getName());
            }
        }

        return order;  // Ritorna l'ordine con i dettagli aggiornati
    }

    public List<OrderMongo> getOrdersByUser(String email) {
        return orderMongoRepository.findByUserEmail(email);
    }
}
