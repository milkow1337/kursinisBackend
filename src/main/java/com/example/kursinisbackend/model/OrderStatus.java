package com.example.kursinisbackend.model;


public enum OrderStatus {
    PLACED,           // Order placed by customer
    ACCEPTED,         // Restaurant accepted the order
    READY,            // Food is ready for pickup
    DRIVER_ASSIGNED,  // Driver has been assigned
    OUT_FOR_DELIVERY, // Order is being delivered
    DELIVERED,        // Order has been delivered
    COMPLETED,        // Order is completed (locked state)
    CANCELLED,        // Order cancelled by user or restaurant
    REJECTED          // Order rejected by restaurant
}