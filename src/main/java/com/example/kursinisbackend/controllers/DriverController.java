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
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BasicUserRepository basicUserRepository;

    /**
     * Get driver's assigned orders
     */
    @GetMapping("/{driverId}/orders")
    public ResponseEntity<List<FoodOrder>> getDriverOrders(@PathVariable int driverId) {
        List<FoodOrder> orders = ordersRepo.findByDriver_Id(driverId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get driver's active orders (not completed)
     */
    @GetMapping("/{driverId}/orders/active")
    public ResponseEntity<List<FoodOrder>> getDriverActiveOrders(@PathVariable int driverId) {
        List<FoodOrder> orders = ordersRepo.findByDriver_IdAndOrderStatusNot(driverId, OrderStatus.COMPLETED);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get unclaimed orders available for pickup
     */
    @GetMapping("/orders/available")
    public ResponseEntity<List<FoodOrder>> getAvailableOrders() {
        List<FoodOrder> orders = orderService.getUnclaimedOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Claim an order for delivery
     */
    @PostMapping("/{driverId}/orders/{orderId}/claim")
    public ResponseEntity<?> claimOrder(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            // Get driver
            Driver driver = (Driver) basicUserRepository.findById(driverId)
                    .orElseThrow(() -> new Exception("Driver not found"));

            // Check if order is available
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            if (order.getDriver() != null) {
                return ResponseEntity.badRequest()
                        .body("Order has already been claimed");
            }

            if (order.getOrderStatus() == OrderStatus.COMPLETED ||
                    order.getOrderStatus() == OrderStatus.DELIVERED) {
                return ResponseEntity.badRequest()
                        .body("Order is already completed or delivered");
            }

            // Assign driver
            FoodOrder updatedOrder = orderService.assignDriver(orderId, driver);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Start delivery (mark as out for delivery)
     */
    @PostMapping("/{driverId}/orders/{orderId}/start-delivery")
    public ResponseEntity<?> startDelivery(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify driver owns this order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order is not assigned to you");
            }

            // Order must be ready or driver assigned
            if (order.getOrderStatus() != OrderStatus.READY &&
                    order.getOrderStatus() != OrderStatus.DRIVER_ASSIGNED) {
                return ResponseEntity.badRequest()
                        .body("Order is not ready for delivery");
            }

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark order as delivered
     */
    @PostMapping("/{driverId}/orders/{orderId}/deliver")
    public ResponseEntity<?> markAsDelivered(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify driver owns this order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order is not assigned to you");
            }

            if (order.getOrderStatus() != OrderStatus.OUT_FOR_DELIVERY) {
                return ResponseEntity.badRequest()
                        .body("Order must be out for delivery first");
            }

            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark order as completed (final state)
     */
    @PostMapping("/{driverId}/orders/{orderId}/complete")
    public ResponseEntity<?> completeOrder(
            @PathVariable int driverId,
            @PathVariable int orderId) {
        try {
            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Verify driver owns this order
            if (order.getDriver() == null || order.getDriver().getId() != driverId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("This order is not assigned to you");
            }

            if (order.getOrderStatus() != OrderStatus.DELIVERED) {
                return ResponseEntity.badRequest()
                        .body("Order must be delivered first");
            }

            // Complete order and award loyalty points
            FoodOrder updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED);

            // Award loyalty points to customer
            try {
                orderService.addLoyaltyPoints(order.getBuyer().getId(), orderId);
            } catch (Exception e) {
                // Log but don't fail the request if loyalty points fail
                System.err.println("Failed to award loyalty points: " + e.getMessage());
            }

            return ResponseEntity.ok(updatedOrder);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get driver statistics
     */
    @GetMapping("/{driverId}/stats")
    public ResponseEntity<?> getDriverStats(@PathVariable int driverId) {
        try {
            List<FoodOrder> allOrders = ordersRepo.findByDriver_Id(driverId);
            long completedOrders = allOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                    .count();
            long activeOrders = allOrders.stream()
                    .filter(o -> o.getOrderStatus() != OrderStatus.COMPLETED)
                    .count();

            DriverStats stats = new DriverStats();
            stats.setTotalOrders(allOrders.size());
            stats.setCompletedOrders((int) completedOrders);
            stats.setActiveOrders((int) activeOrders);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Inner class for driver statistics
    public static class DriverStats {
        private int totalOrders;
        private int completedOrders;
        private int activeOrders;

        public int getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(int totalOrders) {
            this.totalOrders = totalOrders;
        }

        public int getCompletedOrders() {
            return completedOrders;
        }

        public void setCompletedOrders(int completedOrders) {
            this.completedOrders = completedOrders;
        }

        public int getActiveOrders() {
            return activeOrders;
        }

        public void setActiveOrders(int activeOrders) {
            this.activeOrders = activeOrders;
        }
    }
}