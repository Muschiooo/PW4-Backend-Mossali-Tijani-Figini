package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.persistence.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ProductService {
    @Inject
    ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.createProduct(product);
    }

    public Product findProductByName(String name) {
        return productRepository.findProductByName(name);
    }

   public List<Product> getAllProducts() {
        return productRepository.getAllProducts();
    }
}
