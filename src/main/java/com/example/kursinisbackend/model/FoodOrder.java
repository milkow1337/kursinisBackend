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
public class FoodOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private Double price;
    @JsonIgnore
    @ManyToOne
    private BasicUser buyer;
    @JsonIgnore
    @ManyToMany
    private List<Cuisine> cuisineList;
    @JsonIgnore
    @OneToOne
    private Chat chat;
    @JsonIgnore
    @ManyToOne
    private Restaurant restaurant;

}
