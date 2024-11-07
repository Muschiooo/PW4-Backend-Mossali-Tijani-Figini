package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.*;
import it.itsincom.webdev2023.persistence.repository.OrderMongoRepository;
import it.itsincom.webdev2023.persistence.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderMongoRepository orderMongoRepository;

    @Inject
    ProductRepository productRepository;

    @Transactional
    public boolean createOrder(OrderMongo order) {
        double totalPrice = 0.0;

        // Imposta la data di creazione dell'ordine
        order.setOrderDate(orderMongoRepository.getCurrentDateTimeString());

        for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
            String productId = entry.getKey();
            int requestedQuantity = entry.getValue().getQuantity(); // Corretto per usare il getter

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

        return true;
    }


    // Funzione di validazione dell'ObjectId
    private boolean isValidObjectId(String id) {
        return id != null && id.length() == 24 && id.matches("[a-fA-F0-9]{24}");
    }

    @Transactional
    public boolean acceptOrder(String id) {
        if (!isValidObjectId(id)) {
            // Ritorna un errore se l'ID non è valido
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo order = orderMongoRepository.findById(String.valueOf(new ObjectId(id)));
        if (order == null) {
            return false;  // Ordine non trovato
        }

        order.setStatus("accepted");
        orderMongoRepository.update(order);  // Aggiorna lo stato dell'ordine
        return true;
    }


    @Transactional
    public boolean deliverOrder(String id) {
        OrderMongo order = orderMongoRepository.findById(String.valueOf(new ObjectId(id)));
        if (order == null) {
            return false;
        }
        order.setStatus("delivered");
        orderMongoRepository.update(order);
        return true;
    }

    @Transactional
    public boolean updateOrder(String id, OrderMongo updatedOrder) {
        if (!isValidObjectId(id)) {
            throw new IllegalArgumentException("ID non valido: " + id);
        }

        OrderMongo existingOrder = orderMongoRepository.findById(String.valueOf(new ObjectId(id)));
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

        OrderMongo order = orderMongoRepository.findById(String.valueOf(new ObjectId(id)));
        if (order == null) {
            return false;
        }

        orderMongoRepository.delete(order);
        return true;
    }


    public OrderMongo getOrderById(String orderId) {
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
