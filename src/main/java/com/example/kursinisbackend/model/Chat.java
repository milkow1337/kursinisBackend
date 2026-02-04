package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String chatText;

    // FIXED: Add proper JSON format annotation for consistent serialization
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateCreated;

    @JsonIgnore
    @OneToOne(mappedBy = "chat", cascade = CascadeType.ALL)
    private FoodOrder foodOrder;

    @JsonIgnore
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> messages;

    public Chat(String name, FoodOrder foodOrder) {
        this.name = name;
        this.foodOrder = foodOrder;
        this.dateCreated = LocalDate.now();
        this.messages = new ArrayList<>();
    }

    public Chat(String name, String chatText, FoodOrder foodOrder) {
        this.name = name;
        this.chatText = chatText;
        this.dateCreated = LocalDate.now();
        this.foodOrder = foodOrder;
        this.messages = new ArrayList<>();
    }
}