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

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(order.getOrderDate());
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 3);
        java.util.Date proposedDeliveryDate = calendar.getTime();

        proposedDeliveryDate = checkDeliveryDate(proposedDeliveryDate);

        order.setDeliverDate(proposedDeliveryDate);

        for (Map.Entry<String, ProductDetail> entry : order.getDetails().entrySet()) {
            String productId = entry.getKey();
            int requestedQuantity = entry.getValue().getQuantity();

            Product product = productRepository.findProductById(Integer.parseInt(productId));

            if (product == null || product.getStock() < requestedQuantity) {
                System.out.println("Product not available or insufficient stock for product ID: " + productId);
                return false;
            }

            ProductDetail orderDetail = new ProductDetail();
            orderDetail.setName(product.getName());
            orderDetail.setPrice(product.getPrice());
            orderDetail.setQuantity(requestedQuantity);

            order.addOrderDetail(productId, orderDetail);

            totalPrice += product.getPrice() * requestedQuantity;
        }

        order.setTotalPrice(totalPrice);
        order.setStatus("pending");
        orderMongoRepository.save(order);

        User client = userRepository.getUserByEmail(order.getUserEmail());

        String mailText = "Ciao " + client.getName() + ",\n"
                + "Abbiamo ricevuto il tuo ordine! Qui ci sono i dettagli:\n"
                + "Hai lasciato una nota per l'ordine: " + order.getComment() + "\n"
                + "Data di creazione: " + orderMongoRepository.dateTimeFormatter(order.getOrderDate()) + "\n"
                + "Data di ritiro: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "\n"
                + "Prezzo totale: " + Math.round(order.getTotalPrice() * 100.0) / 100.0 + "€\n"
                + "Stato: " + order.getStatus() + "\n"
                + "Sarai avvisato quando l'ordine verrà accettato e preparato.\n"
                + "Grazie per aver scelto Pasticceria C'est la Vie!";

        mailService.sendVerificationEmail(client.getEmail(), client.getName(), mailText);

        User admin = userRepository.getAdmin();

        String mailTextAdmin = "Ciao " + admin.getName() + ",\n"
                + "Un nuovo ordine è stato effettuato! Qui ci sono i dettagli:\n"
                + "Cliente: " + client.getName() + ".\n"
                + "Email: " + client.getEmail() + "\n"
                + "Telefono: " + client.getPhoneNumber() + "\n"
                + "Ha lasciato una nota per l'ordine: " + order.getComment() + "\n"
                + "Data di creazione: " + orderMongoRepository.dateTimeFormatter(order.getOrderDate()) + "\n"
                + "Data di ritiro: " + orderMongoRepository.dateTimeFormatter(order.getDeliverDate()) + "\n"
                + "Prezzo totale: " + Math.round(order.getTotalPrice() * 100.0) / 100.0 + "€\n"
                + "Stato: " + order.getStatus() + "\n"
                + "Controlla la dashboard per accettare l'ordine.\n";

        mailService.sendVerificationEmail(admin.getEmail(), admin.getName(), mailTextAdmin);

        return true;
    }

    public java.util.Date checkDeliveryDate(java.util.Date proposedDate) {
        OrderMongo lastOrder = orderMongoRepository.findLatestDeliveryDate();

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        if (lastOrder != null && lastOrder.getDeliverDate() != null) {
            java.util.Date lastDeliveryDate = lastOrder.getDeliverDate();

            calendar.setTime(lastDeliveryDate);
            calendar.add(java.util.Calendar.MINUTE, 10);

            if (proposedDate.before(calendar.getTime())) {
                proposedDate = calendar.getTime();
            }
        } else {
            calendar.setTime(proposedDate);
        }

        int minutes = calendar.get(java.util.Calendar.MINUTE);
        int remainder = minutes % 10;
        if (remainder != 0) {
            calendar.add(java.util.Calendar.MINUTE, 10 - remainder);
        }

        return calendar.getTime();
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
}
