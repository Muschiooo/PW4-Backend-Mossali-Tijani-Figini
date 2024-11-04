package it.itsincom.webdev2023.service.exceptions;

import java.sql.SQLException;

public class SessionCreatedException extends Exception {
    public SessionCreatedException(SQLException e) {
        super(e);
    }
}
