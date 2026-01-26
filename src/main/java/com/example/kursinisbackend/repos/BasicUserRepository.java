package com.example.kursinisbackend.repos;

import com.example.kursinisbackend.model.BasicUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasicUserRepository extends JpaRepository<BasicUser, Integer> {
}
