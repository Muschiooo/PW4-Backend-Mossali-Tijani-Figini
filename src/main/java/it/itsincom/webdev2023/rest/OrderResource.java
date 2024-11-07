package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.OrderMongo;
import it.itsincom.webdev2023.service.OrderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/order")
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderMongo order) {
        boolean success = orderService.createOrder(order);
        if (success) {
            return Response.ok("Order created successfully.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error creating order.").build();

        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrder(@PathParam("id") String id, OrderMongo order) {
        boolean success = orderService.updateOrder(id, order);
        if (success) {
            return Response.ok("Order updated successfully.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
        }
    }

    @PUT
    @Path("/accept/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response acceptOrder(@PathParam("id") String id) {
        boolean success = orderService.acceptOrder(id);
        if (success) {
            return Response.ok("Order updated successfully.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
        }
    }

    @PUT
    @Path("/deliver/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deliverOrder(@PathParam("id") String id) {
        boolean success = orderService.deliverOrder(id);
        if (success) {
            return Response.ok("Order updated successfully.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error updating order.").build();
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
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderById(@PathParam("id") String id) {
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
}
