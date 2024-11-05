package it.itsincom.webdev2023.service.exceptions;

public class NotVerifiedException extends Exception {
    public NotVerifiedException() {
        super("User not verified.");
    }
}
