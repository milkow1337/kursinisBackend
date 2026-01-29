package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.BasicUserRepository;
import com.example.kursinisbackend.repos.OrdersRepo;
import com.example.kursinisbackend.service.OrderService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private BasicUserRepository basicUserRepository;

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private OrderService orderService;

    /**
     * Get driver by ID
     * GET /api/drivers/{driverId}
     */
    @GetMapping("/{driverId}")
    public ResponseEntity<?> getDriverById(@PathVariable int driverId) {
        try {
            Driver driver = (Driver) basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));
            return ResponseEntity.ok(driver);
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all orders for a specific driver
     * GET /api/drivers/{driverId}/orders
     */
    @GetMapping("/{driverId}/orders")
    public ResponseEntity<?> getDriverOrders(@PathVariable int driverId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            List<FoodOrder> orders = ordersRepo.findByDriver_Id(driverId);
            return ResponseEntity.ok(orders);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get driver's active deliveries (in progress)
     * GET /api/drivers/{driverId}/orders/active
     */
    @GetMapping("/{driverId}/orders/active")
    public ResponseEntity<?> getDriverActiveOrders(@PathVariable int driverId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            List<FoodOrder> allOrders = ordersRepo.findByDriver_Id(driverId);
            List<FoodOrder> activeOrders = allOrders.stream()
                    .filter(order -> order.getOrderStatus() != OrderStatus.COMPLETED &&
                            order.getOrderStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(activeOrders);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get available orders (no driver assigned yet)
     * GET /api/drivers/orders/available
     */
    @GetMapping("/orders/available")
    public ResponseEntity<?> getAvailableOrders() {
        try {
            List<FoodOrder> availableOrders = orderService.getUnclaimedOrders();
            return ResponseEntity.ok(availableOrders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Claim an order (driver accepts delivery)
     * POST /api/drivers/{driverId}/orders/{orderId}/claim
     */
    @PostMapping("/{driverId}/orders/{orderId}/claim")
    public ResponseEntity<?> claimOrder(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            // Verify driver exists
            Driver driver = (Driver) basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Check if order is available for claiming
            if (order.getDriver() != null) {
                return ResponseEntity.badRequest()
                        .body("Order already claimed by another driver");
            }

            if (order.getOrderStatus() != OrderStatus.ACCEPTED) {
                return ResponseEntity.badRequest()
                        .body("Order must be accepted by restaurant before claiming");
            }

            // Assign driver to order
            FoodOrder updatedOrder = orderService.assignDriver(orderId, driver);
            return ResponseEntity.ok(updatedOrder);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Start delivery (driver picked up order from restaurant)
     * POST /api/drivers/{driverId}/orders/{orderId}/start-delivery
     */
    @PostMapping("/{driverId}/orders/{orderId}/start-delivery")
    public ResponseEntity<?> startDelivery(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify this driver is assigned to the order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.badRequest()
                        .body("You are not assigned to this order");
            }

            // Update status to delivering
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);
            return ResponseEntity.ok(updatedOrder);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Mark order as delivered
     * POST /api/drivers/{driverId}/orders/{orderId}/deliver
     */
    @PostMapping("/{driverId}/orders/{orderId}/deliver")
    public ResponseEntity<?> markDelivered(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify this driver is assigned to the order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.badRequest()
                        .body("You are not assigned to this order");
            }

            if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
                return ResponseEntity.badRequest()
                        .body("Order must be in delivering status");
            }

            // Update status to delivered
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
            return ResponseEntity.ok(updatedOrder);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Complete order (final step)
     * POST /api/drivers/{driverId}/orders/{orderId}/complete
     */
    @PostMapping("/{driverId}/orders/{orderId}/complete")
    public ResponseEntity<?> completeOrder(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            // Get the order
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify this driver is assigned to the order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.badRequest()
                        .body("You are not assigned to this order");
            }

            // Update status to completed
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED);
            return ResponseEntity.ok(updatedOrder);

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get driver statistics (earnings, deliveries, ratings)
     * GET /api/drivers/{driverId}/stats
     */
    @GetMapping("/{driverId}/stats")
    public ResponseEntity<?> getDriverStats(@PathVariable int driverId) {
        try {
            // Verify driver exists
            Driver driver = (Driver) basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            List<FoodOrder> allOrders = ordersRepo.findByDriver_Id(driverId);

            JsonObject stats = new JsonObject();
            stats.addProperty("driverId", driverId);
            stats.addProperty("driverName", driver.getName() + " " + driver.getSurname());
            stats.addProperty("vehicleType", driver.getVehicleType().toString());
            stats.addProperty("licence", driver.getLicence());

            // Total deliveries
            stats.addProperty("totalDeliveries", allOrders.size());

            // Completed deliveries
            long completedDeliveries = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .count();
            stats.addProperty("completedDeliveries", completedDeliveries);

            // Active deliveries
            long activeDeliveries = allOrders.stream()
                    .filter(order -> order.getOrderStatus() != OrderStatus.COMPLETED &&
                            order.getOrderStatus() != OrderStatus.CANCELLED)
                    .count();
            stats.addProperty("activeDeliveries", activeDeliveries);

            // Total earnings (sum of completed orders)
            double totalEarnings = allOrders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() * 0.15 : 0.0) // 15% commission
                    .sum();
            stats.addProperty("totalEarnings", totalEarnings);

            // Today's deliveries
            LocalDate today = LocalDate.now();
            long todayDeliveries = allOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().equals(today))
                    .count();
            stats.addProperty("todayDeliveries", todayDeliveries);

            // Average rating (would need Review integration)
            stats.addProperty("averageRating", 4.5); // Placeholder

            return ResponseEntity.ok(stats.toString());

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get driver's earnings
     * GET /api/drivers/{driverId}/earnings
     */
    @GetMapping("/{driverId}/earnings")
    public ResponseEntity<?> getDriverEarnings(@PathVariable int driverId) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            List<FoodOrder> completedOrders = ordersRepo.findByDriver_Id(driverId).stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                    .collect(Collectors.toList());

            JsonObject earnings = new JsonObject();

            // Total earnings
            double totalEarnings = completedOrders.stream()
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() * 0.15 : 0.0)
                    .sum();
            earnings.addProperty("totalEarnings", totalEarnings);

            // Today's earnings
            LocalDate today = LocalDate.now();
            double todayEarnings = completedOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().equals(today))
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() * 0.15 : 0.0)
                    .sum();
            earnings.addProperty("todayEarnings", todayEarnings);

            // This week's earnings
            LocalDate weekAgo = LocalDate.now().minusDays(7);
            double weekEarnings = completedOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().isAfter(weekAgo))
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() * 0.15 : 0.0)
                    .sum();
            earnings.addProperty("weekEarnings", weekEarnings);

            // This month's earnings
            LocalDate monthAgo = LocalDate.now().minusDays(30);
            double monthEarnings = completedOrders.stream()
                    .filter(order -> order.getDateCreated() != null &&
                            order.getDateCreated().isAfter(monthAgo))
                    .mapToDouble(order -> order.getPrice() != null ? order.getPrice() * 0.15 : 0.0)
                    .sum();
            earnings.addProperty("monthEarnings", monthEarnings);

            return ResponseEntity.ok(earnings.toString());

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Update driver location (for future GPS tracking)
     * PUT /api/drivers/{driverId}/location
     */
    @PutMapping("/{driverId}/location")
    public ResponseEntity<?> updateLocation(
            @PathVariable int driverId,
            @RequestBody String locationJson) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            Gson gson = new Gson();
            JsonObject location = gson.fromJson(locationJson, JsonObject.class);

            // In a real implementation, store latitude/longitude in database
            // For now, just acknowledge receipt
            double latitude = location.get("latitude").getAsDouble();
            double longitude = location.get("longitude").getAsDouble();

            JsonObject response = new JsonObject();
            response.addProperty("message", "Location updated");
            response.addProperty("latitude", latitude);
            response.addProperty("longitude", longitude);

            return ResponseEntity.ok(response.toString());

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Toggle driver availability
     * PUT /api/drivers/{driverId}/availability
     */
    @PutMapping("/{driverId}/availability")
    public ResponseEntity<?> toggleAvailability(
            @PathVariable int driverId,
            @RequestBody String availabilityJson) {
        try {
            // Verify driver exists
            basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(availabilityJson, JsonObject.class);
            boolean available = json.get("available").getAsBoolean();

            // In a real implementation, store availability status in database
            JsonObject response = new JsonObject();
            response.addProperty("message", "Availability updated");
            response.addProperty("available", available);

            return ResponseEntity.ok(response.toString());

        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("User is not a driver");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}