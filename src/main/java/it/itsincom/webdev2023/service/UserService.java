package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class UserService {
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    @Inject
    UserRepository userRepository;

    public String generateVerificationToken() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }

    // Metodo per verificare il token di un utente
    public boolean checkToken(String token) {
        try {
            // Log token ricevuto per confronto
            LOGGER.log(Level.INFO, "Received token for verification: " + token);

            // Trova l'utente associato al token
            User user = userRepository.findByVerificationToken(token);

            // Controlla se l'utente è stato trovato e il token è quello corretto
            if (user != null) {
                LOGGER.log(Level.INFO, "User found: " + user.getEmail() + ", verification status: " + user.getVerification());
                if ("pending".equals(user.getVerification())) {
                    // Aggiorna lo stato di verifica a "verified"
                    user.setVerification("verified");
                    user.setVerificationToken(null); // Rimuove il token una volta verificato
                    userRepository.updateUser(user);
                    LOGGER.log(Level.INFO, "User verified successfully: " + user.getEmail());
                    return true;
                } else {
                    LOGGER.log(Level.WARNING, "User already verified or token expired for user: " + user.getEmail());
                }
            } else {
                LOGGER.log(Level.WARNING, "No user found with the provided token: " + token);
            }
            return false; // Token non valido o utente già verificato
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying token: " + token, e);
            return false; // Errore durante la verifica
        }
    }
}
