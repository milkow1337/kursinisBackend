package com.example.kursinisbackend.Utils;

public class Constants {

    // ============== BASE URL ============== //
    // IMPORTANT: Update this IP address to match your development machine
    // Find your IP: Windows (ipconfig) | Mac/Linux (ifconfig or ip addr)
    public static final String HOME_URL = "http://192.168.50.103:8080/";

    // ============== USER MANAGEMENT ============== //
    public static final String VALIDATE_USER_URL = HOME_URL + "validateUser";
    public static final String CREATE_BASIC_USER_URL = HOME_URL + "insertBasic";
    public static final String CREATE_DRIVER_URL = HOME_URL + "insertDriver";
    public static final String CREATE_RESTAURANT_URL = HOME_URL + "insertRestaurant";
    public static final String GET_ALL_USERS_URL = HOME_URL + "allUsers";
    public static final String GET_ALL_RESTAURANTS_URL = HOME_URL + "allRestaurants";
    public static final String GET_ALL_DRIVERS_URL = HOME_URL + "allDrivers";
    public static final String GET_ALL_BASIC_USERS_URL = HOME_URL + "allBasicUsers";
    public static final String GET_USER_BY_ID_URL = HOME_URL + "users/"; // + userId
    public static final String UPDATE_USER_PROFILE_URL = HOME_URL + "users/"; // + userId + "/profile"
    public static final String CHANGE_PASSWORD_URL = HOME_URL + "users/"; // + userId + "/password"
    public static final String GET_USER_STATS_URL = HOME_URL + "users/"; // + userId + "/stats"
    public static final String DELETE_USER_URL = HOME_URL + "deleteUser/"; // + userId

    // ============== ORDER MANAGEMENT ============== //
    public static final String CREATE_ORDER = HOME_URL + "createOrder";
    public static final String GET_ORDERS_BY_USER = HOME_URL + "getOrderByUser/"; // + userId
    public static final String GET_ORDER_BY_ID = HOME_URL + "getOrder/"; // + orderId
    public static final String CANCEL_ORDER = HOME_URL + "cancelOrder/"; // + orderId
    public static final String UPDATE_ORDER_STATUS = HOME_URL + "updateOrderStatus/"; // + orderId
    public static final String GET_ORDERS_BY_STATUS = HOME_URL + "orders/status/"; // + status
    public static final String GET_ORDERS_BY_RESTAURANT = HOME_URL + "orders/restaurant/"; // + restaurantId
    public static final String GET_ORDERS_BY_DRIVER = HOME_URL + "orders/driver/"; // + driverId
    public static final String GET_AVAILABLE_ORDERS = HOME_URL + "orders/available";
    public static final String ASSIGN_DRIVER = HOME_URL + "orders/"; // + orderId + "/assignDriver"
    public static final String GET_ORDER_STATS = HOME_URL + "orders/stats";
    public static final String GET_USER_ACTIVE_ORDERS = HOME_URL + "orders/user/"; // + userId + "/active"
    public static final String GET_RESTAURANT_PENDING_ORDERS = HOME_URL + "orders/restaurant/"; // + restaurantId + "/pending"
    public static final String GET_DRIVER_ACTIVE_ORDERS = HOME_URL + "orders/driver/"; // + driverId + "/active"

    // ============== MENU/CUISINE MANAGEMENT ============== //
    public static final String GET_RESTAURANT_MENU = HOME_URL + "getMenuRestaurant/"; // + restaurantId
    public static final String GET_ALL_CUISINE = HOME_URL + "api/cuisine";
    public static final String GET_CUISINE_BY_ID = HOME_URL + "api/cuisine/"; // + cuisineId
    public static final String CREATE_CUISINE = HOME_URL + "api/cuisine";
    public static final String CREATE_CUISINE_JSON = HOME_URL + "api/cuisine/json";
    public static final String UPDATE_CUISINE = HOME_URL + "api/cuisine/"; // + cuisineId
    public static final String UPDATE_CUISINE_JSON = HOME_URL + "api/cuisine/"; // + cuisineId + "/json"
    public static final String DELETE_CUISINE = HOME_URL + "api/cuisine/"; // + cuisineId
    public static final String SEARCH_CUISINE = HOME_URL + "api/cuisine/search?name="; // + searchTerm
    public static final String GET_VEGAN_CUISINE = HOME_URL + "api/cuisine/vegan";
    public static final String GET_SPICY_CUISINE = HOME_URL + "api/cuisine/spicy";
    public static final String GET_CUISINE_BY_PRICE = HOME_URL + "api/cuisine/price?min="; // + min + "&max=" + max
    public static final String GET_RESTAURANT_VEGAN_MENU = HOME_URL + "api/cuisine/restaurant/"; // + restaurantId + "/vegan"
    public static final String BULK_CREATE_CUISINE = HOME_URL + "api/cuisine/bulk";

