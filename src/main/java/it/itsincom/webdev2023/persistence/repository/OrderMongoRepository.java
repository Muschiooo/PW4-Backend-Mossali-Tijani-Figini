package it.itsincom.webdev2023.persistence.repository;

import com.mongodb.client.FindIterable;
import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.persistence.model.ProductDetail;
import it.itsincom.webdev2023.service.OrderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
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
    OrderService orderService;
    @Inject
    MongoClient mongoClient;

    private MongoCollection<Document> getOrdersCollection() {
        MongoDatabase database = mongoClient.getDatabase("pasticceria");
        return database.getCollection("orders");
    }

    private OrderMongo createOrderFromDocument(Document document) {
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

    private List<Document> convertProductDetailsToList(Map<String, ProductDetail> details) {
        List<Document> detailsList = new ArrayList<>();
        for (Map.Entry<String, ProductDetail> entry : details.entrySet()) {
            detailsList.add(createDetailDocument(entry.getKey(), entry.getValue()));
        }
        return detailsList;
    }

    private Document createDetailDocument(String productId, ProductDetail detail) {
        return new Document()
                .append("productId", productId)
                .append("name", detail.getName())
                .append("quantity", detail.getQuantity())
                .append("price", detail.getPrice());
    }

    public void save(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = getOrderDocument(order);
        collection.insertOne(document);
    }

    private Document getOrderDocument(OrderMongo order) {
        return new Document()
                .append("userEmail", order.getUserEmail())
                .append("comment", order.getComment())
                .append("details", convertProductDetailsToList(order.getDetails()))
                .append("totalPrice", order.getTotalPrice())
                .append("orderDate", order.getOrderDate())
                .append("deliverDate", order.getDeliverDate())
                .append("status", order.getStatus());
    }

    public List<OrderMongo> findAll() {
        MongoCollection<Document> collection = getOrdersCollection();
        List<OrderMongo> orders = new ArrayList<>();
        FindIterable<Document> findIterable = collection.find();

        for (Document document : findIterable) {
            orders.add(createOrderFromDocument(document));
        }

        return orders;
    }

    public OrderMongo findById(ObjectId id) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = collection.find(new Document("_id", id)).first();
        return document != null ? createOrderFromDocument(document) : null;
    }

    public void update(OrderMongo order) {
        MongoCollection<Document> collection = getOrdersCollection();
        Document document = getOrderDocument(order);
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
            orders.add(createOrderFromDocument(document));
        }
        return orders;
    }

    public String dateTimeFormatter(Date date) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'alle ore' HH.mm");
        return dateTime.format(formatter);
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
            orders.add(createOrderFromDocument(document));
        }
        return orders;
    }

    public ByteArrayOutputStream getExcel(String date) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");

            CellStyle headerStyle = createHeaderCellStyle(workbook);
            CellStyle acceptedStyle = createStatusCellStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle pendingStyle = createStatusCellStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle deliveredStyle = createStatusCellStyle(workbook, IndexedColors.LIGHT_BLUE);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Order ID", "User Email", "Total Price", "Order Date", "Deliver Date", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            int rowNum = 1;
            for (OrderMongo order : findByDateRange(date)) {
                Row row = sheet.createRow(rowNum++);
                createOrderRow(row, order, dateFormatter, acceptedStyle, pendingStyle, deliveredStyle);
            }

            for (int i = 0; i < headers.length + 10; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }

        return outputStream;
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private CellStyle createStatusCellStyle(Workbook workbook, IndexedColors color) {
        CellStyle statusStyle = workbook.createCellStyle();
        statusStyle.setFillForegroundColor(color.getIndex());
        statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return statusStyle;
    }

    private void createOrderRow(Row row, OrderMongo order, DateTimeFormatter dateFormatter,
                                CellStyle acceptedStyle, CellStyle pendingStyle, CellStyle deliveredStyle) {
        row.createCell(0).setCellValue(order.getId().toString());
        row.createCell(1).setCellValue(order.getUserEmail());
        row.createCell(2).setCellValue(order.getTotalPrice());
        row.createCell(3).setCellValue(order.getOrderDate().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));
        row.createCell(4).setCellValue(order.getDeliverDate().toInstant().atZone(ZoneId.systemDefault()).format(dateFormatter));

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
                break;
        }

        int detailCol = 6;
        for (ProductDetail detail : order.getDetails().values()) {
            row.createCell(detailCol++).setCellValue(detail.getName());
            row.createCell(detailCol++).setCellValue(detail.getQuantity());
        }
    }

    public boolean isDeliveryDateAvailable(Date proposedDate) {
        List<OrderMongo> orders = findAll();
        for (OrderMongo existingOrder : orders) {
            if (existingOrder.getDeliverDate().equals(proposedDate)) {
                return false;
            }
        }
        return true;
    }

    public Date suggestNextAvailableTime(Date proposedDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(proposedDate);

        if (calendar.get(Calendar.HOUR_OF_DAY) < 14 || calendar.get(Calendar.HOUR_OF_DAY) >= 18) {
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 0);
        }

        while (!isDeliveryDateAvailable(calendar.getTime()) && calendar.get(Calendar.HOUR_OF_DAY) <= 18) {
            calendar.add(Calendar.MINUTE, 10);
        }

        if (calendar.get(Calendar.HOUR_OF_DAY) > 18) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 0);
        }
        return calendar.getTime();
    }

    public Response validateDeliveryDate(OrderMongo order) {
        Date currentDate = new Date();

        if (!isDeliveryDateAvailable(order.getDeliverDate())) {
            Date suggestedDate = suggestNextAvailableTime(order.getDeliverDate());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"La data di consegna non è disponibile. Suggerimento: "
                            + dateTimeFormatter(suggestedDate) + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (order.getDeliverDate().before(currentDate)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"La data di consegna non può essere nel passato.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getDeliverDate());

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 14 || hour > 18) {
            Date suggestedDate = suggestNextAvailableTime(order.getDeliverDate());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"L'orario proposto deve essere tra le 14 e le 18. Suggerimento: "
                            + dateTimeFormatter(suggestedDate) + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return null;
    }


    public Response createNewOrder(OrderMongo order) throws SQLException {
        boolean success = orderService.createOrder(order);
        if (success) {
            return Response.ok("{\"message\":\"Order created successfully.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Error creating order.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
