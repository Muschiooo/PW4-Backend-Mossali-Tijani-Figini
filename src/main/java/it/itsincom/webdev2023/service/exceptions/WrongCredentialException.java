package it.itsincom.webdev2023.service.exceptions;

public class WrongCredentialException extends Exception {
    public WrongCredentialException() {
        super("Wrong credentials.");
    }
}
