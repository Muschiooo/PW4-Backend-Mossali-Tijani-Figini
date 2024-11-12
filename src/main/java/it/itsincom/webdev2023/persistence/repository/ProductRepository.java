package it.itsincom.webdev2023.persistence.repository;


import it.itsincom.webdev2023.persistence.model.Product;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        String sql = "INSERT INTO warehouse.product (name, description, ingredients, price, stock, image, availability) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setString(3, product.getIngredients());
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
        String sql = "SELECT id, name, description, ingredients, price, stock, image, availability FROM warehouse.product WHERE id = ?";

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
        String sql = "SELECT id, name, description, ingredients, price, stock, image, availability FROM warehouse.product WHERE name = ?";

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

    public List<Product> getAllProducts() {
        String sql = "SELECT id, name, description, product.ingredients, price, stock, image, availability FROM warehouse.product";

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
        String sql = "UPDATE warehouse.product SET name = ?, description = ?, ingredients = ?, price = ?, stock = ?, image = ?, availability = ? WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setString(3, product.getIngredients());
            statement.setDouble(4, product.getPrice()); // Corrected index
            statement.setInt(5, product.getStock());
            statement.setString(6, product.getImage());
            statement.setString(7, calculateAvailability(product.getStock()));
            statement.setInt(8, product.getId()); // Corrected index
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating product in the database", e);
            throw new RuntimeException("Error updating product in the database", e);
        }
        return product;
    }

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

    private String calculateAvailability(int stock) {
        return stock > 0 ? "available" : "out of stock";
    }

    private Product mapResultSetToProduct(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getInt("id"));
        product.setName(resultSet.getString("name"));
        product.setDescription(resultSet.getString("description"));
        product.setIngredients(resultSet.getString("ingredients"));
        product.setPrice(resultSet.getDouble("price"));
        product.setStock(resultSet.getInt("stock"));
        product.setImage(resultSet.getString("image"));
        product.setAvailability(resultSet.getString("availability"));
        return product;
    }

    public ByteArrayOutputStream getExcel() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Id", "Name", "Price", "Stock"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Product product : getAllProducts()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getPrice());
                row.createCell(3).setCellValue(product.getStock());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write workbook to the output stream
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream;
    }

}
