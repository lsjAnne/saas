package backend.automation.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 规则表达式白名单匹配器。
 * <p>
 * 支持语法（严格三段式：字段 运算符 字面值，空白分隔）：
 * <ul>
 *   <li>金额阈值：amount &gt;= 100 / amount &lt; 50 / amount == 9.9</li>
 *   <li>状态枚举：status = pending_fulfillment / status != shipped</li>
 *   <li>平台枚举：platform = pdd / platform != douyin</li>
 * </ul>
 * 运算符集合：&gt;= &lt;= == != = &gt; &lt;
 * 任何无法解析的表达式一律返回 false（不匹配），禁止任意脚本执行，杜绝注入。
 */
@Component
public class RuleMatcher {

    private static final List<String> OPERATORS = List.of(">=", "<=", "==", "!=", "=", ">", "<");

    public record RuleMatchContext(BigDecimal amount, String orderStatus, String platform) {
    }

    public boolean matches(String ruleExpression, RuleMatchContext context) {
        ParsedExpression parsed = parse(ruleExpression);
        if (parsed == null) {
            return false;
        }
        return switch (parsed.field()) {
            case "amount" -> matchAmount(parsed, context);
            case "status" -> matchStatus(parsed, context);
            case "platform" -> matchPlatform(parsed, context);
            default -> false;
        };
    }

    private boolean matchAmount(ParsedExpression parsed, RuleMatchContext context) {
        if (context.amount() == null) {
            return false;
        }
        BigDecimal threshold;
        try {
            threshold = new BigDecimal(parsed.value());
        } catch (NumberFormatException ignored) {
            return false;
        }
        int cmp = context.amount().compareTo(threshold);
        return switch (parsed.operator()) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case "==" -> cmp == 0;
            case "!=", "=" -> parsed.operator().equals("!=") ? cmp != 0 : cmp == 0;
            default -> false;
        };
    }

    private boolean matchStatus(ParsedExpression parsed, RuleMatchContext context) {
        String actual = context.orderStatus() == null ? "" : context.orderStatus().trim().toLowerCase(Locale.ROOT);
        String expected = parsed.value().trim().toLowerCase(Locale.ROOT);
        return switch (parsed.operator()) {
            case "=", "==" -> actual.equals(expected);
            case "!=" -> !actual.equals(expected);
            default -> false;
        };
    }

    private boolean matchPlatform(ParsedExpression parsed, RuleMatchContext context) {
        String actual = context.platform() == null ? "" : context.platform().trim().toLowerCase(Locale.ROOT);
        String expected = parsed.value().trim().toLowerCase(Locale.ROOT);
        return switch (parsed.operator()) {
            case "=", "==" -> actual.equals(expected);
            case "!=" -> !actual.equals(expected);
            default -> false;
        };
    }

    private ParsedExpression parse(String ruleExpression) {
        if (ruleExpression == null || ruleExpression.isBlank()) {
            return null;
        }
        String trimmed = ruleExpression.trim();
        int opIndex = -1;
        String matchedOp = null;
        for (String op : OPERATORS) {
            int idx = trimmed.indexOf(op);
            if (idx > 0 && (opIndex < 0 || idx < opIndex)) {
                opIndex = idx;
                matchedOp = op;
            }
        }
        if (opIndex <= 0 || matchedOp == null) {
            return null;
        }
        String field = trimmed.substring(0, opIndex).trim().toLowerCase(Locale.ROOT);
        String value = trimmed.substring(opIndex + matchedOp.length()).trim();
        if (!List.of("amount", "status", "platform").contains(field) || value.isEmpty()) {
            return null;
        }
        return new ParsedExpression(field, matchedOp, value);
    }

    private record ParsedExpression(String field, String operator, String value) {
    }
}
