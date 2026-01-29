package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User getUserByLoginAndPassword(String login, String password);
    User findByLogin(String login); // Added findByLogin method
}
