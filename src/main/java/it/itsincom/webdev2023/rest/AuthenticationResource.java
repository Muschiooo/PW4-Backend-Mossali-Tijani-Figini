package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.User;
import it.itsincom.webdev2023.persistence.repository.UserRepository;
import it.itsincom.webdev2023.rest.model.CreateUserRequest;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.rest.model.LoginRequest;
import it.itsincom.webdev2023.service.AuthenticationService;
import it.itsincom.webdev2023.service.UserService;
import it.itsincom.webdev2023.service.exceptions.NotVerifiedException;
import it.itsincom.webdev2023.service.exceptions.SessionCreatedException;
import it.itsincom.webdev2023.service.exceptions.TokenVerificationException;
import it.itsincom.webdev2023.service.exceptions.WrongCredentialException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Path("/auth")
public class AuthenticationResource {
    @Inject
    AuthenticationService authenticationService;
    @Inject
    UserService userService;
    @Inject
    UserRepository userRepository;

    @POST
    @Path("/register")
    public Response register(CreateUserRequest user) {
        try {
            CreateUserResponse createdUser = authenticationService.register(user);
            return Response.ok(createdUser).build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyToken(Map<String, String> requestBody) throws TokenVerificationException {
        String token = requestBody.get("token");
        boolean isVerified = userService.checkToken(token);
        return isVerified
                ? Response.ok("{\"message\":\"User verified successfully\"}").build()
                : Response.status(Response.Status.BAD_REQUEST).entity("{\"message\":\"Invalid or expired token\"}").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest loginRequest) {
        try {
            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();
            int session = authenticationService.login(email, password);
            User user = userRepository.getUserByEmail(email);

            NewCookie sessionCookie = new NewCookie.Builder("SESSION_COOKIE")
                    .value(String.valueOf(session))
                    .path("/")
                    .build();

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Login effettuato con successo");
            responseBody.put("sessionId", session);
            responseBody.put("role", user.getRole());

            return Response.ok(responseBody)
                    .cookie(sessionCookie)
                    .build();
        } catch (NotVerifiedException e) {
            return createErrorResponse("Account non verificato", Response.Status.FORBIDDEN);
        } catch (WrongCredentialException e) {
            return createErrorResponse("Credenziali non valide", Response.Status.UNAUTHORIZED);
        } catch (SessionCreatedException e) {
            return createErrorResponse("Errore nella creazione della sessione", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @DELETE
    @Path("/logout")
    public Response logout(@CookieParam("SESSION_COOKIE") int sessionId) {
        authenticationService.logout(sessionId);
        NewCookie sessionCookie = new NewCookie.Builder("SESSION_COOKIE").path("/").build();
        return Response.ok().cookie(sessionCookie).build();
    }

    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfile(@CookieParam("SESSION_COOKIE") int sessionId) {
        try {
            CreateUserResponse user = authenticationService.getProfile(sessionId);
            return Response.ok(user).build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/clients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClients(@CookieParam("SESSION_COOKIE") int sessionId) throws SQLException {
        return checkAdminRole(sessionId)
                ? Response.ok(userRepository.getAllClients()).build()
                : Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
    }

    @DELETE
    @Path("/delete/{id}")
    public Response deleteUser(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") int id) throws SQLException {
        return checkAdminRole(sessionId)
                ? deleteUserById(id)
                : Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
    }

    private Response createErrorResponse(String message, Response.Status status) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }

    private boolean checkAdminRole(int sessionId) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        return "admin".equals(user.getRole());
    }

    private Response deleteUserById(int id) throws SQLException {
        userRepository.deleteUser(id);
        return Response.ok().build();
    }
}
