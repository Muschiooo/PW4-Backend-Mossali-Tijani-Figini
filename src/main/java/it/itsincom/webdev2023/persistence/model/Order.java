package it.itsincom.webdev2023.persistence.model;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class Order {
    private int id;
    private int userId;
    private List<Product> product;
    private LocalDateTime dateTime;
    private Status Status;
}
