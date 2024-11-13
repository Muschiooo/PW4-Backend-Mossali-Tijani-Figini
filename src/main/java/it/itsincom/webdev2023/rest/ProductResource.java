package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.rest.model.CreateUserResponse;
import it.itsincom.webdev2023.service.AuthenticationService;
import it.itsincom.webdev2023.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;


@Path("/api/product")
public class ProductResource {
    @Inject
    ProductService productService;

    @Inject
    AuthenticationService authenticationService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(@CookieParam("SESSION_COOKIE") int sessionId, Product product) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            Product createdProduct = productService.createProduct(product);
            return Response.status(Response.Status.CREATED).entity(createdProduct).build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllProducts() {
        return Response.ok(productService.getAllProducts()).build();
    }

    @GET
    @Path("/export")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response getAllProductsToExcel(@CookieParam("SESSION_COOKIE") int sessionId) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (!user.getRole().equals("admin")) {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        } else {
            try {
                ByteArrayOutputStream excel = productService.getExcel();
                byte[] excelBytes = excel.toByteArray();
                return Response.ok(excelBytes)
                        .header("Content-Disposition", "attachment; filename=products.xlsx")
                        .build();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error exporting products to Excel").build();
            }
        }
    }



    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductById(@PathParam("id") int id) {
        Product product = productService.findProductById(id);

        if (product != null) {
            return Response.ok(product).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductByName(@PathParam("name") String name) {
        Product product = productService.findProductByName(name);

        if (product != null) {
            return Response.ok(product).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response updateStock(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") int id, Product updatedProduct) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            Product product = productService.findProductById(id);

            if (product == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            product.setStock(updatedProduct.getStock());
            productService.updateProductStock(product);

            return Response.ok().build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response updateProduct(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("name") String name, Product updatedProduct) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            Product product = productService.findProductByName(name);
            if (product == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            updatedProduct.setId(product.getId());
            productService.updateProduct(updatedProduct);
            return Response.ok(updatedProduct).build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@CookieParam("SESSION_COOKIE") int sessionId, @PathParam("id") int id) throws SQLException {
        CreateUserResponse user = authenticationService.getProfile(sessionId);
        if (user.getRole().equals("admin")) {
            productService.deleteProduct(id);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.FORBIDDEN).entity("User is not authorized to perform this action").build();
        }
    }
}
