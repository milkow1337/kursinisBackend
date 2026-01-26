package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.BasicUser;
import com.example.kursinisbackend.model.Restaurant;
import com.example.kursinisbackend.model.User;
import com.example.kursinisbackend.repos.BasicUserRepository;
import com.example.kursinisbackend.repos.RestaurantRepository;
import com.example.kursinisbackend.repos.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.persistence.Basic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

@RestController
//Tas pats kas
//@Controller
//@Mapping(path="users")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BasicUserRepository basicUserRepository;
    @Autowired
    private RestaurantRepository restaurantRepository;


    @GetMapping(value = "/allUsers")
    public @ResponseBody Iterable<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping(value = "/allRestaurants")
    public @ResponseBody Iterable<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    //Nedaryti sito produkcineje
//    @GetMapping(value = "validateUser") // http://localhost:8080/validateUser
//    public @ResponseBody User getUserByCredentials(@RequestParam String login, @RequestParam String password){
//        return userRepository.getUserByLoginAndPassword(login, password);
//    }

    @PostMapping(value = "validateUser") //http://localhost:8080/validateUser
    public @ResponseBody String getUserByCredentials(@RequestBody String info) {
        System.out.println(info);
        //?Kaip parsint
        Gson gson = new Gson();
        Properties properties = gson.fromJson(info, Properties.class);
        var login = properties.getProperty("login");
        var psw = properties.getProperty("password");
        User user = userRepository.getUserByLoginAndPassword(login, psw);
        if (user != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("userType", user.getClass().getName());
            jsonObject.addProperty("login", user.getLogin());
            jsonObject.addProperty("password", user.getPassword());
            jsonObject.addProperty("name", user.getName());
            jsonObject.addProperty("surname", user.getSurname());
            jsonObject.addProperty("id", user.getId());

            String json = gson.toJson(jsonObject);

            return json;
        }
        return null;
    }

    @PutMapping(value = "updateUser")
    public @ResponseBody User updateUser(@RequestBody User user) {
        userRepository.save(user);
        return userRepository.getReferenceById(user.getId());
    }

    @PutMapping(value = "updateUserById/{id}")
    public @ResponseBody User updateUserById(@RequestBody String info, @PathVariable int id) {

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException()); //cia noriu savo custom error

        Gson gson = new Gson();
        Properties properties = gson.fromJson(info, Properties.class);
        var name = properties.getProperty("name");
        user.setName(name);

        userRepository.save(user);
        return userRepository.getReferenceById(user.getId());
    }

    @PostMapping(value = "insertUser")
    public @ResponseBody User createUser(@RequestBody User user) {
        userRepository.save(user);
        return userRepository.getUserByLoginAndPassword(user.getLogin(), user.getPassword());
    }
//    @PostMapping(value = "insertDriver")
//    public @ResponseBody User createDriver(@RequestBody Driver user) {
//        dri.save(user);
//        return userRepository.getUserByLoginAndPassword(user.getLogin(), user.getPassword());
//    }

    @PostMapping(value = "insertBasic")
    public @ResponseBody User createUser(@RequestBody BasicUser user) {
        basicUserRepository.save(user);
        return userRepository.getUserByLoginAndPassword(user.getLogin(), user.getPassword());
    }

    @PostMapping(value = "insertBasicUser")
    public @ResponseBody User createBasicUser(@RequestBody BasicUser basicUser) {
        basicUserRepository.save(basicUser);
        return userRepository.getUserByLoginAndPassword(basicUser.getLogin(), basicUser.getPassword());
    }

    @DeleteMapping(value = "deleteUser/{id}")
    public @ResponseBody String deleteUser(@PathVariable int id) {
        userRepository.deleteById(id);
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            return "fail on delete";
        } else {
            return "yay";
        }

    }
}
