package it.itsincom.webdev2023.persistence.repository;

import it.itsincom.webdev2023.persistence.exception.SessionNotFoundException;
import it.itsincom.webdev2023.persistence.model.Session;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class SessionRepository {
    private final DataSource dataSource;

    public SessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int insertSession(int idUser) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO session (user_id) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUser);
                ps.executeUpdate();
                ResultSet key = ps.getGeneratedKeys();
                if (key.next()) {
                    return key.getInt(1);
                } else {
                    throw new SQLException("No ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Cannot insert new session for user " + idUser, e);
        }
    }


    public void delete(int sessionId) {
        try {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM session WHERE id = ?")) {
                    statement.setInt(1, sessionId);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Session getSessionById(int sessionId) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, user_id, created_at FROM session WHERE id = ?")) {
                statement.setInt(1, sessionId);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    var sessione = new Session();
                    sessione.setId(rs.getInt("id"));
                    sessione.setUserId(rs.getInt("user_id"));
                    sessione.setCreatedAt(rs.getTimestamp("created_at"));
                    return sessione;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new SessionNotFoundException("Session not found with id: " + sessionId);
    }
}
