package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.*;
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
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrdersRepo ordersRepo;

    /**
     * Get all reviews (admin only)
     */
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        List<Review> reviews = reviewRepo.findAll();
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review by ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getReviewById(@PathVariable int reviewId) {
        return reviewRepo.findById(reviewId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new review
     */
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody CreateReviewRequest request) {
        try {
            // Validate input
            if (request.getReviewText() == null || request.getReviewText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Review text is required");
            }
            if (request.getRating() < 1 || request.getRating() > 5) {
                return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
            }
            if (request.getCommentOwnerId() <= 0) {
                return ResponseEntity.badRequest().body("Valid comment owner ID is required");
            }
            if (request.getFeedbackUserId() <= 0) {
                return ResponseEntity.badRequest().body("Valid feedback user ID is required");
            }

            // Get comment owner (reviewer)
            BasicUser commentOwner = basicUserRepository.findById(request.getCommentOwnerId())
                    .orElseThrow(() -> new Exception("Comment owner not found"));

            // Get feedback user (person being reviewed)
            BasicUser feedbackUser = basicUserRepository.findById(request.getFeedbackUserId())
                    .orElseThrow(() -> new Exception("Feedback user not found"));

            // Validate review permissions
            // Customers and drivers can review restaurants
            // Restaurants and drivers can review customers
            // Only drivers can be reviewed by customers and restaurants
            boolean validReview = validateReviewPermissions(commentOwner, feedbackUser);
            if (!validReview) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to review this user");
            }

            // Create review
            Review review = new Review();
            review.setReviewText(request.getReviewText());
            review.setRating(request.getRating());
            review.setCommentOwner(commentOwner);
            review.setFeedbackUser(feedbackUser);
            review.setDateCreated(LocalDate.now());

            reviewRepo.save(review);

            return ResponseEntity.status(HttpStatus.CREATED).body(review);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating review: " + e.getMessage());
        }
    }

    /**
     * Validate if user can review another user
     */
    private boolean validateReviewPermissions(BasicUser reviewer, BasicUser reviewee) {
        // Customers can review restaurants and drivers
        if (!(reviewer instanceof Restaurant) && !(reviewer instanceof Driver)) {
            return (reviewee instanceof Restaurant) || (reviewee instanceof Driver);
        }

        // Restaurants can review customers and drivers
        if (reviewer instanceof Restaurant) {
            return !(reviewee instanceof Restaurant);
        }

        // Drivers can review customers and restaurants
        if (reviewer instanceof Driver) {
            return true;
        }

        return false;
    }

    /**
     * Get reviews for a specific user (reviews about them)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getReviewsForUser(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> reviews = reviewRepo.findAll().stream()
                    .filter(r -> r.getFeedbackUser() != null && r.getFeedbackUser().getId() == userId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting reviews: " + e.getMessage());
        }
    }

    /**
     * Get reviews written by a user
     */
    @GetMapping("/users/{userId}/written")
    public ResponseEntity<?> getReviewsWrittenByUser(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> reviews = reviewRepo.findAll().stream()
                    .filter(r -> r.getCommentOwner() != null && r.getCommentOwner().getId() == userId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting reviews: " + e.getMessage());
        }
    }

    /**
     * Get reviews for a restaurant
     */
    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<?> getRestaurantReviews(@PathVariable int restaurantId) {
        try {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            List<Review> reviews = reviewRepo.findAll().stream()
                    .filter(r -> r.getFeedbackUser() != null && r.getFeedbackUser().getId() == restaurantId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting reviews: " + e.getMessage());
        }
    }

    /**
     * Get reviews for a driver
     */
    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<?> getDriverReviews(@PathVariable int driverId) {
        try {
            // Verify user is a driver
            BasicUser user = basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("User not found"));

            if (!(user instanceof Driver)) {
                return ResponseEntity.badRequest().body("User is not a driver");
            }

            List<Review> reviews = reviewRepo.findAll().stream()
                    .filter(r -> r.getFeedbackUser() != null && r.getFeedbackUser().getId() == driverId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting reviews: " + e.getMessage());
        }
    }

    /**
     * Get average rating for a user
     */
    @GetMapping("/users/{userId}/rating")
    public ResponseEntity<?> getUserAverageRating(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<Review> reviews = reviewRepo.findAll().stream()
                    .filter(r -> r.getFeedbackUser() != null && r.getFeedbackUser().getId() == userId)
                    .filter(r -> r.getRating() > 0)
                    .collect(Collectors.toList());

            if (reviews.isEmpty()) {
                return ResponseEntity.ok(new RatingResponse(0.0, 0));
            }

            double averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            return ResponseEntity.ok(new RatingResponse(averageRating, reviews.size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error calculating rating: " + e.getMessage());
        }
    }

    /**
     * Update review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable int reviewId,
            @RequestBody UpdateReviewRequest request) {
        try {
            Review review = reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new Exception("Review not found"));

            // Update fields if provided
            if (request.getReviewText() != null && !request.getReviewText().trim().isEmpty()) {
                review.setReviewText(request.getReviewText());
            }

            if (request.getRating() != null) {
                if (request.getRating() < 1 || request.getRating() > 5) {
                    return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
                }
                review.setRating(request.getRating());
            }

            reviewRepo.save(review);
            return ResponseEntity.ok(review);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating review: " + e.getMessage());
        }
    }

    /**
     * Delete review
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable int reviewId) {
        try {
            Review review = reviewRepo.findById(reviewId)
                    .orElseThrow(() -> new Exception("Review not found"));

            // Check if review is a chat message
            if (review.getChat() != null) {
                return ResponseEntity.badRequest()
                        .body("Cannot delete chat messages through this endpoint. Use chat API instead.");
            }

            reviewRepo.delete(review);
            return ResponseEntity.ok("Review deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting review: " + e.getMessage());
        }
    }

    // Request/Response DTOs
    public static class CreateReviewRequest {
        private String reviewText;
        private int rating;
        private int commentOwnerId;
        private int feedbackUserId;

        public String getReviewText() {
            return reviewText;
        }

        public void setReviewText(String reviewText) {
            this.reviewText = reviewText;
        }

        public int getRating() {
            return rating;
        }

        public void setRating(int rating) {
            this.rating = rating;
        }

        public int getCommentOwnerId() {
            return commentOwnerId;
        }

        public void setCommentOwnerId(int commentOwnerId) {
            this.commentOwnerId = commentOwnerId;
        }

        public int getFeedbackUserId() {
            return feedbackUserId;
        }

        public void setFeedbackUserId(int feedbackUserId) {
            this.feedbackUserId = feedbackUserId;
        }
    }

    public static class UpdateReviewRequest {
        private String reviewText;
        private Integer rating;

        public String getReviewText() {
            return reviewText;
        }

        public void setReviewText(String reviewText) {
            this.reviewText = reviewText;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }
    }

    public static class RatingResponse {
        private double averageRating;
        private int totalReviews;

        public RatingResponse(double averageRating, int totalReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(double averageRating) {
            this.averageRating = averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public void setTotalReviews(int totalReviews) {
            this.totalReviews = totalReviews;
        }
    }
}