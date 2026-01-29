package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepo extends JpaRepository<Review, Integer> {
    List<Review> findByCommentOwner_Id(int id);
}
