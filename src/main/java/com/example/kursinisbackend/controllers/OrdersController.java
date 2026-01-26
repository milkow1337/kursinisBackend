package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.Chat;
import com.example.kursinisbackend.model.Cuisine;
import com.example.kursinisbackend.model.FoodOrder;
import com.example.kursinisbackend.model.Review;
import com.example.kursinisbackend.repos.*;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Properties;

@RestController
public class OrdersController {
    @Autowired
    private OrdersRepo ordersRepo;
    @Autowired
    private ChatRepo chatRepo;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private ReviewRepo reviewRepo;
    @Autowired
    private CuisineRepo cuisineRepo;

    @GetMapping(value = "getMenuRestaurant/{id}")
    public Iterable<Cuisine> getRestaurantMenu(@PathVariable int id){
        return cuisineRepo.getCuisineByRestaurantId(id);
    }

    @GetMapping(value = "getOrderByUser/{id}")
    public @ResponseBody Iterable<FoodOrder> getOrdersForUser(@PathVariable int id) {
        return ordersRepo.getFoodOrdersByBuyer_Id(id);
    }

    @GetMapping(value = "getMessagesForOrder/{id}")
    public @ResponseBody Iterable<Review> getMessagesForOrder(@PathVariable int id) {
        Chat chat = chatRepo.getChatByFoodOrder_Id(id);
        if(chat == null){
            FoodOrder order = ordersRepo.getReferenceById(id);
            Chat chat1 = new Chat("User " + order.getBuyer().getLogin(), "Chat order" + id, order);
            order.setChat(chat1);
            chatRepo.save(chat1);
        }
        return chatRepo.getChatByFoodOrder_Id(id).getMessages();
    }

    @PostMapping(value = "sendMessage")
    public @ResponseBody String sendMessage(@RequestBody String info) {
        Gson gson = new Gson();
        Properties properties = gson.fromJson(info, Properties.class);
        var messageText = properties.getProperty("messageText");
        var commentOwner = basicUserRepository.getReferenceById(Integer.valueOf(properties.getProperty("userId")));
        var order = ordersRepo.getReferenceById(Integer.valueOf(properties.getProperty("orderId")));

        Review review = new Review(messageText, commentOwner, order.getChat());
        reviewRepo.save(review);

        return "test";

    }

}