    // ============== CHAT SYSTEM ============== //
    public static final String GET_MESSAGES_BY_ORDER = HOME_URL + "getMessagesForOrder/"; // + orderId
    public static final String SEND_MESSAGE = HOME_URL + "sendMessage";
    public static final String GET_ALL_CHATS = HOME_URL + "api/chats";
    public static final String GET_CHAT_BY_ID = HOME_URL + "api/chats/"; // + chatId
    public static final String GET_CHAT_BY_ORDER = HOME_URL + "api/chats/orders/"; // + orderId
    public static final String GET_CHAT_MESSAGES = HOME_URL + "api/chats/"; // + chatId + "/messages"
    public static final String SEND_CHAT_MESSAGE = HOME_URL + "api/chats/"; // + chatId + "/messages"
    public static final String DELETE_CHAT_MESSAGE = HOME_URL + "api/chats/"; // + chatId + "/messages/" + messageId
    public static final String DELETE_CHAT = HOME_URL + "api/chats/"; // + chatId
    public static final String IS_CHAT_LOCKED = HOME_URL + "api/chats/"; // + chatId + "/locked"

    // ============== DRIVER SPECIFIC ============== //
    public static final String GET_DRIVER_ORDERS = HOME_URL + "api/drivers/"; // + driverId + "/orders"
    public static final String GET_DRIVER_ACTIVE_DELIVERIES = HOME_URL + "api/drivers/"; // + driverId + "/orders/active"
    public static final String GET_DRIVER_AVAILABLE_ORDERS = HOME_URL + "api/drivers/orders/available";
    public static final String CLAIM_ORDER = HOME_URL + "api/drivers/"; // + driverId + "/orders/" + orderId + "/claim"
    public static final String START_DELIVERY = HOME_URL + "api/drivers/"; // + driverId + "/orders/" + orderId + "/start-delivery"
    public static final String MARK_DELIVERED = HOME_URL + "api/drivers/"; // + driverId + "/orders/" + orderId + "/deliver"
    public static final String COMPLETE_ORDER = HOME_URL + "api/drivers/"; // + driverId + "/orders/" + orderId + "/complete"
    public static final String GET_DRIVER_STATS = HOME_URL + "api/drivers/"; // + driverId + "/stats"

    // ============== RESTAURANT SPECIFIC ============== //
    public static final String GET_RESTAURANT_BY_ID = HOME_URL + "api/restaurants/"; // + restaurantId
    public static final String GET_RESTAURANT_ORDERS = HOME_URL + "api/restaurants/"; // + restaurantId + "/orders"
    public static final String GET_RESTAURANT_ORDERS_BY_STATUS = HOME_URL + "api/restaurants/"; // + restaurantId + "/orders/status/" + status
    public static final String UPDATE_RESTAURANT_ORDER_STATUS = HOME_URL + "api/restaurants/"; // + restaurantId + "/orders/" + orderId + "/status"
    public static final String ACCEPT_ORDER = HOME_URL + "api/restaurants/"; // + restaurantId + "/orders/" + orderId + "/accept"
    public static final String MARK_ORDER_READY = HOME_URL + "api/restaurants/"; // + restaurantId + "/orders/" + orderId + "/ready"
    public static final String ADD_MENU_ITEM = HOME_URL + "api/restaurants/"; // + restaurantId + "/menu"
    public static final String UPDATE_MENU_ITEM = HOME_URL + "api/restaurants/"; // + restaurantId + "/menu/" + cuisineId
    public static final String DELETE_MENU_ITEM = HOME_URL + "api/restaurants/"; // + restaurantId + "/menu/" + cuisineId

