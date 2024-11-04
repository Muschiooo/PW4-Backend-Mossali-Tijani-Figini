package it.itsincom.webdev2023.service;

import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.SQLException;

@ApplicationScoped
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    private CreateUserResponse convertToResponse(User user) {
        CreateUserResponse response = new CreateUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setOrders(user.getOrders());
        response.setComments(user.getComments());
        return response;
    }

    public CreateUserResponse getUserById(int userId) throws SQLException {
        User u = userRepository.getUserById(userId);
        CreateUserResponse ur = convertToResponse(u);
        return ur;
    }
}
