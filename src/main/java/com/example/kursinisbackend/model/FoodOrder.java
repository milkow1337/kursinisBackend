package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class FoodOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private Double price;

    @ManyToOne
    private BasicUser buyer;

    @ManyToOne
    private Driver driver; // Driver assignment

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Cuisine> cuisineList;

    @OneToOne(cascade = CascadeType.ALL)
    private Chat chat;

    @ManyToOne
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private LocalDate dateCreated;
    private LocalDate dateUpdated;

    public FoodOrder(String name, Double price, BasicUser buyer, Restaurant restaurant) {
        this.name = name;
        this.price = price;
        this.buyer = buyer;
        this.restaurant = restaurant;
        this.orderStatus = OrderStatus.PLACED;
        this.dateCreated = LocalDate.now();
    }

    public FoodOrder(String name, Double price, BasicUser buyer, List<Cuisine> cuisineList, Restaurant restaurant) {
        this.name = name;
        this.price = price;
        this.buyer = buyer;
        this.cuisineList = cuisineList;
        this.restaurant = restaurant;
        this.orderStatus = OrderStatus.PLACED;
        this.dateCreated = LocalDate.now();
    }

    @Override
    public String toString() {
        return String.format("#%d - %s (€%.2f) - %s", id, name, price, orderStatus);
    }
}