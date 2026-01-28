package com.example.kursinisbackend.controllers;

import com.example.kursinisbackend.model.*;
import com.example.kursinisbackend.repos.*;
import com.example.kursinisbackend.service.OrderService;
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
    private BasicUserRepository basicUserRepository;

    @Autowired
    private OrdersRepo ordersRepo;

    @Autowired
    private OrderService orderService;

    /**
     * Get all chats (admin only)
     */
    @GetMapping
    public ResponseEntity<List<Chat>> getAllChats() {
        List<Chat> chats = chatRepo.findAll();
        return ResponseEntity.ok(chats);
    }

    /**
     * Get chat by ID
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<Chat> getChatById(@PathVariable int chatId) {
        return chatRepo.findById(chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get chat for specific order
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getChatByOrder(@PathVariable int orderId) {
        try {
            Chat chat = chatRepo.getChatByFoodOrder_Id(orderId);

            if (chat == null) {
                // Create chat if doesn't exist
                FoodOrder order = ordersRepo.findById(orderId)
                        .orElseThrow(() -> new Exception("Order not found"));

                chat = new Chat("Order Chat #" + orderId, order);
                chatRepo.save(chat);

                order.setChat(chat);
                ordersRepo.save(order);
            }

            return ResponseEntity.ok(chat);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting chat: " + e.getMessage());
        }
    }

    /**
     * Get all messages in a chat
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            List<Review> messages = chat.getMessages();
            return ResponseEntity.ok(messages != null ? messages : List.of());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting messages: " + e.getMessage());
        }
    }

    /**
     * Send message in chat
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable int chatId,
            @RequestBody SendMessageRequest request) {
        try {
            // Validate input
            if (request.getMessageText() == null || request.getMessageText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message text is required");
            }
            if (request.getUserId() <= 0) {
                return ResponseEntity.badRequest().body("Valid user ID is required");
            }

            // Get chat
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            // Check if chat is locked
            if (chat.getFoodOrder() != null) {
                boolean isLocked = orderService.isChatLocked(chat.getFoodOrder().getId());
                if (isLocked) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Cannot send messages - order is completed");
                }
            }

            // Get user
            BasicUser user = basicUserRepository.findById(request.getUserId())
                    .orElseThrow(() -> new Exception("User not found"));

            // Verify user can participate in this chat
            if (chat.getFoodOrder() != null) {
                FoodOrder order = chat.getFoodOrder();
                boolean canParticipate = false;

                // Check if user is customer
                if (order.getBuyer() != null && order.getBuyer().getId() == user.getId()) {
                    canParticipate = true;
                }
                // Check if user is driver
                else if (order.getDriver() != null && order.getDriver().getId() == user.getId()) {
                    canParticipate = true;
                }
                // Check if user is restaurant
                else if (order.getRestaurant() != null && order.getRestaurant().getId() == user.getId()) {
                    canParticipate = true;
                }

                if (!canParticipate) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("You are not authorized to participate in this chat");
                }
            }

            // Create and save message
            Review message = new Review(request.getMessageText(), user, chat);
            message.setDateCreated(LocalDate.now());
            reviewRepo.save(message);

            return ResponseEntity.status(HttpStatus.CREATED).body(message);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Delete message (admin only)
     */
    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable int chatId,
            @PathVariable int messageId) {
        try {
            Review message = reviewRepo.findById(messageId)
                    .orElseThrow(() -> new Exception("Message not found"));

            // Verify message belongs to this chat
            if (message.getChat() == null || message.getChat().getId() != chatId) {
                return ResponseEntity.badRequest().body("Message does not belong to this chat");
            }

            reviewRepo.delete(message);
            return ResponseEntity.ok("Message deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting message: " + e.getMessage());
        }
    }

    /**
     * Delete entire chat (admin only)
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            chatRepo.delete(chat);
            return ResponseEntity.ok("Chat deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting chat: " + e.getMessage());
        }
    }

    /**
     * Check if chat is locked (order completed)
     */
    @GetMapping("/{chatId}/locked")
    public ResponseEntity<?> isChatLocked(@PathVariable int chatId) {
        try {
            Chat chat = chatRepo.findById(chatId)
                    .orElseThrow(() -> new Exception("Chat not found"));

            boolean isLocked = false;
            if (chat.getFoodOrder() != null) {
                isLocked = orderService.isChatLocked(chat.getFoodOrder().getId());
            }

            return ResponseEntity.ok(new ChatLockStatus(isLocked));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking chat status: " + e.getMessage());
        }
    }

    // Request DTOs
    public static class SendMessageRequest {
        private String messageText;
        private int userId;

        public String getMessageText() {
            return messageText;
        }

        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }

    public static class ChatLockStatus {
        private boolean locked;

        public ChatLockStatus(boolean locked) {
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }
    }
}