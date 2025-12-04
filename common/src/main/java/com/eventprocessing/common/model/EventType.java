package com.eventprocessing.common.model;

/**
 * Event types supported by the system
 */
public class EventType {

    // User events
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";

    // Order events
    public static final String ORDER_PLACED = "order.placed";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_SHIPPED = "order.shipped";
    public static final String ORDER_DELIVERED = "order.delivered";
    public static final String ORDER_CANCELLED = "order.cancelled";

    // Payment events
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";

    // System events
    public static final String SYSTEM_HEALTH_CHECK = "system.health_check";
    public static final String SYSTEM_ALERT = "system.alert";
    public static final String SYSTEM_METRIC = "system.metric";

    // Generic events
    public static final String GENERIC_EVENT = "generic.event";

    private EventType() {
        // Private constructor to prevent instantiation
    }
}
