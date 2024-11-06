package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.rest.model.CreateUserRequest;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.rest.model.LoginRequest;
import it.itsincom.webdev2023.service.AuthenticationService;
import it.itsincom.webdev2023.service.UserService;
import it.itsincom.webdev2023.service.exceptions.NotVerifiedException;
import it.itsincom.webdev2023.service.exceptions.SessionCreatedException;
import it.itsincom.webdev2023.service.exceptions.WrongCredentialException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
public class AuthenticationResource {
    @Inject
    AuthenticationService authenticationService;
    @Inject
    UserService userService;

    @POST
    @Path("/register")
    public CreateUserResponse register(CreateUserRequest user) {
        return authenticationService.register(user);
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyToken(Map<String, String> requestBody) {
        String token = requestBody.get("token");
        boolean isVerified = userService.checkToken(token);
        if (isVerified) {
            return Response.ok("User verified successfully").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid or expired token").build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest loginRequest) throws NotVerifiedException, WrongCredentialException, SessionCreatedException {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        int session = authenticationService.login(email, password);
        NewCookie sessionCookie = new NewCookie.Builder("SESSION_COOKIE").path("/").value(String.valueOf(session)).build();
        return Response.ok()
                .cookie(sessionCookie)
                .build();
    }

    @DELETE
    @Path("/logout")
    public Response logout(@CookieParam("SESSION_COOKIE") int sessionId) {
        authenticationService.logout(sessionId);
        NewCookie sessionCookie = new NewCookie.Builder("SESSION_COOKIE").path("/").build();
        return Response.ok()
                .cookie(sessionCookie)
                .build();
    }
}


