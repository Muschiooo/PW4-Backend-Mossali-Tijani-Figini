package it.itsincom.webdev2023.persistence.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;
    private String userEmail;
    private double totalPrice;
    private String orderDate;
    private String deliverDate;
    private String status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> dettagli;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getDeliverDate() {
        return deliverDate;
    }

    public void setDeliverDate(String deliverDate) {
        this.deliverDate = deliverDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderDetail> getDettagli() {
        return dettagli;
    }

    public void setDettagli(List<OrderDetail> dettagli) {
        this.dettagli = dettagli;
    }
}


