package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.*;
import com.example.kursinisbackend.service.OrderService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersRepo ordersRepo;
    private final ChatRepo chatRepo;
    private final BasicUserRepository basicUserRepository;
    private final ReviewRepo reviewRepo;
    private final CuisineRepo cuisineRepo;
    private final RestaurantRepository restaurantRepository;
    private final OrderService orderService;


    @GetMapping(value = "getMenuRestaurant/{id}")
    public Iterable<Cuisine> getRestaurantMenu(@PathVariable int id) {
        return cuisineRepo.getCuisineByRestaurantId(id);
    }

    @GetMapping(value = "getOrderByUser/{id}")
    public @ResponseBody Iterable<FoodOrder> getOrdersForUser(@PathVariable int id) {
        return ordersRepo.findByBuyer_Id(id);
    }

    @GetMapping(value = "getMessagesForOrder/{id}")
    public @ResponseBody Iterable<Review> getMessagesForOrder(@PathVariable int id) {
        Chat chat = chatRepo.getChatByFoodOrder_Id(id);
        if (chat == null) {
            FoodOrder order = ordersRepo.getReferenceById(id);
            Chat chat1 = new Chat("User " + order.getBuyer().getLogin(), "Chat order" + id, order);
            order.setChat(chat1);
            chatRepo.save(chat1);
        }
        return chatRepo.getChatByFoodOrder_Id(id).getMessages();
    }

    @PostMapping(value = "sendMessage")
    public @ResponseBody String sendMessage(@RequestBody String info) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(info, JsonObject.class);

        var messageText = jsonObject.get("messageText").getAsString();
        var userId = jsonObject.get("userId").getAsInt();
        var orderId = jsonObject.get("orderId").getAsInt();

        var commentOwner = basicUserRepository.getReferenceById(userId);
        var order = ordersRepo.getReferenceById(orderId);

        Review review = new Review(messageText, commentOwner, order.getChat());

        if (jsonObject.has("rating")) {
            int rating = jsonObject.get("rating").getAsInt();
            review.setRating(rating);
            review.setReviewText(messageText + " [Rating: " + rating + "/5]");
        }

        reviewRepo.save(review);

        return "Message sent";
    }


    @PostMapping("createOrder")
    public ResponseEntity<?> createOrder(@RequestBody String orderJson) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(orderJson, JsonObject.class);

            int userId = jsonObject.get("userId").getAsInt();
            BasicUser buyer = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            int restaurantId = jsonObject.get("restaurantId").getAsInt();
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            List<Cuisine> cuisineList = new ArrayList<>();
            double basePrice = 0.0;

            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject item = itemsArray.get(i).getAsJsonObject();
                int cuisineId = item.get("cuisineId").getAsInt();
                int quantity = item.get("quantity").getAsInt();

                Cuisine cuisine = cuisineRepo.findById(cuisineId)
                        .orElseThrow(() -> new Exception("Cuisine item not found: " + cuisineId));

                for (int j = 0; j < quantity; j++) {
                    cuisineList.add(cuisine);
                }

                basePrice += cuisine.getPrice() * quantity;
            }

            if (cuisineList.isEmpty()) {
                return ResponseEntity.badRequest().body("Order must contain at least one item");
            }

            double finalPrice = orderService.calculateDynamicPrice(basePrice);

            String orderName = "Order for " + buyer.getName();
            FoodOrder order = new FoodOrder(orderName, finalPrice, buyer, cuisineList, restaurant);
            order.setOrderStatus(OrderStatus.PLACED);
            order.setDateCreated(LocalDate.now());

            ordersRepo.save(order);

            Chat chat = new Chat("Order Chat #" + order.getId(), order);
            chatRepo.save(chat);

            order.setChat(chat);
            ordersRepo.save(order);

            try {
                int loyaltyPoints = orderService.calculateLoyaltyPoints(finalPrice);
                buyer.setLoyaltyPoints(buyer.getLoyaltyPoints() + loyaltyPoints);
                basicUserRepository.save(buyer);
            } catch (Exception e) {
                System.err.println("Failed to award loyalty points: " + e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating order: " + e.getMessage());
        }
    }

    @GetMapping("getOrder/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("cancelOrder/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            if (order.getOrderStatus() != OrderStatus.PLACED) {
                return ResponseEntity.badRequest()
                        .body("Can only cancel orders that haven't been accepted");
            }

            ordersRepo.delete(order);
            return ResponseEntity.ok("Order cancelled successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("updateOrderStatus/{orderId}")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable int orderId,
            @RequestBody String statusJson) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(statusJson, JsonObject.class);
            String statusStr = jsonObject.get("status").getAsString();
            OrderStatus newStatus = OrderStatus.valueOf(statusStr);

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            List<FoodOrder> orders = ordersRepo.findByOrderStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid order status: " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/restaurant/{restaurantId}")
    public ResponseEntity<?> getOrdersByRestaurant(@PathVariable int restaurantId) {
        try {
            List<FoodOrder> orders = ordersRepo.findByRestaurant_Id(restaurantId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/driver/{driverId}")
    public ResponseEntity<?> getOrdersByDriver(@PathVariable int driverId) {
        try {
            List<FoodOrder> orders = ordersRepo.findByDriver_Id(driverId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/available")
    public ResponseEntity<?> getAvailableOrders() {
        try {
            List<FoodOrder> orders = orderService.getUnclaimedOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("orders/{orderId}/assignDriver")
    public ResponseEntity<?> assignDriver(
            @PathVariable int orderId,
            @RequestBody String driverJson) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(driverJson, JsonObject.class);
            int driverId = jsonObject.get("driverId").getAsInt();

            Driver driver = (Driver) basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            FoodOrder updatedOrder = orderService.assignDriver(orderId, driver);
            return ResponseEntity.ok(updatedOrder);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/stats")
    public ResponseEntity<?> getOrderStatistics() {
        try {
            List<FoodOrder> allOrders = ordersRepo.findAll();

            JsonObject stats = new JsonObject();
            stats.addProperty("totalOrders", allOrders.size());

            long placedOrders = allOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.PLACED)
                    .count();
            stats.addProperty("placedOrders", placedOrders);

            long completedOrders = allOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                    .count();
            stats.addProperty("completedOrders", completedOrders);

            long activeOrders = allOrders.stream()
                    .filter(o -> o.getOrderStatus() != OrderStatus.COMPLETED)
                    .count();
            stats.addProperty("activeOrders", activeOrders);

            double totalRevenue = allOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(o -> o.getPrice() != null ? o.getPrice() : 0.0)
                    .sum();
            stats.addProperty("totalRevenue", totalRevenue);

            return ResponseEntity.ok(stats.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/user/{userId}/active")
    public ResponseEntity<?> getUserActiveOrders(@PathVariable int userId) {
        try {
            List<FoodOrder> allUserOrders = ordersRepo.findByBuyer_Id(userId);
            List<FoodOrder> activeOrders = allUserOrders.stream()
                    .filter(o -> o.getOrderStatus() != OrderStatus.COMPLETED)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(activeOrders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/restaurant/{restaurantId}/pending")
    public ResponseEntity<?> getRestaurantPendingOrders(@PathVariable int restaurantId) {
        try {
            List<FoodOrder> allRestaurantOrders = ordersRepo.findByRestaurant_Id(restaurantId);
            List<FoodOrder> pendingOrders = allRestaurantOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.PLACED ||
                            o.getOrderStatus() == OrderStatus.ACCEPTED)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(pendingOrders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("orders/driver/{driverId}/active")
    public ResponseEntity<?> getDriverActiveOrders(@PathVariable int driverId) {
        try {
            List<FoodOrder> orders = ordersRepo.findByDriver_IdAndOrderStatusNot(
                    driverId, OrderStatus.COMPLETED);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}