package it.itsincom.webdev2023.rest;

import it.itsincom.webdev2023.persistence.model.Product;
import it.itsincom.webdev2023.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


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
    @Path("/{name}")  // Percorso con parametro dinamico
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductByName(@PathParam("name") String name) {  // Usa @PathParam con il nome corretto
        Product product = productService.findProductByName(name);

        if (product != null) {
            return Response.ok(product).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
