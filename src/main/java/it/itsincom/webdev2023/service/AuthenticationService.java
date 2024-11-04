package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.SessionRepository;

import it.itsincom.webdev2023.persistence.repository.UserRepository;
import it.itsincom.webdev2023.rest.model.CreateUserRequest;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.service.exceptions.SessionCreatedException;
import it.itsincom.webdev2023.service.exceptions.WrongCredentialException;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private final UserRepository userRepository;
    private final HashCalculator hashCalculator;
    private final SessionRepository sessionRepository;
    private final UserService userService;

    public AuthenticationService(UserRepository userRepository, HashCalculator hashCalculator, SessionRepository sessionRepository, UserService userService) {
        this.userRepository = userRepository;
        this.hashCalculator = new HashCalculator();
        this.sessionRepository = sessionRepository;
        this.userService = userService;
    }


    public int login(String email, String password) throws WrongCredentialException, SessionCreatedException {

        String hash = hashCalculator.calculateHash(password);

        Optional<User> maybeUser = userRepository.findByCredentials(email, hash);
        if (maybeUser.isPresent()) {
            LOGGER.log(Level.INFO, "User found: " + email);
            User user = maybeUser.get();
            try {
                int session = sessionRepository.insertSession(user.getId());
                LOGGER.info("Session created with ID: " + session);
                return session;
            } catch (SQLException e) {
                LOGGER.severe("Failed to create session.");
                throw new SessionCreatedException(e);
            }

        } else {
            LOGGER.log(Level.INFO, "User not found: " + email);
            throw new WrongCredentialException();
        }
    }

    public CreateUserResponse register(CreateUserRequest userRequest) {
        try {
            String password = userRequest.getPassword();
            String hash = hashCalculator.calculateHash(password);

            User user = new User();
            user.setName(userRequest.getName());
            user.setEmail(userRequest.getEmail());
            user.setPasswordHash(hash);

            User createdUser = userRepository.createUser(user);


            CreateUserResponse response = new CreateUserResponse();

            response.setId(user.getId());
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            return response;

        } catch (Exception e) {
            // Log the exception
            e.printStackTrace();
            throw new RuntimeException("Error while registering user", e);
        }
    }

    public void logout(int sessionId) {
        sessionRepository.delete(sessionId);
    }

}