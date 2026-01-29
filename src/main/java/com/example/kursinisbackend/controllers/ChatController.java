package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.BasicUser;
import com.example.kursinisbackend.model.Chat;
import com.example.kursinisbackend.model.FoodOrder;
import com.example.kursinisbackend.model.Review;
import com.example.kursinisbackend.repos.BasicUserRepository;
import com.example.kursinisbackend.repos.ChatRepo;
import com.example.kursinisbackend.repos.OrdersRepo;
import com.example.kursinisbackend.repos.ReviewRepo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @Autowired
    private ChatRepo chatRepo;

    @Autowired
    private ReviewRepo reviewRepo;

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private BasicUserRepository basicUserRepository;

    /**
     * Get all chats
     * GET /api/chats
     */
    @GetMapping
    public ResponseEntity<List<Chat>> getAllChats() {
        List<Chat> chats = chatRepo.findAll();
        return ResponseEntity.ok(chats);
    }

    /**
     * Get chat by ID
     * GET /api/chats/{chatId}
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<?> getChatById(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get chat by order ID
     * GET /api/chats/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getChatByOrderId(@PathVariable int orderId) {
        try {
            Chat chat = chatRepo.getChatByFoodOrder_Id(orderId);

            if (chat == null) {
                // Create chat if it doesn't exist
                FoodOrder order = ordersRepo.findById(orderId)
                        .orElseThrow(() -> new Exception("Order not found"));

                String chatName = "Order #" + orderId + " Chat";
                chat = new Chat(chatName, order);
                chatRepo.save(chat);

                order.setChat(chat);
                ordersRepo.save(order);
            }

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get messages for a chat
     * GET /api/chats/{chatId}/messages
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            List<Review> messages = chat.getMessages();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Send a message to a chat
     * POST /api/chats/{chatId}/messages
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable int chatId,
            @RequestBody String messageJson) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(messageJson, JsonObject.class);

            String messageText = json.get("messageText").getAsString();
            int userId = json.get("userId").getAsInt();

            BasicUser sender = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            Review message = new Review(messageText, sender, chat);
            message.setDateCreated(LocalDate.now());

            // Handle rating
            if (json.has("rating")) {
                message.setRating(json.get("rating").getAsInt());
            }

            reviewRepo.save(message);

            return ResponseEntity.status(HttpStatus.CREATED).body(message);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete a message
     * DELETE /api/chats/{chatId}/messages/{messageId}
     */
    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable int chatId,
            @PathVariable int messageId) {
        try {
            Review message = reviewRepo.findById(messageId)
                    .orElseThrow(() -> new Exception("Message not found"));

            // Verify message belongs to this chat
            if (message.getChat().getId() != chatId) {
                return ResponseEntity.badRequest()
                        .body("Message does not belong to this chat");
            }

            reviewRepo.delete(message);
            return ResponseEntity.ok("Message deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Create a new chat (for orders without chats)
     * POST /api/chats
     */
    @PostMapping
    public ResponseEntity<?> createChat(@RequestBody String chatJson) {
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(chatJson, JsonObject.class);

            int orderId = json.get("orderId").getAsInt();
            String chatName = json.has("chatName") ?
                    json.get("chatName").getAsString() :
                    "Order #" + orderId + " Chat";

            FoodOrder order = ordersRepo.findById(orderId)
                    .orElseThrow(() -> new Exception("Order not found"));

            // Check if chat already exists
            Chat existingChat = chatRepo.getChatByFoodOrder_Id(orderId);
            if (existingChat != null) {
                return ResponseEntity.badRequest()
                        .body("Chat already exists for this order");
            }

            Chat chat = new Chat(chatName, order);
            chatRepo.save(chat);

            order.setChat(chat);
            ordersRepo.save(order);

            return ResponseEntity.status(HttpStatus.CREATED).body(chat);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete a chat
     * DELETE /api/chats/{chatId}
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            chatRepo.delete(chat);
            return ResponseEntity.ok("Chat deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get chats for a user (across all their orders)
     * GET /api/chats/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserChats(@PathVariable int userId) {
        try {
            BasicUser user = basicUserRepository.findById(userId)
                    .orElseThrow(() -> new Exception("User not found"));

            List<FoodOrder> userOrders = ordersRepo.findByBuyer_Id(userId);
            List<Chat> userChats = userOrders.stream()
                    .map(FoodOrder::getChat)
                    .filter(chat -> chat != null)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(userChats);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get message count for a chat
     * GET /api/chats/{chatId}/count
     */
    @GetMapping("/{chatId}/count")
    public ResponseEntity<?> getMessageCount(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            JsonObject response = new JsonObject();
            response.addProperty("chatId", chatId);
            response.addProperty("messageCount", chat.getMessages().size());

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Check if chat is locked (order completed/cancelled)
     * GET /api/chats/{chatId}/locked
     */
    @GetMapping("/{chatId}/locked")
    public ResponseEntity<?> isChatLocked(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            FoodOrder order = chat.getFoodOrder();
            boolean isLocked = order.getOrderStatus().toString().equals("COMPLETED") ||
                    order.getOrderStatus().toString().equals("CANCELLED");

            JsonObject response = new JsonObject();
            response.addProperty("chatId", chatId);
            response.addProperty("isLocked", isLocked);
            response.addProperty("orderStatus", order.getOrderStatus().toString());

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get recent messages (last N messages)
     * GET /api/chats/{chatId}/messages/recent?limit={limit}
     */
    @GetMapping("/{chatId}/messages/recent")
    public ResponseEntity<?> getRecentMessages(
            @PathVariable int chatId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            List<Review> allMessages = chat.getMessages();
            int startIndex = Math.max(0, allMessages.size() - limit);
            List<Review> recentMessages = allMessages.subList(startIndex, allMessages.size());

            return ResponseEntity.ok(recentMessages);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Mark messages as read (future implementation)
     * PUT /api/chats/{chatId}/read
     */
    @PutMapping("/{chatId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable int chatId,
            @RequestBody String userJson) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            Gson gson = new Gson();
            JsonObject json = gson.fromJson(userJson, JsonObject.class);
            int userId = json.get("userId").getAsInt();

            // In a real implementation, track read status per user
            JsonObject response = new JsonObject();
            response.addProperty("message", "Messages marked as read");
            response.addProperty("chatId", chatId);
            response.addProperty("userId", userId);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}