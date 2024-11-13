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

    public Product createProduct(Product product) {
        String sql = "INSERT INTO warehouse.product (name, description, ingredients, price, stock, image, availability) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                setProductStatement(statement, product);
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating product failed, no rows affected.");
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) product.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating product in the database", e);
            throw new RuntimeException("Error creating product in the database", e);
        }
        return product;
    }

    public Product findProductById(int id) {
        String sql = "SELECT id, name, description, ingredients, price, stock, image, availability FROM warehouse.product WHERE id = ?";
        return findProduct(sql, id);
    }

    public Product findProductByName(String name) {
        String sql = "SELECT id, name, description, ingredients, price, stock, image, availability FROM warehouse.product WHERE name = ?";
        return findProduct(sql, name);
    }

    private Product findProduct(String sql, Object param) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (param instanceof Integer) {
                statement.setInt(1, (Integer) param);
            } else if (param instanceof String) {
                statement.setString(1, (String) param);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return mapResultSetToProduct(resultSet);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding product in the database", e);
            throw new RuntimeException("Error finding product in the database", e);
        }
        return null;
    }

    public void updateProductStock(Product product) {
        String sql = "UPDATE warehouse.product SET stock = ?, availability = ? WHERE id = ?";
        executeUpdate(sql, product.getStock(), calculateAvailability(product.getStock()), product.getId());
    }

    public Product updateProduct(Product product) {
        String sql = "UPDATE warehouse.product SET name = ?, description = ?, ingredients = ?, price = ?, stock = ?, image = ?, availability = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            setProductStatement(statement, product);
            statement.setInt(8, product.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating product in the database", e);
            throw new RuntimeException("Error updating product in the database", e);
        }
        return product;
    }

    public void deleteProduct(int id) {
        String sql = "DELETE FROM warehouse.product WHERE id = ?";
        executeUpdate(sql, id);
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) {
                    statement.setInt(i + 1, (Integer) params[i]);
                } else if (params[i] instanceof String) {
                    statement.setString(i + 1, (String) params[i]);
                }
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database update error", e);
            throw new RuntimeException("Database update error", e);
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

    private void setProductStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getName());
        statement.setString(2, product.getDescription());
        statement.setString(3, product.getIngredients());
        statement.setDouble(4, product.getPrice());
        statement.setInt(5, product.getStock());
        statement.setString(6, product.getImage());
        statement.setString(7, calculateAvailability(product.getStock()));
    }

    public List<Product> getAllProducts() {
        String sql = "SELECT id, name, description, ingredients, price, stock, image, availability FROM warehouse.product";
        List<Product> products = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapResultSetToProduct(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all products from the database", e);
            throw new RuntimeException("Error fetching all products from the database", e);
        }
        return products;
    }

    public ByteArrayOutputStream getExcel() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");
            createHeader(sheet);
            fillData(sheet);
            autoSizeColumns(sheet);
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream;
    }

    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Id", "Name", "Price", "Stock"};
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillData(Sheet sheet) {
        int rowNum = 1;
        for (Product product : getAllProducts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getName());
            row.createCell(2).setCellValue(product.getPrice());
            row.createCell(3).setCellValue(product.getStock());
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
