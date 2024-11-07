package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.persistence.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class ProductService {
    @Inject
    ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.createProduct(product);
    }

    public Product findProductById(int id) {
        return productRepository.findProductById(id);
    }

    public Product findProductByName(String name) {
        return productRepository.findProductByName(name);
    }

   public List<Product> getAllProducts() {
        return productRepository.getAllProducts();
   }

    public void updateProductStock(Product product) throws SQLException {
        productRepository.updateProductStock(product);
    }

   public Product updateProduct(Product product) {
        return productRepository.updateProduct(product);
   }

    public void deleteProduct(int id) {
          productRepository.deleteProduct(id);
    }
}
