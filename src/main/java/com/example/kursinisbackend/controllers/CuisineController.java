package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.Cuisine;
import com.example.kursinisbackend.model.Restaurant;
import com.example.kursinisbackend.repos.CuisineRepo;
import com.example.kursinisbackend.repos.RestaurantRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuisine")
public class CuisineController {

    @Autowired
    private CuisineRepo cuisineRepo;

    @Autowired
    private RestaurantRepository restaurantRepository;

    /**
     * Get all cuisine items
     * GET /api/cuisine
     */
    @GetMapping
    public ResponseEntity<List<Cuisine>> getAllCuisine() {
        List<Cuisine> cuisines = cuisineRepo.findAll();
        return ResponseEntity.ok(cuisines);
    }

    /**
     * Get cuisine by ID
     * GET /api/cuisine/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCuisineById(@PathVariable int id) {
        try {
            Cuisine cuisine = cuisineRepo.findById(id)
                    .orElseThrow(() -> new Exception("Cuisine item not found"));
            return ResponseEntity.ok(cuisine);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get menu for specific restaurant
     * GET /api/cuisine/restaurant/{restaurantId}
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<?> getRestaurantMenu(@PathVariable int restaurantId) {
        try {
            List<Cuisine> menu = cuisineRepo.getCuisineByRestaurantId(restaurantId);
            return ResponseEntity.ok(menu);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Create new cuisine item
     * POST /api/cuisine
     */
    @PostMapping
    public ResponseEntity<?> createCuisine(@RequestBody Cuisine cuisine) {
        try {
            // Validate required fields
            if (cuisine.getName() == null || cuisine.getName().isEmpty()) {
                return ResponseEntity.badRequest().body("Cuisine name is required");
            }

            if (cuisine.getPrice() == null || cuisine.getPrice() <= 0) {
                return ResponseEntity.badRequest().body("Valid price is required");
            }

            if (cuisine.getRestaurant() == null || cuisine.getRestaurant().getId() == 0) {
                return ResponseEntity.badRequest().body("Restaurant is required");
            }

            // Verify restaurant exists
            Restaurant restaurant = restaurantRepository.findById(cuisine.getRestaurant().getId())
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            cuisine.setRestaurant(restaurant);
            Cuisine savedCuisine = cuisineRepo.save(cuisine);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating cuisine: " + e.getMessage());
        }
    }

