package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.*;
import com.example.kursinisbackend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private CuisineRepo cuisineRepo;

    @Autowired
    private OrderService orderService;

    /**
     * Get all restaurants
     */
    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        return ResponseEntity.ok(restaurants);
    }

    /**
     * Get restaurant by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurantById(@PathVariable int id) {
        return restaurantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get restaurant's menu
     */
    @GetMapping("/{id}/menu")
    public ResponseEntity<List<Cuisine>> getRestaurantMenu(@PathVariable int id) {
        List<Cuisine> menu = cuisineRepo.getCuisineByRestaurantId(id);
        return ResponseEntity.ok(menu);
    }

    /**
     * Get restaurant's orders
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<FoodOrder>> getRestaurantOrders(@PathVariable int id) {
        List<FoodOrder> orders = ordersRepo.findByRestaurant_Id(id);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get restaurant's orders filtered by status
     */
    @GetMapping("/{id}/orders/status/{status}")
    public ResponseEntity<List<FoodOrder>> getRestaurantOrdersByStatus(
            @PathVariable int id,
            @PathVariable OrderStatus status) {
        List<FoodOrder> orders = ordersRepo.findByRestaurant_IdAndOrderStatus(id, status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order status (restaurant accepting/preparing order)
     */
    @PutMapping("/{restaurantId}/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable int restaurantId,
            @PathVariable int orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        try {
            // Verify restaurant owns this order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order does not belong to this restaurant");
            }

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Accept order (restaurant confirms order)
     */
    @PostMapping("/{restaurantId}/orders/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(
            @PathVariable int restaurantId,
            @PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order does not belong to this restaurant");
            }

            if (order.getOrderStatus() != OrderStatus.PLACED) {
                return ResponseEntity.badRequest()
                        .body("Can only accept orders in PLACED status");
            }

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.ACCEPTED);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark order as ready for pickup
     */
    @PostMapping("/{restaurantId}/orders/{orderId}/ready")
    public ResponseEntity<?> markOrderReady(
            @PathVariable int restaurantId,
            @PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            if (order.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order does not belong to this restaurant");
            }

            if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
                return ResponseEntity.badRequest()
                        .body("Can only mark accepted orders as ready");
            }

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.READY);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Add menu item to restaurant
     */
    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<?> addMenuItem(
            @PathVariable int restaurantId,
            @RequestBody Cuisine cuisine) {
        try {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            cuisine.setRestaurant(restaurant);
            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.ok(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update menu item
     */
    @PutMapping("/{restaurantId}/menu/{cuisineId}")
    public ResponseEntity<?> updateMenuItem(
            @PathVariable int restaurantId,
            @PathVariable int cuisineId,
            @RequestBody Cuisine updatedCuisine) {
        try {
            Cuisine cuisine = cuisineRepo.findById(cuisineId)
                    .orElseThrow(() -> new Exception("Menu item not found"));

            if (cuisine.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This menu item does not belong to this restaurant");
            }

            cuisine.setName(updatedCuisine.getName());
            cuisine.setIngredients(updatedCuisine.getIngredients());
            cuisine.setPrice(updatedCuisine.getPrice());
            cuisine.setSpicy(updatedCuisine.isSpicy());
            cuisine.setVegan(updatedCuisine.isVegan());

            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.ok(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete menu item
     */
    @DeleteMapping("/{restaurantId}/menu/{cuisineId}")
    public ResponseEntity<?> deleteMenuItem(
            @PathVariable int restaurantId,
            @PathVariable int cuisineId) {
        try {
            Cuisine cuisine = cuisineRepo.findById(cuisineId)
                    .orElseThrow(() -> new Exception("Menu item not found"));

            if (cuisine.getRestaurant().getId() != restaurantId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This menu item does not belong to this restaurant");
            }

            cuisineRepo.delete(cuisine);
            return ResponseEntity.ok("Menu item deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Inner class for status update request
    public static class OrderStatusUpdateRequest {
        private OrderStatus status;

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }
}