package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.persistence.repository.OrderMongoRepository;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.service.AuthenticationService;
import it.itsincom.webdev2023.service.OrderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

@Path("/api/order")
public class OrderResource {
    @Inject
    OrderService orderService;
    @Inject
    AuthenticationService authenticationService;
    @Inject
    OrderMongoRepository orderMongoRepository;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderMongo order) {
        // Create a Calendar instance and set the delivery date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(order.getDeliverDate());

        // Subtract one hour from the delivery date
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        order.setDeliverDate(calendar.getTime());

        Response validationResponse = orderMongoRepository.validateDeliveryDate(order);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            return orderMongoRepository.createNewOrder(order);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore interno durante la creazione dell'ordine.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrder(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") String id, OrderMongo order) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            boolean success = orderService.updateOrder(id, order);
            if (success) {
                return Response.ok("Order updated successfully.").build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
            }
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @PUT
    @Path("/accept/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptOrder(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") String id) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            boolean success = orderService.acceptOrder(id);
            if (success) {
                return Response.ok("Order updated successfully.").build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
            }
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @PUT
    @Path("/deliver/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deliverOrder(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") String id) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            boolean success = orderService.deliverOrder(id);
            if (success) {
                return Response.ok("Order updated successfully.").build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
            }
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteOrder(@PathParam("id") String id) {
        boolean success = orderService.deleteOrder(id);
        if (success) {
            return Response.ok("Order deleted successfully.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error deleting order.").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrders(@CookieParam("SESSION_COOKIE") int sessionId) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            List<OrderMongo> orders = orderService.getAllOrders();
            return Response.ok(orders).build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderById(@PathParam("id") ObjectId id) {
        OrderMongo order = orderService.getOrderById(id);

        if (order != null) {
            return Response.ok(order).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/user/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrdersByUser(@PathParam("email") String email) {
        List<OrderMongo> orders = orderService.getOrdersByUser(email);
        return Response.ok(orders).build();
    }

    @GET
    @Path("/date/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrdersByDate(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("date") String date) throws SQLException {

        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            List<OrderMongo> orders = orderService.getOrdersByDate(date);
            return Response.ok(orders).build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @GET
    @Path("/date/{date}/export")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportOrdersByDateToExcel(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("date") String date) throws SQLException, IOException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (!user.getRole().equals("admin")) {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }

        ByteArrayOutputStream outputStream = orderService.getExcel(date);

        return Response.ok(outputStream.toByteArray())
                .header("Content-Disposition", "attachment; filename=orders_" + date + ".xlsx")
                .build();
    }
}
