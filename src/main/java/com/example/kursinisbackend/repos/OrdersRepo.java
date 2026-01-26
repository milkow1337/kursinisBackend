package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepo extends JpaRepository<FoodOrder, Integer> {
    List<FoodOrder> getFoodOrdersByBuyer_Id(int id);

}
