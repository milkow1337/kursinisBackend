package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class BasicUser extends User{
    protected String address;
    @JsonIgnore
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    protected List<FoodOrder> myOrders;
    @JsonIgnore
    @OneToMany(mappedBy = "commentOwner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    protected List<Review> myReviews;
    @JsonIgnore
    @OneToMany(mappedBy = "feedbackUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    protected List<Review> feedback;

    public BasicUser(String login, String password, String name, String surname, String phoneNumber, String address) {
        super(login, password, name, surname, phoneNumber);
        this.address = address;
        this.myReviews = new ArrayList<>();
        this.feedback = new ArrayList<>();
        this.myOrders = new ArrayList<>();
    }
}
