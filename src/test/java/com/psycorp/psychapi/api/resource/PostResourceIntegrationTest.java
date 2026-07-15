package com.psycorp.psychapi.api.resource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.psycorp.psychapi.domain.model.Post;

import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;

@QuarkusTest
@SuppressWarnings("unused")
public class PostResourceIntegrationTest {

    @BeforeEach
    void setUp() {
        // Clean database sebelum setiap test untuk memastikan isolasi
        Post.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean database setelah setiap test
        Post.deleteAll();
    }

    // ===========================================
    // CREATE POST TESTS
    // ===========================================
    @Nested
    @DisplayName("Create Post API Tests")
    class CreatePostTests {

        @Test
        @DisplayName("Should create post with valid request (default status: draft)")
        void testCreatePost_validRequest_shouldReturn201() {
            String requestBody = """
            {
                "title": "Test Post Title",
                "content": "Test post content here"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("message", equalTo("Post created successfully"))
                .body("data.id", notNullValue())
                .body("data.title", equalTo("Test Post Title"))
                .body("data.content", equalTo("Test post content here"))
                .body("data.status", equalTo("draft"))
                .body("data.createdAt", notNullValue())
                .body("data.updatedAt", notNullValue());
        }

        @Test
        @DisplayName("Should create post with explicit published status")
        void testCreatePost_withPublishedStatus_shouldReturn201() {
            String requestBody = """
            {
                "title": "Published Post",
                "content": "This is published",
                "status": "published"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("published"));
        }

