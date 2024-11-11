package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;
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

    public boolean checkToken(String token) {
        try {
            LOGGER.log(Level.INFO, "Received token for verification: " + token);

            User user = userRepository.findByVerificationToken(token);

            if (user != null) {
                LOGGER.log(Level.INFO, "User found: " + user.getEmail() + ", verification status: " + user.getVerification());
                if ("pending".equals(user.getVerification())) {
                    userRepository.verifyUser(user.getId());
                    LOGGER.log(Level.INFO, "User verified successfully: " + user.getEmail());
                    return true;
                } else {
                    LOGGER.log(Level.WARNING, "User already verified or token expired for user: " + user.getEmail());
                }
            } else {
                LOGGER.log(Level.WARNING, "No user found with the provided token: " + token);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying token: " + token, e);
            return false;
        }
    }

    private CreateUserResponse convertToResponse(User user) {
        CreateUserResponse response = new CreateUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        return response;
    }

    public CreateUserResponse getUserById(int userId) throws SQLException {
        User u = userRepository.getUserById(userId);
        CreateUserResponse ur = convertToResponse(u);
        return ur;
    }
}
