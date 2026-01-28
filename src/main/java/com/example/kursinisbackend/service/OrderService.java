package com.example.kursinisbackend.service;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private BasicUserRepository basicUserRepository;

    @Autowired
    private CuisineRepo cuisineRepo;

    // Peak hour multiplier
    private static final double PEAK_HOUR_MULTIPLIER = 1.5;
    private static final int LUNCH_START = 12;
    private static final int LUNCH_END = 14;
    private static final int DINNER_START = 18;
    private static final int DINNER_END = 21;

    /**
     * Calculate dynamic price based on time of day (peak hours)
     */
    public double calculateDynamicPrice(double basePrice) {
        return calculateDynamicPrice(basePrice, LocalDateTime.now());
    }

    /**
     * Calculate dynamic price with specific time
     */
    public double calculateDynamicPrice(double basePrice, LocalDateTime orderTime) {
        if (isPeakHour(orderTime)) {
            return basePrice * PEAK_HOUR_MULTIPLIER;
        }
        return basePrice;
    }

    /**
     * Check if given time is during peak hours
     */
    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= LUNCH_START && hour < LUNCH_END) ||
                (hour >= DINNER_START && hour < DINNER_END);
    }

    /**
     * Calculate base price from cuisine list
     */
    public double calculateBasePrice(List<Cuisine> cuisineList) {
        return cuisineList.stream()
                .mapToDouble(Cuisine::getPrice)
                .sum();
    }

    /**
     * Create order with dynamic pricing
     */
    @Transactional
    public FoodOrder createOrder(String name, BasicUser buyer, List<Cuisine> cuisineList, Restaurant restaurant) {
        double basePrice = calculateBasePrice(cuisineList);
        double finalPrice = calculateDynamicPrice(basePrice);

        FoodOrder order = new FoodOrder(name, finalPrice, buyer, cuisineList, restaurant);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setDateCreated(LocalDate.now());

        return ordersRepo.save(order);
    }

    /**
     * Update order status with validation
     */
    @Transactional
    public FoodOrder updateOrderStatus(int orderId, OrderStatus newStatus) throws Exception {
        FoodOrder order = ordersRepo.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found"));

        // Cannot modify completed orders
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new Exception("Cannot modify completed orders");
        }

        order.setOrderStatus(newStatus);
        order.setDateUpdated(LocalDate.now());

        return ordersRepo.save(order);
    }

    /**
     * Assign driver to order
     */
    @Transactional
    public FoodOrder assignDriver(int orderId, Driver driver) throws Exception {
        FoodOrder order = ordersRepo.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found"));

        // Validate order status
        if (order.getOrderStatus() == OrderStatus.COMPLETED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new Exception("Cannot assign driver to completed/delivered order");
        }

        // Cannot change driver after order has been accepted with a driver
        if (order.getDriver() != null && order.getOrderStatus() != OrderStatus.PLACED) {
            throw new Exception("Cannot change driver after order has been accepted");
        }

        order.setDriver(driver);
        order.setOrderStatus(OrderStatus.DRIVER_ASSIGNED);
        order.setDateUpdated(LocalDate.now());

        return ordersRepo.save(order);
    }

    /**
     * Recalculate order price when dishes are modified
     */
    @Transactional
    public FoodOrder recalculateOrderPrice(int orderId, List<Cuisine> newCuisineList) throws Exception {
        FoodOrder order = ordersRepo.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found"));

        // Cannot modify completed orders
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new Exception("Cannot modify completed orders");
        }

        if (newCuisineList != null && !newCuisineList.isEmpty()) {
            double basePrice = calculateBasePrice(newCuisineList);

            // Apply dynamic pricing based on original order time
            LocalDateTime orderTime = order.getDateCreated() != null ?
                    order.getDateCreated().atStartOfDay() : LocalDateTime.now();
            double newPrice = calculateDynamicPrice(basePrice, orderTime);

            order.setCuisineList(newCuisineList);
            order.setPrice(newPrice);
            order.setDateUpdated(LocalDate.now());
        }

        return ordersRepo.save(order);
    }

    /**
     * Remove cuisine from order and recalculate price
     */
    @Transactional
    public FoodOrder removeCuisineFromOrder(int orderId, int cuisineId) throws Exception {
        FoodOrder order = ordersRepo.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found"));

        // Cannot modify completed or accepted orders
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new Exception("Cannot modify completed orders");
        }

        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new Exception("Can only remove items from orders that haven't been accepted");
        }

        List<Cuisine> cuisineList = order.getCuisineList();
        cuisineList.removeIf(c -> c.getId() == cuisineId);

        if (cuisineList.isEmpty()) {
            throw new Exception("Order must have at least one item");
        }

        return recalculateOrderPrice(orderId, cuisineList);
    }

    /**
     * Get unclaimed orders (for driver marketplace)
     */
    public List<FoodOrder> getUnclaimedOrders() {
        return ordersRepo.findByOrderStatusInAndDriverIsNull(
                List.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.READY)
        );
    }

    /**
     * Check if chat is locked (order completed)
     */
    public boolean isChatLocked(int orderId) {
        return ordersRepo.findById(orderId)
                .map(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .orElse(false);
    }

    /**
     * Calculate loyalty points for an order
     */
    public int calculateLoyaltyPoints(double orderValue) {
        // 1 point per 10 euros spent
        return (int) (orderValue / 10);
    }

    /**
     * Add loyalty points to customer after order completion
     */
    @Transactional
    public void addLoyaltyPoints(int userId, int orderId) throws Exception {
        FoodOrder order = ordersRepo.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found"));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new Exception("Can only award points for completed orders");
        }

        BasicUser customer = order.getBuyer();
        int points = calculateLoyaltyPoints(order.getPrice());
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);

        basicUserRepository.save(customer);
    }
}