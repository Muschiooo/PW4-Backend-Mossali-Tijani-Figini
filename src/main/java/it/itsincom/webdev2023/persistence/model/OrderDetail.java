package it.itsincom.webdev2023.persistence.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_details")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Modifica il tipo da Order a OrderMongo
    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderMongo order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductDetail productDetail;

    private int quantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderMongo getOrder() {
        return order;
    }

    public void setOrder(OrderMongo order) {
        this.order = order;
    }

    public ProductDetail getProductDetail() {
        return productDetail;
    }

    public void setProductDetail(ProductDetail productDetail) {
        this.productDetail = productDetail;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
