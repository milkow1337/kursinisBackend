package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.FoodOrder;
import com.example.kursinisbackend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepo extends JpaRepository<FoodOrder, Integer> {
    // Existing
    List<FoodOrder> findByBuyer_Id(int buyerId);

    // New query methods for Android app
    List<FoodOrder> findByDriver_Id(int driverId);

    List<FoodOrder> findByRestaurant_Id(int restaurantId);

    List<FoodOrder> findByOrderStatus(OrderStatus status);

    List<FoodOrder> findByRestaurant_IdAndOrderStatus(int restaurantId, OrderStatus status);

    List<FoodOrder> findByDriver_IdAndOrderStatusNot(int driverId, OrderStatus status);

    List<FoodOrder> findByOrderStatusInAndDriverIsNull(List<OrderStatus> statuses);

    // Get orders for a buyer with specific status
    List<FoodOrder> findByBuyer_IdAndOrderStatus(int buyerId, OrderStatus status);
}