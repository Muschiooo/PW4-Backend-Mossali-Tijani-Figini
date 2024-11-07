package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;


@Path("/api/product")
public class ProductResource {
    @Inject
    ProductService productService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(Product product) {
        Product createdProduct = productService.createProduct(product);
        return Response.status(Response.Status.CREATED).entity(createdProduct).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllProducts() {
        return Response.ok(productService.getAllProducts()).build();
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
    public Response updateStock(@PathParam("id") int id, Product updatedProduct) throws SQLException {
        Product product = productService.findProductById(id);

        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        product.setStock(updatedProduct.getStock());
        productService.updateProductStock(product);

        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Response updateProduct(@PathParam("name") String name, Product updatedProduct) {
        Product product = productService.findProductByName(name);
        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        updatedProduct.setId(product.getId());
        productService.updateProduct(updatedProduct);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") int id) {
        productService.deleteProduct(id);
        return Response.ok().build();
    }
}
