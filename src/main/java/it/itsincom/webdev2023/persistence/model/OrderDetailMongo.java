package it.itsincom.webdev2023.persistence.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity(collection = "order_details")
public class OrderDetailMongo {
    public ObjectId id;
    public ObjectId productDetailId; // Assicurati che sia di tipo ObjectId
    public int quantity;

    // Getter e setter
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getProductDetailId() {
        return productDetailId;
    }

    public void setProductDetailId(ObjectId productDetailId) {
        this.productDetailId = productDetailId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}