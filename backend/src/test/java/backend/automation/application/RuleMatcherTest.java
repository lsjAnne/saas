package backend.automation.application;

import backend.automation.application.RuleMatcher.RuleMatchContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleMatcherTest {

    private final RuleMatcher matcher = new RuleMatcher();

    private RuleMatchContext orderContext(String amount, String status, String platform) {
        return new RuleMatchContext(
                amount == null ? null : new BigDecimal(amount), status, platform);
    }

    @Test
    void shouldMatchAmountThreshold() {
        RuleMatchContext expensive = orderContext("398.00", "pending_fulfillment", "pdd");
        assertThat(matcher.matches("amount >= 100", expensive)).isTrue();
        assertThat(matcher.matches("amount > 100", expensive)).isTrue();
        assertThat(matcher.matches("amount < 100", expensive)).isFalse();
        assertThat(matcher.matches("amount <= 398", expensive)).isTrue();

        RuleMatchContext cheap = orderContext("9.90", "pending_fulfillment", "pdd");
        assertThat(matcher.matches("amount < 100", cheap)).isTrue();
        assertThat(matcher.matches("amount <= 9.9", cheap)).isTrue();
        assertThat(matcher.matches("amount >= 100", cheap)).isFalse();
        assertThat(matcher.matches("amount == 9.9", cheap)).isTrue();
    }

    @Test
    void shouldMatchStatusEnum() {
        RuleMatchContext pending = orderContext("10", "pending_fulfillment", "pdd");
        assertThat(matcher.matches("status = pending_fulfillment", pending)).isTrue();
        assertThat(matcher.matches("status = shipped", pending)).isFalse();
        assertThat(matcher.matches("status != shipped", pending)).isTrue();
    }

    @Test
    void shouldMatchPlatformEnum() {
        RuleMatchContext pdd = orderContext("10", "pending_fulfillment", "pdd");
        assertThat(matcher.matches("platform = pdd", pdd)).isTrue();
        assertThat(matcher.matches("platform == pdd", pdd)).isTrue();
        assertThat(matcher.matches("platform = douyin", pdd)).isFalse();
        assertThat(matcher.matches("platform != douyin", pdd)).isTrue();
    }

    @Test
    void shouldNotThrowOnIllegalExpressionAndReturnFalse() {
        RuleMatchContext ctx = orderContext("100", "pending_fulfillment", "pdd");
        assertThat(matcher.matches(null, ctx)).isFalse();
        assertThat(matcher.matches("", ctx)).isFalse();
        assertThat(matcher.matches("   ", ctx)).isFalse();
        assertThat(matcher.matches("alert(1)", ctx)).isFalse();
        assertThat(matcher.matches("amount", ctx)).isFalse();
        assertThat(matcher.matches("amount >= ", ctx)).isFalse();
        assertThat(matcher.matches("unknown_field = 1", ctx)).isFalse();
        assertThat(matcher.matches("amount = abc", ctx)).isFalse();
        assertThat(matcher.matches("amount >> 1", ctx)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenContextFieldMissing() {
        RuleMatchContext noAmount = new RuleMatchContext(null, "pending_fulfillment", "pdd");
        assertThat(matcher.matches("amount >= 100", noAmount)).isFalse();
        RuleMatchContext noPlatform = new RuleMatchContext(new BigDecimal("10"), "pending_fulfillment", null);
        assertThat(matcher.matches("platform = pdd", noPlatform)).isFalse();
        assertThat(matcher.matches("platform != pdd", noPlatform)).isTrue();
    }
}
