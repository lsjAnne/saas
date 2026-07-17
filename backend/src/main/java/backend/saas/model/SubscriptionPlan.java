package backend.saas.model;

public record SubscriptionPlan(
        String planCode,
        String planName,
        String billingType,
        int monthlyPrice,
        int yearlyPrice,
        int seatLimit
) {
}

