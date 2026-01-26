package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepo extends JpaRepository<Review, Integer> {
}
