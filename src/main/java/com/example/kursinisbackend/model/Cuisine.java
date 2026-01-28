package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Cuisine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    protected String name;
    protected String ingredients; // Single string field, will be persisted
    protected Double price;
    protected boolean spicy = false;
    protected boolean vegan = false;

    @JsonIgnore
    @ManyToMany(mappedBy = "cuisineList", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private List<FoodOrder> orderList;

    @JsonIgnore
    @ManyToOne
    private Restaurant restaurant;

    public Cuisine(String name, String ingredients, Double price, boolean spicy, boolean vegan, Restaurant restaurant) {
        this.name = name;
        this.ingredients = ingredients;
        this.price = price;
        this.spicy = spicy;
        this.vegan = vegan;
        this.restaurant = restaurant;
    }

    @Override
    public String toString() {
        return name;
    }
}