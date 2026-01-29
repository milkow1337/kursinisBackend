package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.BasicUser;
import com.example.kursinisbackend.model.Chat;
import com.example.kursinisbackend.model.Review;
import com.example.kursinisbackend.repos.BasicUserRepository;
import com.example.kursinisbackend.repos.ChatRepo;
import com.example.kursinisbackend.repos.ReviewRepo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepo reviewRepo;

    @Autowired
    private BasicUserRepository basicUserRepository;

    @Autowired
    private ChatRepo chatRepo;

    /**
     * Get all reviews
     * GET /api/reviews
     */
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        List<Review> reviews = reviewRepo.findAll();
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review by ID
     * GET /api/reviews/{reviewId}
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(@PathVariable int reviewId) {
        try {
            Review review = reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new Exception("Review not found"));
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Create a new review/rating
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody String reviewJson) {
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reviewJson, JsonObject.class);

            String reviewText = json.get("reviewText").getAsString();
            int userId = json.get("userId").getAsInt();
            int chatId = json.get("chatId").getAsInt();

            BasicUser commentOwner = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            Review review = new Review(reviewText, commentOwner, chat);
            review.setDateCreated(LocalDate.now());

            // If rating is provided
            if (json.has("rating")) {
                int rating = json.get("rating").getAsInt();
                if (rating < 1 || rating > 5) {
                    return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
                }
                // Store rating in reviewText or add a rating field to Review model
                review.setReviewText(reviewText + " [Rating: " + rating + "/5]");
            }

            reviewRepo.save(review);
            return ResponseEntity.status(HttpStatus.CREATED).body(review);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update a review
     * PUT /api/reviews/{reviewId}
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable int reviewId,
            @RequestBody String reviewJson) {
        try {
            Review review = reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new Exception("Review not found"));

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reviewJson, JsonObject.class);

            if (json.has("reviewText")) {
                review.setReviewText(json.get("reviewText").getAsString());
            }

            reviewRepo.save(review);
            return ResponseEntity.ok(review);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete a review
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable int reviewId) {
        try {
            Review review = reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new Exception("Review not found"));

            reviewRepo.delete(review);
            return ResponseEntity.ok("Review deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get reviews by user (reviews about a user)
     * GET /api/reviews/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getReviewsByUser(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> allReviews = reviewRepo.findAll();
            List<Review> userReviews = allReviews.stream()
                    .filter(review -> review.getCommentOwner().getId() == userId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get reviews written by a user
     * GET /api/reviews/users/{userId}/written
     */
    @GetMapping("/users/{userId}/written")
    public ResponseEntity<?> getReviewsWrittenByUser(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> writtenReviews = reviewRepo.findByCommentOwner_Id(userId);
            return ResponseEntity.ok(writtenReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get reviews for a restaurant
     * GET /api/reviews/restaurants/{restaurantId}
     */
    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<?> getRestaurantReviews(@PathVariable int restaurantId) {
        try {
            // Get all chats related to orders from this restaurant
            List<Chat> allChats = chatRepo.findAll();
            List<Chat> restaurantChats = allChats.stream()
                    .filter(chat -> chat.getFoodOrder() != null &&
                            chat.getFoodOrder().getRestaurant() != null &&
                            chat.getFoodOrder().getRestaurant().getId() == restaurantId)
                    .collect(Collectors.toList());

            // Get all reviews from these chats
            List<Review> restaurantReviews = restaurantChats.stream()
                    .flatMap(chat -> chat.getMessages().stream())
                    .filter(review -> review.getReviewText().contains("Rating:"))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(restaurantReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get reviews for a driver
     * GET /api/reviews/drivers/{driverId}
     */
    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<?> getDriverReviews(@PathVariable int driverId) {
        try {
            // Get all chats related to orders delivered by this driver
            List<Chat> allChats = chatRepo.findAll();
            List<Chat> driverChats = allChats.stream()
                    .filter(chat -> chat.getFoodOrder() != null &&
                            chat.getFoodOrder().getDriver() != null &&
                            chat.getFoodOrder().getDriver().getId() == driverId)
                    .collect(Collectors.toList());

            // Get all reviews from these chats
            List<Review> driverReviews = driverChats.stream()
                    .flatMap(chat -> chat.getMessages().stream())
                    .filter(review -> review.getReviewText().contains("Rating:"))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(driverReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get average rating for a user (restaurant or driver)
     * GET /api/reviews/users/{userId}/rating
     */
    @GetMapping("/users/{userId}/rating")
    public ResponseEntity<?> getAverageRating(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> allReviews = reviewRepo.findAll();

            // Filter reviews that mention this user and have ratings
            List<Review> userRatings = allReviews.stream()
                    .filter(review -> {
                        if (review.getChat() == null || review.getChat().getFoodOrder() == null) {
                            return false;
                        }

                        int restaurantId = review.getChat().getFoodOrder().getRestaurant() != null ?
                                review.getChat().getFoodOrder().getRestaurant().getId() : -1;
                        int driverId = review.getChat().getFoodOrder().getDriver() != null ?
                                review.getChat().getFoodOrder().getDriver().getId() : -1;

                        return (restaurantId == userId || driverId == userId) &&
                                review.getReviewText().contains("Rating:");
                    })
                    .collect(Collectors.toList());

            if (userRatings.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("userId", userId);
                response.addProperty("averageRating", 0.0);
                response.addProperty("totalRatings", 0);
                return ResponseEntity.ok(response.toString());
            }

            // Extract ratings from review text (format: "... [Rating: X/5]")
            double totalRating = 0.0;
            int count = 0;

            for (Review review : userRatings) {
                String text = review.getReviewText();
                int ratingStart = text.indexOf("[Rating: ") + 9;
                int ratingEnd = text.indexOf("/5]");

                if (ratingStart > 8 && ratingEnd > ratingStart) {
                    try {
                        int rating = Integer.parseInt(text.substring(ratingStart, ratingEnd));
                        totalRating += rating;
                        count++;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            double averageRating = count > 0 ? totalRating / count : 0.0;

            JsonObject response = new JsonObject();
            response.addProperty("userId", userId);
            response.addProperty("userName", user.getName() + " " + user.getSurname());
            response.addProperty("averageRating", Math.round(averageRating * 10.0) / 10.0);
            response.addProperty("totalRatings", count);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get recent reviews (last N reviews)
     * GET /api/reviews/recent?limit={limit}
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentReviews(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Review> allReviews = reviewRepo.findAll();

            // Sort by date (most recent first) and limit
            List<Review> recentReviews = allReviews.stream()
                    .sorted((r1, r2) -> {
                        LocalDate d1 = r1.getDateCreated() != null ? r1.getDateCreated() : LocalDate.MIN;
                        LocalDate d2 = r2.getDateCreated() != null ? r2.getDateCreated() : LocalDate.MIN;
                        return d2.compareTo(d1);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(recentReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get reviews by rating
     * GET /api/reviews/rating/{rating}
     */
    @GetMapping("/rating/{rating}")
    public ResponseEntity<?> getReviewsByRating(@PathVariable int rating) {
        try {
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
            }

            List<Review> allReviews = reviewRepo.findAll();
            String ratingPattern = "[Rating: " + rating + "/5]";

            List<Review> filteredReviews = allReviews.stream()
                    .filter(review -> review.getReviewText().contains(ratingPattern))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredReviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}