    /**
     * Create cuisine item with JSON
     * POST /api/cuisine/json
     */
    @PostMapping("/json")
    public ResponseEntity<?> createCuisineFromJson(@RequestBody String cuisineJson) {
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(cuisineJson, JsonObject.class);

            String name = jsonObject.get("name").getAsString();
            double price = jsonObject.get("price").getAsDouble();
            int restaurantId = jsonObject.get("restaurantId").getAsInt();

            String ingredients = jsonObject.has("ingredients") ?
                    jsonObject.get("ingredients").getAsString() : "";
            boolean spicy = jsonObject.has("spicy") ?
                    jsonObject.get("spicy").getAsBoolean() : false;
            boolean vegan = jsonObject.has("vegan") ?
                    jsonObject.get("vegan").getAsBoolean() : false;

            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                    .orElseThrow(() -> new Exception("Restaurant not found"));

            Cuisine cuisine = new Cuisine(name, ingredients, price, spicy, vegan, restaurant);
            Cuisine savedCuisine = cuisineRepo.save(cuisine);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating cuisine: " + e.getMessage());
        }
    }

    /**
     * Update cuisine item
     * PUT /api/cuisine/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCuisine(
            @PathVariable int id,
            @RequestBody Cuisine updatedCuisine) {
        try {
            Cuisine cuisine = cuisineRepo.findById(id)
                    .orElseThrow(() -> new Exception("Cuisine item not found"));

            // Update fields
            if (updatedCuisine.getName() != null && !updatedCuisine.getName().isEmpty()) {
                cuisine.setName(updatedCuisine.getName());
            }

            if (updatedCuisine.getIngredients() != null) {
                cuisine.setIngredients(updatedCuisine.getIngredients());
            }

            if (updatedCuisine.getPrice() != null && updatedCuisine.getPrice() > 0) {
                cuisine.setPrice(updatedCuisine.getPrice());
            }

            cuisine.setSpicy(updatedCuisine.isSpicy());
            cuisine.setVegan(updatedCuisine.isVegan());

            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.ok(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating cuisine: " + e.getMessage());
        }
    }

    /**
     * Update cuisine item with JSON
     * PUT /api/cuisine/{id}/json
     */
    @PutMapping("/{id}/json")
    public ResponseEntity<?> updateCuisineFromJson(
            @PathVariable int id,
            @RequestBody String cuisineJson) {
        try {
            Cuisine cuisine = cuisineRepo.findById(id)
                    .orElseThrow(() -> new Exception("Cuisine item not found"));

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(cuisineJson, JsonObject.class);

            if (jsonObject.has("name")) {
                cuisine.setName(jsonObject.get("name").getAsString());
            }

            if (jsonObject.has("ingredients")) {
                cuisine.setIngredients(jsonObject.get("ingredients").getAsString());
            }

            if (jsonObject.has("price")) {
                cuisine.setPrice(jsonObject.get("price").getAsDouble());
            }

            if (jsonObject.has("spicy")) {
                cuisine.setSpicy(jsonObject.get("spicy").getAsBoolean());
            }

            if (jsonObject.has("vegan")) {
                cuisine.setVegan(jsonObject.get("vegan").getAsBoolean());
            }

            Cuisine savedCuisine = cuisineRepo.save(cuisine);
            return ResponseEntity.ok(savedCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating cuisine: " + e.getMessage());
        }
    }

    /**
     * Delete cuisine item
     * DELETE /api/cuisine/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCuisine(@PathVariable int id) {
        try {
            Cuisine cuisine = cuisineRepo.findById(id)
                    .orElseThrow(() -> new Exception("Cuisine item not found"));

            cuisineRepo.delete(cuisine);
            return ResponseEntity.ok("Cuisine item deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting cuisine: " + e.getMessage());
        }
    }

    /**
     * Search cuisine by name
     * GET /api/cuisine/search?name={name}
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCuisineByName(@RequestParam String name) {
        try {
            List<Cuisine> allCuisine = cuisineRepo.findAll();
            List<Cuisine> matchingCuisine = allCuisine.stream()
                    .filter(c -> c.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(matchingCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error searching cuisine: " + e.getMessage());
        }
    }

    /**
     * Get vegan options
     * GET /api/cuisine/vegan
     */
    @GetMapping("/vegan")
    public ResponseEntity<?> getVeganCuisine() {
        try {
            List<Cuisine> allCuisine = cuisineRepo.findAll();
            List<Cuisine> veganCuisine = allCuisine.stream()
                    .filter(Cuisine::isVegan)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(veganCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get spicy options
     * GET /api/cuisine/spicy
     */
    @GetMapping("/spicy")
    public ResponseEntity<?> getSpicyCuisine() {
        try {
            List<Cuisine> allCuisine = cuisineRepo.findAll();
            List<Cuisine> spicyCuisine = allCuisine.stream()
                    .filter(Cuisine::isSpicy)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(spicyCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get cuisine by price range
     * GET /api/cuisine/price?min={min}&max={max}
     */
    @GetMapping("/price")
    public ResponseEntity<?> getCuisineByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        try {
            List<Cuisine> allCuisine = cuisineRepo.findAll();
            List<Cuisine> priceRangeCuisine = allCuisine.stream()
                    .filter(c -> c.getPrice() >= min && c.getPrice() <= max)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(priceRangeCuisine);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get vegan options for specific restaurant
     * GET /api/cuisine/restaurant/{restaurantId}/vegan
     */
    @GetMapping("/restaurant/{restaurantId}/vegan")
    public ResponseEntity<?> getRestaurantVeganMenu(@PathVariable int restaurantId) {
        try {
            List<Cuisine> menu = cuisineRepo.getCuisineByRestaurantId(restaurantId);
            List<Cuisine> veganMenu = menu.stream()
                    .filter(Cuisine::isVegan)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(veganMenu);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Bulk create cuisine items
     * POST /api/cuisine/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkCreateCuisine(@RequestBody List<Cuisine> cuisineList) {
        try {
            // Validate restaurant exists for each item
            for (Cuisine cuisine : cuisineList) {
                if (cuisine.getRestaurant() == null || cuisine.getRestaurant().getId() == 0) {
                    return ResponseEntity.badRequest()
                            .body("All cuisine items must have a valid restaurant");
                }

                Restaurant restaurant = restaurantRepository.findById(cuisine.getRestaurant().getId())
                        .orElseThrow(() -> new Exception("Restaurant not found: " +
                                cuisine.getRestaurant().getId()));

                cuisine.setRestaurant(restaurant);
            }

            List<Cuisine> savedCuisines = cuisineRepo.saveAll(cuisineList);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCuisines);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error bulk creating cuisine: " + e.getMessage());
        }
    }
}