        @Test
        @DisplayName("Should return 400 when title is empty (Bean Validation)")
        void testCreatePost_emptyTitle_shouldReturn400() {
            String requestBody = """
            {
                "title": "",
                "content": "Content here"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors.field", hasItem("createPost.request.title"))
                .body("errors.message", hasItem(containsString("must not be blank")));
        }

        @Test
        @DisplayName("Should return 400 when title is less than 3 characters (Custom Validation)")
        void testCreatePost_shortTitle_shouldReturn400() {
            String requestBody = """
            {
                "title": "AB",
                "content": "Content here"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("message", containsString("Title must be at least 3 characters"));
        }

        @Test
        @DisplayName("Should return 400 when title exceeds 200 characters (Custom Validation)")
        void testCreatePost_longTitle_shouldReturn400() {
            String longTitle = "A".repeat(201);
            String requestBody = """
            {
                "title": "%s",
                "content": "Content here"
            }
            """.formatted(longTitle);
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("message", containsString("Title must not exceed 200 characters"));
        }

        @Test
        @DisplayName("Should return 400 when content exceeds 10000 characters (Custom Validation)")
        void testCreatePost_longContent_shouldReturn400() {
            String longContent = "A".repeat(10001);
            String requestBody = """
            {
                "title": "Test Post",
                "content": "%s"
            }
            """.formatted(longContent);
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("message", containsString("Content must not exceed 10000 characters"));
        }

        @Test
        @DisplayName("Should return 400 when status is invalid (Bean Validation)")
        void testCreatePost_invalidStatus_shouldReturn400() {
            String requestBody = """
            {
                "title": "Test Post",
                "content": "Content",
                "status": "invalid_status"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should return 400 when title is null (Bean Validation)")
        void testCreatePost_nullTitle_shouldReturn400() {
            String requestBody = """
            {
                "content": "Content here"
            }
            """;
            
            given()
                .contentType("application/json")
                .body(requestBody)
            .when()
                .post("/posts")
            .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors.field", hasItem("createPost.request.title"))
                .body("errors.message", hasItem(containsString("must not be blank")));
        }
    }

    // ===========================================
    // GET ALL POSTS TESTS
    // ===========================================
    @Nested
    @DisplayName("Get All Posts API Tests")
    class GetAllPostsTests {

        @Test
        @DisplayName("Should return empty list when database is empty")
        void testGetAllPosts_emptyDatabase_shouldReturnEmptyList() {
            given()
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", empty())
                .body("meta.total", equalTo(0))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10))
                .body("meta.totalPages", equalTo(0));
        }

        @Test
        @DisplayName("Should return list of posts with default pagination")
        void testGetAllPosts_withData_shouldReturnPostList() {
            createTestPost("Post 1", "Content 1", "draft");
            createTestPost("Post 2", "Content 2", "published");
            createTestPost("Post 3", "Content 3", "draft");
            
            given()
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(3))
                .body("meta.total", equalTo(3))
                .body("meta.page", equalTo(1))
                .body("meta.limit", equalTo(10));
        }

        @Test
        @DisplayName("Should return correct page with custom pagination")
        void testGetAllPosts_withPagination_shouldReturnCorrectPage() {
            for (int i = 1; i <= 15; i++) {
                createTestPost("Post " + i, "Content " + i, "draft");
            }
            
            given()
                .queryParam("page", 2)
                .queryParam("limit", 10)
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", hasSize(5))  // Page 2 with 15 items total = 5 items on page 2
                .body("meta.page", equalTo(2))
                .body("meta.limit", equalTo(10))
                .body("meta.total", equalTo(15))
                .body("meta.totalPages", equalTo(2));
        }

        @Test
        @DisplayName("Should filter posts by search keyword in title and content")
        void testGetAllPosts_withSearch_shouldReturnFilteredResults() {
            createTestPost("Psychology Basics", "About psychology", "published");
            createTestPost("Mental Health", "Mental health content", "published");
            createTestPost("Psychology Advanced", "More psychology", "draft");
            
            given()
                .queryParam("search", "psychology")
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("meta.total", equalTo(2));
        }

        @Test
        @DisplayName("Should filter posts by status using IN operator")
        void testGetAllPosts_withStatusFilter_shouldReturnFilteredResults() {
            createTestPost("Draft Post", "Content", "draft");
            createTestPost("Published Post", "Content", "published");
            createTestPost("Another Draft", "Content", "draft");
            
            given()
                .queryParam("filter", "status:in:draft")
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", hasSize(2))
                .body("meta.total", equalTo(2));
        }

        @Test
        @DisplayName("Should filter posts by status with multiple values")
        void testGetAllPosts_withMultipleStatusFilter_shouldReturnFilteredResults() {
            createTestPost("Draft Post", "Content", "draft");
            createTestPost("Published Post", "Content", "published");
            createTestPost("Another Draft", "Content", "draft");
            
            given()
                .queryParam("filter", "status:in:draft,published")
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", hasSize(3))
                .body("meta.total", equalTo(3));
        }

        @Test
        @DisplayName("Should sort posts by field and order")
        void testGetAllPosts_withSort_shouldReturnSortedResults() {
            createTestPost("Post C", "Content", "draft");
            createTestPost("Post A", "Content", "draft");
            createTestPost("Post B", "Content", "draft");
            
            given()
                .queryParam("sortBy", "title")
                .queryParam("sortOrder", "asc")
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data[0].title", equalTo("Post A"))
                .body("data[1].title", equalTo("Post B"))
                .body("data[2].title", equalTo("Post C"));
        }

        @Test
        @DisplayName("Should return empty result when page is out of range")
        void testGetAllPosts_pageOutOfRange_shouldReturnEmptyList() {
            createTestPost("Post 1", "Content 1", "draft");
            
            given()
                .queryParam("page", 100)
                .queryParam("limit", 10)
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", empty())
                .body("meta.total", equalTo(1));
        }

        @Test
        @DisplayName("Should return empty result when search keyword not found")
        void testGetAllPosts_searchNotFound_shouldReturnEmptyList() {
            createTestPost("Post 1", "Content 1", "draft");
            createTestPost("Post 2", "Content 2", "draft");
            
            given()
                .queryParam("search", "nonexistent_keyword")
            .when()
                .get("/posts")
            .then()
                .statusCode(200)
                .body("data", empty())
                .body("meta.total", equalTo(0));
        }
    }

    // ===========================================
    // GET POST BY ID TESTS
    // ===========================================
    @Nested
    @DisplayName("Get Post By ID API Tests")
    class GetPostByIdTests {

        @Test
        @DisplayName("Should return post when ID is valid")
        void testGetPostById_validId_shouldReturnPost() {
            String postId = createTestPost("Test Post", "Content", "draft");
            
            given()
            .when()
                .get("/posts/" + postId)
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(postId))
                .body("data.title", equalTo("Test Post"))
                .body("data.content", equalTo("Content"))
                .body("data.status", equalTo("draft"));
        }

        @Test
        @DisplayName("Should return 404 when post not found")
        void testGetPostById_nonExistentId_shouldReturn404() {
            String fakeId = "507f1f77bcf86cd799439011";
            
            given()
            .when()
                .get("/posts/" + fakeId)
            .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("code", equalTo("POST_NOT_FOUND"))
                .body("message", containsString("not found"));
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void testGetPostById_invalidIdFormat_shouldReturn400() {
            given()
            .when()
                .get("/posts/invalid-id")
            .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_ID"))
                .body("message", containsString("Invalid id format"));
        }

        @Test
        @DisplayName("Should return 200 with empty list when ID path is empty")
        void testGetPostById_emptyId_shouldReturnEmptyList() {
            // GET /posts/ tanpa ID akan dianggap sebagai GET /posts
            given()
            .when()
                .get("/posts/")
            .then()
                .statusCode(200)
                .body("success", equalTo(true));
        }
    }

    // ===========================================
    // UPDATE POST TESTS
    // ===========================================
    @Nested
    @DisplayName("Update Post API Tests")
    class UpdatePostTests {

        @Test
        @DisplayName("Should update post with valid request")
        void testUpdatePost_validRequest_shouldReturnUpdatedPost() {
            String postId = createTestPost("Original Title", "Original Content", "draft");
            
            String updateBody = """
            {
                "title": "Updated Title",
                "content": "Updated Content",
                "status": "published"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", postId)
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(postId))
                .body("data.title", equalTo("Updated Title"))
                .body("data.content", equalTo("Updated Content"))
                .body("data.status", equalTo("published"));
        }

        @Test
        @DisplayName("Should update only provided fields (partial update)")
        void testUpdatePost_partialUpdate_shouldUpdateOnlyProvidedFields() {
            String postId = createTestPost("Original Title", "Original Content", "draft");
            
            String updateBody = """
            {
                "title": "New Title Only"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", postId)
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(200)
                .body("data.title", equalTo("New Title Only"))
                .body("data.content", equalTo("Original Content"))
                .body("data.status", equalTo("draft"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent post")
        void testUpdatePost_nonExistentId_shouldReturn404() {
            String fakeId = "507f1f77bcf86cd799439011";
            
            String updateBody = """
            {
                "title": "Updated Title"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", fakeId)
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(404)
                .body("code", equalTo("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid title (Custom Validation)")
        void testUpdatePost_invalidTitle_shouldReturn400() {
            String postId = createTestPost("Original Title", "Content", "draft");
            
            String updateBody = """
            {
                "title": "AB"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", postId)
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(400)
                .body("message", containsString("Title must be at least 3 characters"));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid status (Bean Validation)")
        void testUpdatePost_invalidStatus_shouldReturn400() {
            String postId = createTestPost("Original Title", "Content", "draft");
            
            String updateBody = """
            {
                "title": "Updated Title",
                "status": "invalid_status"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", postId)
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors.field", hasItem("updatePost.request.status"))
                .body("errors.message", hasItem(containsString("must match")));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid ID format")
        void testUpdatePost_invalidIdFormat_shouldReturn400() {
            String updateBody = """
            {
                "title": "Updated Title"
            }
            """;
            
            given()
                .contentType("application/json")
                .pathParam("id", "invalid-id")
                .body(updateBody)
            .when()
                .put("/posts/{id}")
            .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_ID"))
                .body("message", containsString("Invalid id format"));
        }
    }

    // ===========================================
    // DELETE POST TESTS
    // ===========================================
    @Nested
    @DisplayName("Delete Post API Tests")
    class DeletePostTests {

        @Test
        @DisplayName("Should delete post and return success")
        void testDeletePost_validId_shouldReturnSuccess() {
            String postId = createTestPost("To Delete", "Content", "draft");
            
            given()
                .pathParam("id", postId)
            .when()
                .delete("/posts/{id}")
            .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Post deleted successfully"))
                .body("data", nullValue());
            
            // Verify post is deleted
            given()
            .when()
                .get("/posts/" + postId)
            .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent post")
        void testDeletePost_nonExistentId_shouldReturn404() {
            String fakeId = "507f1f77bcf86cd799439011";
            
            given()
                .pathParam("id", fakeId)
            .when()
                .delete("/posts/{id}")
            .then()
                .statusCode(404)
                .body("code", equalTo("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 when deleting with invalid ID format")
        void testDeletePost_invalidIdFormat_shouldReturn400() {
            given()
                .pathParam("id", "invalid-id")
            .when()
                .delete("/posts/{id}")
            .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_ID"))
                .body("message", containsString("Invalid id format"));
        }
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================
    
    /**
     * Helper method untuk membuat test post
     * @param title judul post
     * @param content konten post
     * @param status status post (draft/published)
     * @return ID dari post yang dibuat
     */
    private String createTestPost(String title, String content, String status) {
        String requestBody = """
        {
            "title": "%s",
            "content": "%s",
            "status": "%s"
        }
        """.formatted(title, content, status);
        
        Response response = given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/posts");
        
        return response.jsonPath().getString("data.id");
    }
}
