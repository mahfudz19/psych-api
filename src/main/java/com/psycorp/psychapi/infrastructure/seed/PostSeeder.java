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

        // ✅ Gunakan factory method + setters
        Post post1 = new Post();
        post1.setTitle("Post 1");
        post1.setContent("Content 1");
        post1.setStatus("published");
        post1.setCreatedAt(Instant.now());
        post1.setUpdatedAt(Instant.now());

        Post post2 = new Post();
        post2.setTitle("Post 2");
        post2.setContent("Content 2");
        post2.setStatus("draft");
        post2.setCreatedAt(Instant.now());
        post2.setUpdatedAt(Instant.now());

        Post.persist(post1, post2);
        System.out.println("Posts seeded successfully!");
    }
}
