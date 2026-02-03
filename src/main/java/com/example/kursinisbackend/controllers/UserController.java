package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.BasicUserRepository;
import com.example.kursinisbackend.repos.RestaurantRepository;
import com.example.kursinisbackend.repos.UserRepository;
import com.example.kursinisbackend.service.AuthService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final BasicUserRepository basicUserRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthService authService; // Inject AuthService

    // ============== EXISTING ENDPOINTS ============== //

    @GetMapping(value = "/allUsers")
    public @ResponseBody Iterable<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping(value = "/allRestaurants")
    public @ResponseBody Iterable<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @PostMapping(value = "validateUser")
    public @ResponseBody String getUserByCredentials(@RequestBody String info) {
        System.out.println(info);
        Gson gson = new Gson();
        Properties properties = gson.fromJson(info, Properties.class);
        var login = properties.getProperty("login");
        var psw = properties.getProperty("password"); // This is the plain-text password

        // 1. Find user by login
        User user = userRepository.findByLogin(login); // Assuming findByLogin method exists in UserRepository

        if (user != null) {
            // 2. Verify password using AuthService
            if (authService.verifyUser(psw, user.getPassword())) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("userType", user.getClass().getName());
                jsonObject.addProperty("login", user.getLogin());
                jsonObject.addProperty("password", user.getPassword()); // Send hashed password back (or omit for security)
                jsonObject.addProperty("name", user.getName());
                jsonObject.addProperty("surname", user.getSurname());
                jsonObject.addProperty("id", user.getId());

                // Add type-specific fields
                if (user instanceof BasicUser) {
                    BasicUser basicUser = (BasicUser) user;
                    jsonObject.addProperty("address", basicUser.getAddress());
                    jsonObject.addProperty("loyaltyPoints", basicUser.getLoyaltyPoints());

                    if (user instanceof Driver) {
                        Driver driver = (Driver) user;
                        jsonObject.addProperty("licence", driver.getLicence());
                        jsonObject.addProperty("bDate", driver.getBirthDate().toString()); // Updated to getBirthDate()
                        jsonObject.addProperty("vehicleType", driver.getVehicleType().toString());
                    } else if (user instanceof Restaurant) {
                        Restaurant restaurant = (Restaurant) user;
                        jsonObject.addProperty("restaurantName", restaurant.getRestaurantName());
                        if (restaurant.getOpeningTime() != null) {
                            jsonObject.addProperty("openingTime", restaurant.getOpeningTime().toString());
                        }
                        if (restaurant.getClosingTime() != null) {
                            jsonObject.addProperty("closingTime", restaurant.getClosingTime().toString());
                        }
                    }
                }

                String json = gson.toJson(jsonObject);
                return json;
            }
        }
        return null; // Return null if user not found or password doesn't match
    }

    @PutMapping(value = "updateUser")
    public @ResponseBody User updateUser(@RequestBody User user) {
        userRepository.save(user);
        return userRepository.getReferenceById(user.getId());
    }

    @PutMapping(value = "updateUserById/{id}")
    public @ResponseBody User updateUserById(@RequestBody String info, @PathVariable int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        Gson gson = new Gson();
        Properties properties = gson.fromJson(info, Properties.class);
        var name = properties.getProperty("name");
        user.setName(name);

        userRepository.save(user);
        return userRepository.getReferenceById(user.getId());
    }

    @PostMapping(value = "insertUser")
    public @ResponseBody User createUser(@RequestBody User user) {
        // This endpoint should also hash passwords if it's used for new user creation
        userRepository.save(user);
        return userRepository.findByLogin(user.getLogin()); // Use findByLogin
    }

    @PostMapping(value = "insertBasic")
    public @ResponseBody User createBasicUser(@RequestBody BasicUser user) {
        // This endpoint should also hash passwords if it's used for new user creation
        basicUserRepository.save(user);
        return userRepository.findByLogin(user.getLogin()); // Use findByLogin
    }

    @PostMapping(value = "insertBasicUser")
    public @ResponseBody User createBasicUserAlt(@RequestBody BasicUser basicUser) {
        // This endpoint should also hash passwords if it's used for new user creation
        basicUserRepository.save(basicUser);
        return userRepository.findByLogin(basicUser.getLogin()); // Use findByLogin
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

    // ============== NEW MISSING ENDPOINTS ============== //

    /**
     * Create a Driver user
     * POST /insertDriver
     */
    @PostMapping(value = "insertDriver")
    public ResponseEntity<?> createDriver(@RequestBody Driver driver) {
        try {
            // Validate driver-specific fields
            if (driver.getLicence() == null || driver.getLicence().isEmpty()) {
                return ResponseEntity.badRequest().body("License number is required for drivers");
            }

            if (driver.getBirthDate() == null) { // Updated to getBirthDate()
                return ResponseEntity.badRequest().body("Birth date is required for drivers");
            }

            // Validate age (must be 18+)
            LocalDate minDate = LocalDate.now().minusYears(18);
            if (driver.getBirthDate().isAfter(minDate)) { // Updated to getBirthDate()
                return ResponseEntity.badRequest().body("Driver must be at least 18 years old");
            }

            if (driver.getVehicleType() == null) {
                return ResponseEntity.badRequest().body("Vehicle type is required for drivers");
            }

            // Check if username already exists
            User existingUser = userRepository.findByLogin(driver.getLogin()); // Use findByLogin
            if (existingUser != null) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            // Hash password before saving
            driver.setPassword(authService.encodePassword(driver.getPassword()));

            basicUserRepository.save(driver);
            User savedDriver = userRepository.findByLogin(driver.getLogin()); // Use findByLogin

            return ResponseEntity.status(HttpStatus.CREATED).body(savedDriver);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating driver: " + e.getMessage());
        }
    }

    /**
     * Create a Restaurant user
     * POST /insertRestaurant
     */
    @PostMapping(value = "insertRestaurant")
    public ResponseEntity<?> createRestaurant(@RequestBody Restaurant restaurant) {
        try {
            // Validate restaurant-specific fields
            if (restaurant.getRestaurantName() == null || restaurant.getRestaurantName().isEmpty()) {
                return ResponseEntity.badRequest().body("Restaurant name is required");
            }

            if (restaurant.getOpeningTime() == null || restaurant.getClosingTime() == null) {
                return ResponseEntity.badRequest().body("Opening and closing times are required");
            }

            // Check if username already exists
            User existingUser = userRepository.findByLogin(restaurant.getLogin()); // Use findByLogin
            if (existingUser != null) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            // Hash password before saving
            restaurant.setPassword(authService.encodePassword(restaurant.getPassword()));

            restaurantRepository.save(restaurant);
            User savedRestaurant = userRepository.findByLogin(restaurant.getLogin()); // Use findByLogin

            return ResponseEntity.status(HttpStatus.CREATED).body(savedRestaurant);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating restaurant: " + e.getMessage());
        }
    }

    /**
     * Get user by ID with full details
     * GET /users/{id}
     */
    @GetMapping(value = "users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all drivers
     * GET /allDrivers
     */
    @GetMapping(value = "/allDrivers")
    public ResponseEntity<List<User>> getAllDrivers() {
        List<User> allUsers = userRepository.findAll();
        List<User> drivers = allUsers.stream()
                .filter(user -> user instanceof Driver)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(drivers);
    }

    /**
     * Get all basic users (customers)
     * GET /allBasicUsers
     */
    @GetMapping(value = "/allBasicUsers")
    public ResponseEntity<List<User>> getAllBasicUsers() {
        List<User> allUsers = userRepository.findAll();
        List<User> basicUsers = allUsers.stream()
                .filter(user -> user instanceof BasicUser &&
                        !(user instanceof Driver) &&
                        !(user instanceof Restaurant))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(basicUsers);
    }

    /**
     * Update user profile
     * PUT /users/{id}/profile
     */
    @PutMapping(value = "users/{id}/profile")
    public ResponseEntity<?> updateUserProfile(@PathVariable int id, @RequestBody String profileJson) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            Gson gson = new Gson();
            JsonObject profile = gson.fromJson(profileJson, JsonObject.class);

            // Update common fields
            if (profile.has("name") && !profile.get("name").isJsonNull()) {
                user.setName(profile.get("name").getAsString());
            }
            if (profile.has("surname") && !profile.get("surname").isJsonNull()) {
                user.setSurname(profile.get("surname").getAsString());
            }
            if (profile.has("phoneNumber") && !profile.get("phoneNumber").isJsonNull()) {
                user.setPhoneNumber(profile.get("phoneNumber").getAsString());
            }

            // Update type-specific fields
            if (user instanceof BasicUser) {
                BasicUser basicUser = (BasicUser) user;
                if (profile.has("address") && !profile.get("address").isJsonNull()) {
                    basicUser.setAddress(profile.get("address").getAsString());
                }

                if (user instanceof Driver) {
                    Driver driver = (Driver) user;
                    if (profile.has("vehicleType") && !profile.get("vehicleType").isJsonNull()) {
                        driver.setVehicleType(VehicleType.valueOf(profile.get("vehicleType").getAsString()));
                    }
                } else if (user instanceof Restaurant) {
                    Restaurant restaurant = (Restaurant) user;
                    if (profile.has("restaurantName") && !profile.get("restaurantName").isJsonNull()) {
                        restaurant.setRestaurantName(profile.get("restaurantName").getAsString());
                    }
                    if (profile.has("openingTime") && !profile.get("openingTime").isJsonNull()) {
                        restaurant.setOpeningTime(LocalTime.parse(profile.get("openingTime").getAsString()));
                    }
                    if (profile.has("closingTime") && !profile.get("closingTime").isJsonNull()) {
                        restaurant.setClosingTime(LocalTime.parse(profile.get("closingTime").getAsString()));
                    }
                }
            }

            userRepository.save(user);
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating profile: " + e.getMessage());
        }
    }

    /**
     * Change user password
     * PUT /users/{id}/password
     */
    @PutMapping(value = "users/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable int id, @RequestBody String passwordJson) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            Gson gson = new Gson();
            JsonObject passwordData = gson.fromJson(passwordJson, JsonObject.class);

            String oldPassword = passwordData.get("oldPassword").getAsString();
            String newPassword = passwordData.get("newPassword").getAsString();

            // Verify old password using AuthService
            if (!authService.verifyUser(oldPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body("Incorrect current password");
            }

            // Hash new password before saving
            user.setPassword(authService.encodePassword(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok("Password updated successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error changing password: " + e.getMessage());
        }
    }

    /**
     * Get user statistics
     * GET /users/{id}/stats
     */
    @GetMapping(value = "users/{id}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable int id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new Exception("User not found"));

            JsonObject stats = new JsonObject();
            stats.addProperty("userId", user.getId());
            stats.addProperty("userType", user.getClass().getSimpleName());
            stats.addProperty("accountCreated", user.getDateCreated() != null ?
                    user.getDateCreated().toString() : "N/A");

            if (user instanceof BasicUser) {
                BasicUser basicUser = (BasicUser) user;
                stats.addProperty("loyaltyPoints", basicUser.getLoyaltyPoints());

                // Order statistics would go here
                // This would require counting orders from OrdersRepo
                stats.addProperty("totalOrders", basicUser.getMyOrders() != null ?
                        basicUser.getMyOrders().size() : 0);
            }

            return ResponseEntity.ok(stats.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}