package it.itsincom.webdev2023.persistence.repository;

import it.itsincom.webdev2023.persistence.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class UserRepository {
    @Inject
    DataSource dataSource;

    public User createUser(User user) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "INSERT INTO user (name, email, password, phone, role, verification, verification_token) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPasswordHash());
                statement.setString(4, user.getPhoneNumber());
                statement.setString(5, "client");
                statement.setString(6, "pending");
                statement.setString(7, user.getVerificationToken());

                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        user.setId(id);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating user in the database", e);
        }
        return user;
    }

    public Optional<User> findByCredentials(String email, String hash) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT id, name, email, password, phone, role, verification, verification_token FROM user WHERE email = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);
                statement.setString(2, hash);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        User user = new User();
                        user.setId(resultSet.getInt("id"));
                        user.setName(resultSet.getString("name"));
                        user.setEmail(resultSet.getString("email"));
                        user.setPasswordHash(resultSet.getString("password"));
                        user.setPhoneNumber(resultSet.getString("phone"));
                        user.setRole(resultSet.getString("role"));
                        user.setVerification(resultSet.getString("verification"));
                        user.setVerificationToken(resultSet.getString("verification_token"));
                        return Optional.of(user);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving user by credentials", e);
        }
        return Optional.empty();
    }

    public User getUserById(int userId) throws SQLException {
        User user = null;
        String sql = "SELECT id, name, email, password, phone, role, verification, verification_token FROM user WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password"));
                    user.setPhoneNumber(rs.getString("phone"));
                    user.setRole(rs.getString("role"));
                    user.setVerification(rs.getString("verification"));
                    user.setVerificationToken(rs.getString("verification_token"));
                } else {
                    System.out.println("User not found with id: " + userId);
                }
            }
        }
        return user;
    }

    public User findByVerificationToken(String token) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT id, name, email, password, phone, role, verification, verification_token FROM user WHERE verification_token = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, token);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        User user = new User();
                        user.setId(resultSet.getInt("id"));
                        user.setName(resultSet.getString("name"));
                        user.setEmail(resultSet.getString("email"));
                        user.setPasswordHash(resultSet.getString("password"));
                        user.setPhoneNumber(resultSet.getString("phone"));
                        user.setRole(resultSet.getString("role"));
                        user.setVerification(resultSet.getString("verification"));
                        user.setVerificationToken(resultSet.getString("verification_token"));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving user by verification token", e);
        }
        return null;
    }

    public void updateUser(User user) {
        try (Connection connection = dataSource.getConnection()) {
            String sql = "UPDATE user SET verification = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getVerification());
                statement.setInt(2, user.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user in the database", e);
        }
    }

}