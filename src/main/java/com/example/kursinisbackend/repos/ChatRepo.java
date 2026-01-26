package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepo extends JpaRepository<Chat, Integer> {
    Chat getChatByFoodOrder_Id(int id);
}
