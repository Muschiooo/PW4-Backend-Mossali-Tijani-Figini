package it.itsincom.webdev2023.persistence.repository;

import it.itsincom.webdev2023.persistence.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProductRepository {
    @Inject
    DataSource dataSource;
    public Product createProduct(Product product) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "INSERT INTO warehouse.product (name, description, price, stock, image) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, product.getName());
                statement.setString(2, product.getDescription());
                statement.setDouble(3, product.getPrice());
                statement.setInt(4, product.getStock());
                statement.setString(5, product.getImage());

                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating product failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        product.setId(id);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating product in the database", e);
        }
        return product;
    }

    public Product findProductByName(String name) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT id, name, description, price, stock, image FROM warehouse.product WHERE name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Product product = new Product();
                        product.setId(resultSet.getInt("id"));
                        product.setName(resultSet.getString("name"));
                        product.setDescription(resultSet.getString("description"));
                        product.setPrice(resultSet.getDouble("price"));
                        product.setStock(resultSet.getInt("stock"));
                        product.setImage(resultSet.getString("image"));
                        return product;
                    }
                }
            }
        } catch (SQLException e) {
            // Log the exception for debugging purposes
            e.printStackTrace();  // Log this exception properly using a logger in real-world applications
            throw new RuntimeException("Error finding product in the database", e);
        }
        return null;
    }

    public List<Product> getAllProducts() {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT id, name, description, price, stock, image FROM warehouse.product";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Product> products = new ArrayList<>();
                    while (resultSet.next()) {
                        Product product = new Product();
                        product.setId(resultSet.getInt("id"));
                        product.setName(resultSet.getString("name"));
                        product.setDescription(resultSet.getString("description"));
                        product.setPrice(resultSet.getDouble("price"));
                        product.setStock(resultSet.getInt("stock"));
                        product.setImage(resultSet.getString("image"));
                        products.add(product);
                    }
                    return products;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
