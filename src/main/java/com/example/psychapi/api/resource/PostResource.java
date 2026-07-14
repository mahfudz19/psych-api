package com.example.psychapi.api.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @GET
    public List<String> getAllPosts() {
        return List.of("Post 1", "Post 2", "Post 3");
    }

    @GET
    @Path("/{id}")
    public String getPostById(@PathParam("id") String id) {
        return "Post with id: " + id;
    }
    
    @POST
    public Response createPost(String title) {
        return Response.status(Response.Status.CREATED).entity("Created: " + title).build();
    }

    @PUT
    @Path("/{id}")
    public String updatePost(@PathParam("id") String id, String title) {
        return "Updated post " + id + " with title: " + title;
    }

    @DELETE
    @Path("/{id}")
    public Response deletePost(@PathParam("id") String id) {
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
