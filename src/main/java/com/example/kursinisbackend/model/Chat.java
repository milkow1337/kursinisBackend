package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import netscape.javascript.JSObject;

import java.time.LocalDate;
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
    private LocalDate dateCreated;
    @JsonIgnore
    @OneToOne(mappedBy = "chat", cascade = CascadeType.ALL)
    private FoodOrder foodOrder;
    @JsonIgnore
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
    private List<Review> messages;

    public Chat(String name, String chatText, FoodOrder foodOrder) {
        this.name = name;
        this.chatText = chatText;
        this.dateCreated = LocalDate.now();
        this.foodOrder = foodOrder;
    }
}
