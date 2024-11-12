package it.itsincom.webdev2023.persistence.repository;

import com.mongodb.client.FindIterable;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mongodb.client.model.Filters.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;


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
                    .append("name", entry.getValue().getName())
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
                            detail.setName(detailDoc.getString("name"));
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
                        detail.setName(detailDoc.getString("name"));
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

            List<Document> detailsList = (List<Document>) document.get("details");
            Map<String, ProductDetail> detailsMap = new HashMap<>();
            for (Document detailDoc : detailsList) {
                String productId = detailDoc.getString("productId");
                ProductDetail detail = new ProductDetail();
                detail.setName(detailDoc.getString("name"));
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
            detail.setName(detailDoc.getString("name"));
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

    public List<OrderMongo> findByDateRange(String date) {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date parameter cannot be null or empty");
        }

        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, formatter);

        ZonedDateTime startDate = localDate.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endDate = startDate.plusDays(1).minusSeconds(1);

        for (Document document : collection.find(and(
                gte("deliverDate", Date.from(startDate.toInstant())),
                lte("deliverDate", Date.from(endDate.toInstant()))
        ))) {
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
                detail.setName(detailDoc.getString("name"));
                detail.setQuantity(detailDoc.getInteger("quantity"));
                detail.setPrice(detailDoc.getDouble("price"));
                detailsMap.put(productId, detail);
            }
            order.setDetails(detailsMap);
            orders.add(order);
        }
        return orders;
    }


    public ByteArrayOutputStream getExcel(String date) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");

            // Define header styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Define status styles
            CellStyle acceptedStyle = workbook.createCellStyle();
            acceptedStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            acceptedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle pendingStyle = workbook.createCellStyle();
            pendingStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            pendingStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle deliveredStyle = workbook.createCellStyle();
            deliveredStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            deliveredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Order ID", "User Email", "Total Price", "Order Date", "Deliver Date", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Date formatter for display
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // Fill data rows
            int rowNum = 1;
            for (OrderMongo order : findByDateRange(date)) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(order.getId().toString());
                row.createCell(1).setCellValue(order.getUserEmail());
                row.createCell(2).setCellValue(order.getTotalPrice());
                row.createCell(3).setCellValue(order.getOrderDate().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));
                row.createCell(4).setCellValue(order.getDeliverDate().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));

                // Set cell style based on status
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(order.getStatus());
                switch (order.getStatus().toLowerCase()) {
                    case "accepted":
                        statusCell.setCellStyle(acceptedStyle);
                        break;
                    case "pending":
                        statusCell.setCellStyle(pendingStyle);
                        break;
                    case "delivered":
                        statusCell.setCellStyle(deliveredStyle);
                        break;
                    default:
                        // Leave it with the default style if no match
                        break;
                }

                // Add products without individual prices
                int detailCol = 6; // Start column for product details
                for (ProductDetail detail : order.getDetails().values()) {
                    Cell productNameCell = row.createCell(detailCol++);
                    productNameCell.setCellValue(detail.getName());

                    Cell quantityCell = row.createCell(detailCol++);
                    quantityCell.setCellValue(detail.getQuantity());
                }
            }

            for (int i = 0; i < headers.length + 10; i++) { // Adjust "+10" based on expected number of product columns
                sheet.autoSizeColumn(i);
            }

            // Write workbook to the output stream
            workbook.write(outputStream);
        }

        return outputStream;
    }
}