    // ============== REVIEWS ============== //
    public static final String GET_ALL_REVIEWS = HOME_URL + "api/reviews";
    public static final String GET_REVIEW_BY_ID = HOME_URL + "api/reviews/"; // + reviewId
    public static final String CREATE_REVIEW = HOME_URL + "api/reviews";
    public static final String UPDATE_REVIEW = HOME_URL + "api/reviews/"; // + reviewId
    public static final String DELETE_REVIEW = HOME_URL + "api/reviews/"; // + reviewId
    public static final String GET_USER_REVIEWS = HOME_URL + "api/reviews/users/"; // + userId
    public static final String GET_REVIEWS_BY_USER = HOME_URL + "api/reviews/users/"; // + userId + "/written"
    public static final String GET_RESTAURANT_REVIEWS = HOME_URL + "api/reviews/restaurants/"; // + restaurantId
    public static final String GET_DRIVER_REVIEWS = HOME_URL + "api/reviews/drivers/"; // + driverId
    public static final String GET_USER_RATING = HOME_URL + "api/reviews/users/"; // + userId + "/rating"

    // ============== HELPER METHODS ============== //

    /**
     * Build URL for getting user by ID
     * @param userId User ID
     * @return Complete URL
     */
    public static String getUserByIdUrl(int userId) {
        return GET_USER_BY_ID_URL + userId;
    }

    /**
     * Build URL for updating user profile
     * @param userId User ID
     * @return Complete URL
     */
    public static String updateUserProfileUrl(int userId) {
        return UPDATE_USER_PROFILE_URL + userId + "/profile";
    }

    /**
     * Build URL for changing password
     * @param userId User ID
     * @return Complete URL
     */
    public static String changePasswordUrl(int userId) {
        return CHANGE_PASSWORD_URL + userId + "/password";
    }

    /**
     * Build URL for getting user stats
     * @param userId User ID
     * @return Complete URL
     */
    public static String getUserStatsUrl(int userId) {
        return GET_USER_STATS_URL + userId + "/stats";
    }

    /**
     * Build URL for getting restaurant menu
     * @param restaurantId Restaurant ID
     * @return Complete URL
     */
    public static String getRestaurantMenuUrl(int restaurantId) {
        return GET_RESTAURANT_MENU + restaurantId;
    }

    /**
     * Build URL for getting order by ID
     * @param orderId Order ID
     * @return Complete URL
     */
    public static String getOrderByIdUrl(int orderId) {
        return GET_ORDER_BY_ID + orderId;
    }

    /**
     * Build URL for claiming an order (driver)
     * @param driverId Driver ID
     * @param orderId Order ID
     * @return Complete URL
     */
    public static String claimOrderUrl(int driverId, int orderId) {
        return "api/drivers/" + driverId + "/orders/" + orderId + "/claim";
    }

    /**
     * Build URL for accepting an order (restaurant)
     * @param restaurantId Restaurant ID
     * @param orderId Order ID
     * @return Complete URL
     */
    public static String acceptOrderUrl(int restaurantId, int orderId) {
        return HOME_URL + "api/restaurants/" + restaurantId + "/orders/" + orderId + "/accept";
    }

    /**
     * Build URL for chat messages
     * @param orderId Order ID
     * @return Complete URL
     */
    public static String getChatMessagesUrl(int orderId) {
        return GET_MESSAGES_BY_ORDER + orderId;
    }

    /**
     * Build search URL with query parameter
     * @param searchTerm Search term
     * @return Complete URL
     */
    public static String searchCuisineUrl(String searchTerm) {
        return SEARCH_CUISINE + searchTerm;
    }

    /**
     * Build price range URL
     * @param min Minimum price
     * @param max Maximum price
     * @return Complete URL
     */
    public static String getCuisineByPriceUrl(double min, double max) {
        return GET_CUISINE_BY_PRICE + min + "&max=" + max;
    }
}







