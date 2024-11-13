package it.itsincom.webdev2023.service.exceptions;

import java.sql.SQLException;

public class TokenVerificationException extends Throwable {
    public TokenVerificationException(String databaseErrorDuringTokenVerification, Exception e) {
        super("Token verification failed: " + databaseErrorDuringTokenVerification, e);
    }
}
