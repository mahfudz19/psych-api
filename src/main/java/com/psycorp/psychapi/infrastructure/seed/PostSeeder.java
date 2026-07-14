package com.psycorp.psychapi.infrastructure.seed;

import java.time.Instant;

import com.psycorp.psychapi.domain.model.Post;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class PostSeeder {

    public void init(@Observes StartupEvent event) {
        long count = Post.count();
        if (count > 0) {
            return; 
        }

        Post post1 = new Post();
        post1.title = "Post 1";
        post1.content = "Content 1";
        post1.status = "published";
        post1.createdAt = Instant.now();
        post1.updatedAt = Instant.now();

        Post post2 = new Post();
        post2.title = "Post 2";
        post2.content = "Content 2";
        post2.status = "draft";
        post2.createdAt = Instant.now();
        post2.updatedAt = Instant.now();

        Post.persist(post1, post2);
        System.out.println("Posts seeded successfully!");
    }
}
