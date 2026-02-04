package com.example.kursinisbackend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int rating;
    private String reviewText;

    // FIXED: Add proper JSON format annotation for consistent serialization
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateCreated;

    @JsonIgnore
    @ManyToOne
    private BasicUser commentOwner;

    @JsonIgnore
    @ManyToOne
    private BasicUser feedbackUser;

    @JsonIgnore
    @ManyToOne
    private Chat chat;

    public Review(String reviewText, BasicUser commentOwner, Chat chat) {
        this.reviewText = reviewText;
        this.commentOwner = commentOwner;
        this.chat = chat;
        this.dateCreated = LocalDate.now();
    }
}