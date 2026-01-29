package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.CuisineRepo;
import com.example.kursinisbackend.repos.OrdersRepo;
import com.example.kursinisbackend.repos.RestaurantRepository;
import com.example.kursinisbackend.service.OrderService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final OrdersRepo ordersRepo;
    private final CuisineRepo cuisineRepo;
    private final OrderService orderService;

    /**
     * Get restaurant by ID
     * GET /api/restaurants/{restaurantId}
     */
    @GetMapping("/{restaurantId}")
    public ResponseEntity<?> getRestaurantById(@PathVariable int restaurantId) {
        try {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));
            return ResponseEntity.ok(restaurant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all orders for a restaurant
     * GET /api/restaurants/{restaurantId}/orders
     */
    @GetMapping("/{restaurantId}/orders")
    public ResponseEntity<?> getRestaurantOrders(@PathVariable int restaurantId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            List<FoodOrder> orders = ordersRepo.findByRestaurant_Id(restaurantId);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get restaurant orders by status
     * GET /api/restaurants/{restaurantId}/orders/status/{status}
     */
    @GetMapping("/{restaurantId}/orders/status/{status}")
    public ResponseEntity<?> getRestaurantOrdersByStatus(
            @PathVariable int restaurantId,
            @PathVariable String status) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            OrderStatus orderStatus = OrderStatus.valueOf(status);
            List<FoodOrder> allOrders = ordersRepo.findByRestaurant_Id(restaurantId);
            List<FoodOrder> filteredOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == orderStatus)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredOrders);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid order status: " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update order status (generic)
     * PUT /api/restaurants/{restaurantId}/orders/{orderId}/status
     */
    @PutMapping("/{restaurantId}/orders/{orderId}/status")
    public ResponseEntity<?> updateRestaurantOrderStatus(
            @PathVariable int restaurantId,
            @PathVariable int orderId,
            @RequestBody String statusJson) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify order belongs to this restaurant
            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.badRequest()
                        .body("Order does not belong to this restaurant");
            }

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(statusJson, JsonObject.class);
            String statusStr = json.get("status").getAsString();
            OrderStatus newStatus = OrderStatus.valueOf(statusStr);

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid order status");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Accept an order
     * POST /api/restaurants/{restaurantId}/orders/{orderId}/accept
     */
    @PostMapping("/{restaurantId}/orders/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(
            @PathVariable int restaurantId,
            @PathVariable int orderId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify order belongs to this restaurant
            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.badRequest()
                        .body("Order does not belong to this restaurant");
            }

            // Check if order is in PLACED status
            if (order.getOrderStatus() != OrderStatus.PLACED) {
                return ResponseEntity.badRequest()
                        .body("Can only accept orders in PLACED status");
            }

            // Update status to ACCEPTED
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.ACCEPTED);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Mark order as ready for pickup
     * POST /api/restaurants/{restaurantId}/orders/{orderId}/ready
     */
    @PostMapping("/{restaurantId}/orders/{orderId}/ready")
    public ResponseEntity<?> markOrderReady(
            @PathVariable int restaurantId,
            @PathVariable int orderId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify order belongs to this restaurant
            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.badRequest()
                        .body("Order does not belong to this restaurant");
            }

            // Check if order is in ACCEPTED status
            if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
                return ResponseEntity.badRequest()
                        .body("Order must be accepted before marking as ready");
            }

            // Update status to READY
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.READY);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get restaurant's pending orders (placed or accepted)
     * GET /api/restaurants/{restaurantId}/orders/pending
     */
    @GetMapping("/{restaurantId}/orders/pending")
    public ResponseEntity<?> getRestaurantPendingOrders(@PathVariable int restaurantId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            List<FoodOrder> allOrders = ordersRepo.findByRestaurant_Id(restaurantId);
            List<FoodOrder> pendingOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.PLACED ||
                            order.getOrderStatus() == OrderStatus.ACCEPTED)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(pendingOrders);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Add menu item to restaurant
     * POST /api/restaurants/{restaurantId}/menu
     */
    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<?> addMenuItem(
            @PathVariable int restaurantId,
            @RequestBody Cuisine cuisine) {
        try {
            // Verify restaurant exists
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Set restaurant for cuisine
            cuisine.setRestaurant(restaurant);

            // Validate cuisine
            if (cuisine.getName() == null || cuisine.getName().isEmpty()) {
                return ResponseEntity.badRequest().body("Cuisine name is required");
            }

            if (cuisine.getPrice() == null || cuisine.getPrice() <= 0) {
                return ResponseEntity.badRequest().body("Valid price is required");
            }

            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update menu item
     * PUT /api/restaurants/{restaurantId}/menu/{cuisineId}
     */
    @PutMapping("/{restaurantId}/menu/{cuisineId}")
    public ResponseEntity<?> updateMenuItem(
            @PathVariable int restaurantId,
            @PathVariable int cuisineId,
            @RequestBody Cuisine updatedCuisine) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Get the cuisine item
            Cuisine cuisine = cuisineRepo.findById(cuisineId)
                    .orElseThrow(() -> new Exception("Menu item not found"));

            // Verify cuisine belongs to this restaurant
            if (cuisine.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.badRequest()
                        .body("Menu item does not belong to this restaurant");
            }

            // Update fields
            if (updatedCuisine.getName() != null && !updatedCuisine.getName().isEmpty()) {
                cuisine.setName(updatedCuisine.getName());
            }

            if (updatedCuisine.getIngredients() != null) {
                cuisine.setIngredients(updatedCuisine.getIngredients());
            }

            if (updatedCuisine.getPrice() != null && updatedCuisine.getPrice() > 0) {
                cuisine.setPrice(updatedCuisine.getPrice());
            }

            cuisine.setSpicy(updatedCuisine.isSpicy());
            cuisine.setVegan(updatedCuisine.isVegan());

            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.ok(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete menu item
     * DELETE /api/restaurants/{restaurantId}/menu/{cuisineId}
     */
    @DeleteMapping("/{restaurantId}/menu/{cuisineId}")
    public ResponseEntity<?> deleteMenuItem(
            @PathVariable int restaurantId,
            @PathVariable int cuisineId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            // Get the cuisine item
            Cuisine cuisine = cuisineRepo.findById(cuisineId)
                    .orElseThrow(() -> new Exception("Menu item not found"));

            // Verify cuisine belongs to this restaurant
            if (cuisine.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.badRequest()
                        .body("Menu item does not belong to this restaurant");
            }

            cuisineRepo.delete(cuisine);
            return ResponseEntity.ok("Menu item deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get restaurant statistics
     * GET /api/restaurants/{restaurantId}/stats
     */
    @GetMapping("/{restaurantId}/stats")
    public ResponseEntity<?> getRestaurantStats(@PathVariable int restaurantId) {
        try {
            // Verify restaurant exists
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            List<FoodOrder> allOrders = ordersRepo.findByRestaurant_Id(restaurantId);
            List<Cuisine> menuItems = cuisineRepo.getCuisineByRestaurantId(restaurantId);

            JsonObject stats = new JsonObject();
            stats.addProperty("restaurantId", restaurantId);
            stats.addProperty("restaurantName", restaurant.getRestaurantName());
            stats.addProperty("address", restaurant.getAddress());

            // Opening hours
            if (restaurant.getOpeningTime() != null) {
                stats.addProperty("openingTime", restaurant.getOpeningTime().toString());
            }
            if (restaurant.getClosingTime() != null) {
                stats.addProperty("closingTime", restaurant.getClosingTime().toString());
            }

            // Menu stats
            stats.addProperty("totalMenuItems", menuItems.size());

            // Order stats
            stats.addProperty("totalOrders", allOrders.size());

            long completedOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .count();
            stats.addProperty("completedOrders", completedOrders);

            long pendingOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.PLACED)
                    .count();
            stats.addProperty("pendingOrders", pendingOrders);

            long activeOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() != OrderStatus.COMPLETED &&
                            order.getOrderStatus() != OrderStatus.CANCELLED)
                    .count();
            stats.addProperty("activeOrders", activeOrders);

            // Revenue
            double totalRevenue = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() : 0.0)
                    .sum();
            stats.addProperty("totalRevenue", totalRevenue);

            // Today's stats
            LocalDate today = LocalDate.now();
            long todayOrders = allOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().equals(today))
                    .count();
            stats.addProperty("todayOrders", todayOrders);

            double todayRevenue = allOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().equals(today) &&
                            order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() : 0.0)
                    .sum();
            stats.addProperty("todayRevenue", todayRevenue);

            // Average rating (placeholder)
            stats.addProperty("averageRating", 4.3);

            return ResponseEntity.ok(stats.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update restaurant operating hours
     * PUT /api/restaurants/{restaurantId}/hours
     */
    @PutMapping("/{restaurantId}/hours")
    public ResponseEntity<?> updateOperatingHours(
            @PathVariable int restaurantId,
            @RequestBody String hoursJson) {
        try {
            // Verify restaurant exists
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            Gson gson = new Gson();
            JsonObject hours = gson.fromJson(hoursJson, JsonObject.class);

            if (hours.has("openingTime")) {
                LocalTime openingTime = LocalTime.parse(hours.get("openingTime").getAsString());
                restaurant.setOpeningTime(openingTime);
            }

            if (hours.has("closingTime")) {
                LocalTime closingTime = LocalTime.parse(hours.get("closingTime").getAsString());
                restaurant.setClosingTime(closingTime);
            }

            Restaurant savedRestaurant = restaurantRepository.save(restaurant);
            return ResponseEntity.ok(savedRestaurant);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Check if restaurant is currently open
     * GET /api/restaurants/{restaurantId}/open
     */
    @GetMapping("/{restaurantId}/open")
    public ResponseEntity<?> isRestaurantOpen(@PathVariable int restaurantId) {
        try {
            // Verify restaurant exists
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            LocalTime now = LocalTime.now();
            boolean isOpen = false;

            if (restaurant.getOpeningTime() != null && restaurant.getClosingTime() != null) {
                isOpen = now.isAfter(restaurant.getOpeningTime()) &&
                        now.isBefore(restaurant.getClosingTime());
            }

            JsonObject response = new JsonObject();
            response.addProperty("restaurantId", restaurantId);
            response.addProperty("restaurantName", restaurant.getRestaurantName());
            response.addProperty("isOpen", isOpen);
            response.addProperty("currentTime", now.toString());

            if (restaurant.getOpeningTime() != null) {
                response.addProperty("openingTime", restaurant.getOpeningTime().toString());
            }
            if (restaurant.getClosingTime() != null) {
                response.addProperty("closingTime", restaurant.getClosingTime().toString());
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get today's orders for restaurant
     * GET /api/restaurants/{restaurantId}/orders/today
     */
    @GetMapping("/{restaurantId}/orders/today")
    public ResponseEntity<?> getTodayOrders(@PathVariable int restaurantId) {
        try {
            // Verify restaurant exists
            restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            LocalDate today = LocalDate.now();
            List<FoodOrder> allOrders = ordersRepo.findByRestaurant_Id(restaurantId);
            List<FoodOrder> todayOrders = allOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().equals(today))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(todayOrders);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}