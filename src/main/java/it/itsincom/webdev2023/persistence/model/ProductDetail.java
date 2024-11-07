package it.itsincom.webdev2023.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection="order_details")
public  class ProductDetail {
    public String name;
    public int quantity;
    public double price;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}