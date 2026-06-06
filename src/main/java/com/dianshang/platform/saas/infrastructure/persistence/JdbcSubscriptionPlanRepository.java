package com.dianshang.platform.saas.infrastructure.persistence;

import com.dianshang.platform.saas.domain.repository.SubscriptionPlanRepository;
import com.dianshang.platform.saas.model.SubscriptionPlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jdbc", matchIfMissing = true)
public class JdbcSubscriptionPlanRepository implements SubscriptionPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSubscriptionPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SubscriptionPlan> findAll() {
        return jdbcTemplate.query(
                """
                SELECT plan_code, plan_name, billing_type, monthly_price, yearly_price, seat_limit
                FROM subscription_plan
                ORDER BY id
                """,
                (rs, rowNum) -> new SubscriptionPlan(
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("billing_type"),
                        rs.getInt("monthly_price"),
                        rs.getInt("yearly_price"),
                        rs.getInt("seat_limit")
                )
        );
    }

    @Override
    public Optional<SubscriptionPlan> findByPlanCode(String planCode) {
        List<SubscriptionPlan> results = jdbcTemplate.query(
                """
                SELECT plan_code, plan_name, billing_type, monthly_price, yearly_price, seat_limit
                FROM subscription_plan
                WHERE plan_code = ?
                """,
                (rs, rowNum) -> new SubscriptionPlan(
                        rs.getString("plan_code"),
                        rs.getString("plan_name"),
                        rs.getString("billing_type"),
                        rs.getInt("monthly_price"),
                        rs.getInt("yearly_price"),
                        rs.getInt("seat_limit")
                ),
                planCode
        );
        return results.stream().findFirst();
    }

    @Override
    public void saveAll(List<SubscriptionPlan> plans) {
        for (SubscriptionPlan plan : plans) {
            long planId = mapPlanId(plan.planCode());
            int updated = jdbcTemplate.update(
                    """
                    UPDATE subscription_plan
                    SET plan_code = ?, plan_name = ?, billing_type = ?, monthly_price = ?, yearly_price = ?, seat_limit = ?, status = 'enabled'
                    WHERE id = ?
                    """,
                    plan.planCode(),
                    plan.planName(),
                    plan.billingType(),
                    plan.monthlyPrice(),
                    plan.yearlyPrice(),
                    plan.seatLimit(),
                    planId
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        """
                        INSERT INTO subscription_plan (id, plan_code, plan_name, billing_type, monthly_price, yearly_price, seat_limit, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'enabled')
                        """,
                        planId,
                        plan.planCode(),
                        plan.planName(),
                        plan.billingType(),
                        plan.monthlyPrice(),
                        plan.yearlyPrice(),
                        plan.seatLimit()
                );
            }
        }
    }

    private long mapPlanId(String planCode) {
        return switch (planCode) {
            case "trial" -> 1L;
            case "basic" -> 2L;
            case "pro" -> 3L;
            default -> 100L + Math.abs(planCode.hashCode());
        };
    }
}
