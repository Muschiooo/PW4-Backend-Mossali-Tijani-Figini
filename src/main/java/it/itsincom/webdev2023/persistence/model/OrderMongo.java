package it.itsincom.webdev2023.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Map;

@MongoEntity(collection="orders")
public class OrderMongo {
    public ObjectId id;
    public String userEmail;
    public Map<String, ProductDetail> details;  // Dettagli dell'ordine (Map)
    public double totalPrice;
    public Date orderDate;
    public Date deliverDate;
    public String status;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Map<String, ProductDetail> getDetails() {
        return details;
    }

    public void setDetails(Map<String, ProductDetail> details) {
        this.details = details;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getDeliverDate() {
        return deliverDate;
    }

    public void setDeliverDate(Date deliverDate) {
        this.deliverDate = deliverDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addOrderDetail(String productId, ProductDetail orderDetail) {
        this.details.put(productId, orderDetail);
    }

}