package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.*;
import it.itsincom.webdev2023.persistence.repository.OrderMongoRepository;
import it.itsincom.webdev2023.persistence.repository.ProductRepository;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bson.types.ObjectId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

        order.setOrderDate(new java.util.Date());

        if (!orderMongoRepository.isDeliveryDateAvailable(order.getDeliverDate())) {
            return false;
        }

        for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
            String productId = entry.getKey();
            int requestedQuantity = entry.getValue().getQuantity();

            try {
                int productIdInt = Integer.parseInt(productId);
                Product product = productRepository.findProductById(productIdInt);

                if (product == null || product.getStock() < requestedQuantity) {
                    System.out.println("Prodotto non disponibile o stock insufficiente per l'ID: " + productId);
                    return false;
                } else {
                    product.setStock(product.getStock() - requestedQuantity);
                    productRepository.updateProduct(product);
                }

                ProductDetail orderDetail = new ProductDetail();
                orderDetail.setName(product.getName());
                orderDetail.setPrice(product.getPrice());
                orderDetail.setQuantity(requestedQuantity);
                order.addOrderDetail(productId, orderDetail);

                totalPrice += product.getPrice() * requestedQuantity;
            } catch (NumberFormatException e) {
                System.out.println("Errore: l'ID del prodotto non è valido (non è un numero) - " + productId);
                return false;
            }
        }

        order.setTotalPrice(totalPrice);
        order.setStatus("pending");
        orderMongoRepository.save(order);

        User client = userRepository.getUserByEmail(order.getUserEmail());
        String mailText = "Ciao " + client.getName() + ",\n"
                + "Abbiamo ricevuto il tuo ordine! Qui ci sono i dettagli:\n"
                + "Data di consegna: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "\n"
                + "Prezzo totale: " + Math.round(order.getTotalPrice() * 100.0) / 100.0 + "€\n"
                + "Stato: " + order.getStatus() + "\n";

        mailService.sendVerificationEmail(client.getEmail(), client.getName(), mailText);

        User admin = userRepository.getAdmin();
        String mailTextAdmin = "Ciao " + admin.getName() + ",\n"
                + "Un nuovo ordine è stato effettuato! Qui ci sono i dettagli:\n"
                + "Cliente: " + client.getName() + ".\n"
                + "Email: " + client.getEmail() + "\n"
                + "Data di consegna: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "\n";

        mailService.sendVerificationEmail(admin.getEmail(), admin.getName(), mailTextAdmin);

        return true;
    }

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
            return false;
        }

        if (order.getStatus().equals("accepted")) {
            return false;
        }

        order.setStatus("accepted");
        orderMongoRepository.update(order);

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
            return false;
        }
        if (order.getStatus().equals("delivered")) {
            return false;
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
        existingOrder.setComment(updatedOrder.getComment());
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
        OrderMongo order = orderMongoRepository.findById(orderId);
        if (order != null) {
            for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
                String productId = entry.getKey();
                ProductDetail productDetail = entry.getValue();
                Product product = productRepository.findProductById(Integer.parseInt(productId));
                if (product != null) {
                    productDetail.setName(product.getName());
                }
                System.out.println("Product Name: " + productDetail.getName());
            }
        }
        return order;
    }

    public List<OrderMongo> getOrdersByUser(String email) {
        return orderMongoRepository.findByUserEmail(email);
    }

    public List<OrderMongo> getOrdersByDate(String date) {
        return orderMongoRepository.findByDateRange(date);
    }

    public ByteArrayOutputStream getExcel(String date) throws IOException {
        return  orderMongoRepository. getExcel(date);
    }
}
