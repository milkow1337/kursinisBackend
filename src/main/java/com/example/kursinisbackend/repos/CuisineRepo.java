package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuisineRepo extends JpaRepository<Cuisine, Integer> {
    List<Cuisine> getCuisineByRestaurantId(int id);
}
