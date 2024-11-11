package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.MailService;
import it.itsincom.webdev2023.persistence.model.Session;
import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.SessionRepository;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import it.itsincom.webdev2023.rest.model.CreateUserRequest;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.service.exceptions.NotVerifiedException;
import it.itsincom.webdev2023.service.exceptions.SessionCreatedException;
import it.itsincom.webdev2023.service.exceptions.WrongCredentialException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    @Inject
    UserRepository userRepository;
    @Inject
     HashCalculator hashCalculator;
    @Inject
    SessionRepository sessionRepository;
    @Inject
    UserService userService;
    @Inject
    MailService mailService;

    public int login(String email, String password) throws NotVerifiedException, WrongCredentialException, SessionCreatedException {
        String hash = hashCalculator.calculateHash(password);
        Optional<User> maybeUser = userRepository.findByCredentials(email, hash);

        if (maybeUser.isPresent()) {
            LOGGER.log(Level.INFO, "User found: " + email);
            User user = maybeUser.get();
            if ("pending".equals(user.getVerification())) {
                LOGGER.log(Level.INFO, "User not verified: " + email);
                throw new NotVerifiedException();
            }
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

    public CreateUserResponse register(CreateUserRequest userRequest)  throws SQLException {
        try {
            String password = userRequest.getPassword();
            String hash = hashCalculator.calculateHash(password);
            String token = userService.generateVerificationToken();

            User user = new User();
            user.setName(userRequest.getName());
            user.setEmail(userRequest.getEmail());
            user.setPasswordHash(hash);
            user.setPhoneNumber(userRequest.getPhoneNumber());
            user.setRole("client");
            user.setVerification("pending");
            user.setVerificationToken(token);

            User createdUser = userRepository.createUser(user);

            createdUser.setVerificationToken(token);

            String mailText = "Ciao " + createdUser.getName() + ",\n"
                    + "Benvenuto in Pasticceria C'est la Vie! Per favore, clicca sul link sottostante per verificare il tuo account.\n"
                    + "Il tuo codice di verifica è: " + token + "\n"
                    + "Segui le istruzioni sul sito, inserisci il codice di verifica dell'account ed il gioco è fatto!\n"
                    + "http://localhost:3000/verify";

            mailService.sendVerificationEmail(createdUser.getEmail(), createdUser.getName(),  mailText);

            CreateUserResponse response = new CreateUserResponse();
            response.setId(createdUser.getId());
            response.setName(createdUser.getName());
            response.setEmail(createdUser.getEmail());
            response.setPhoneNumber(createdUser.getPhoneNumber());
            response.setRole(createdUser.getRole());
            response.setVerification(createdUser.getVerification());
            response.setVerificationToken(createdUser.getVerificationToken());
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while registering user", e);
            throw new RuntimeException("Error while registering user", e);
        }
    }

    public void logout(int sessionId) {
        sessionRepository.delete(sessionId);
    }

    public CreateUserResponse getProfile(int sessionId) throws SQLException {
        Session s = sessionRepository.getSessionById(sessionId);
        int userId = s.getUserId();
        CreateUserResponse user = userService.getUserById(userId);

        CreateUserResponse response = new CreateUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());

        return response;
    }
}
