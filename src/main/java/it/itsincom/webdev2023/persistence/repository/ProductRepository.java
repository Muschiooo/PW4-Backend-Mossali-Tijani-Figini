package it.itsincom.webdev2023.persistence.repository;

import it.itsincom.webdev2023.persistence.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ProductRepository {

    @Inject
    DataSource dataSource;

    private static final Logger LOGGER = Logger.getLogger(ProductRepository.class.getName());

    // Metodo per creare un nuovo prodotto
    public Product createProduct(Product product) {
        String sql = "INSERT INTO warehouse.product (name, description, price, stock, image, availability) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setDouble(3, product.getPrice());
            statement.setInt(4, product.getStock());
            statement.setString(5, product.getImage());
            statement.setString(6, calculateAvailability(product.getStock()));  // Usare metodo helper

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            // Recupera l'ID generato automaticamente
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating product in the database", e);
            throw new RuntimeException("Error creating product in the database", e);
        }
        return product;
    }

    // Trova un prodotto per ID
    public Product findProductById(int id) {
        String sql = "SELECT id, name, description, price, stock, image, availability FROM warehouse.product WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding product in the database", e);
            throw new RuntimeException("Error finding product in the database", e);
        }
        return null;
    }

    // Aggiorna stock e disponibilità di un prodotto
    public void updateProductStock(Product product) {
        String sql = "UPDATE warehouse.product SET stock = ?, availability = ? WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, product.getStock());
            statement.setString(2, calculateAvailability(product.getStock()));
            statement.setInt(3, product.getId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating product stock failed, no rows affected.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating product stock in the database", e);
            throw new RuntimeException("Error updating product stock in the database", e);
        }
    }

    // Trova un prodotto per nome
    public Product findProductByName(String name) {
        String sql = "SELECT id, name, description, price, stock, image, availability FROM warehouse.product WHERE name = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToProduct(resultSet);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding product by name in the database", e);
            throw new RuntimeException("Error finding product by name in the database", e);
        }
        return null;
    }

    // Ottiene tutti i prodotti
    public List<Product> getAllProducts() {
        String sql = "SELECT id, name, description, price, stock, image, availability FROM warehouse.product";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<Product> products = new ArrayList<>();
            while (resultSet.next()) {
                products.add(mapResultSetToProduct(resultSet));
            }
            return products;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all products from the database", e);
            throw new RuntimeException("Error fetching all products from the database", e);
        }
    }

    // Aggiorna tutti i dettagli di un prodotto
    public Product updateProduct(Product product) {
        String sql = "UPDATE warehouse.product SET name = ?, description = ?, price = ?, stock = ?, image = ?, availability = ? WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setDouble(3, product.getPrice());
            statement.setInt(4, product.getStock());
            statement.setString(5, product.getImage());
            statement.setString(6, calculateAvailability(product.getStock()));
            statement.setInt(7, product.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating product in the database", e);
            throw new RuntimeException("Error updating product in the database", e);
        }
        return product;
    }

    // Cancella un prodotto per nome
    public void deleteProduct(int id) {
        String sql = "DELETE FROM warehouse.product WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "id");
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting product from the database", e);
            throw new RuntimeException("Error deleting product from the database", e);
        }
    }

    // Helper per calcolare la disponibilità
    private String calculateAvailability(int stock) {
        return stock > 0 ? "available" : "out of stock";
    }

    // Helper per mappare il ResultSet su un oggetto Product
    private Product mapResultSetToProduct(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getInt("id"));
        product.setName(resultSet.getString("name"));
        product.setDescription(resultSet.getString("description"));
        product.setPrice(resultSet.getDouble("price"));
        product.setStock(resultSet.getInt("stock"));
        product.setImage(resultSet.getString("image"));
        product.setAvailability(resultSet.getString("availability"));
        return product;
    }
}
