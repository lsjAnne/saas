package backend.digitalhuman.domain;

import java.util.List;
import java.util.Map;

/**
 * 数字人问题类型识别：共享的问题类型常量与关键词路由表。
 * 问答服务（判断如何作答）与学习服务（记忆未答问题）共用同一套类型识别，
 * 保证"答过的类型"和"记住的类型"口径一致。
 */
public final class DigitalHumanQuestionTypes {

    public static final String PRICE = "price";
    public static final String SIZE = "size";
    public static final String MATERIAL = "material";
    public static final String SHIPPING = "shipping";
    public static final String AFTER_SALE = "after_sale";
    public static final String DISCOUNT = "discount";
    public static final String USAGE = "usage";
    public static final String GENERIC = "generic";

    /** 问题类型 → 触发关键词（弹幕命中任一关键词即归为该类型） */
    private static final Map<String, List<String>> TYPE_KEYWORDS = Map.of(
            PRICE, List.of("多少钱", "价格", "贵不贵", "便宜", "价位", "怎么卖", "售价", "多少钱呀"),
            SIZE, List.of("尺码", "大小", "尺寸", "多大", "码数", "合身", "穿得下", "胖", "瘦"),
            MATERIAL, List.of("材质", "面料", "成分", "什么料", "质量", "做工", "耐用", "掉色", "起球"),
            SHIPPING, List.of("发货", "快递", "物流", "几天到", "包邮", "运费", "什么时候发", "多久到"),
            AFTER_SALE, List.of("退货", "退款", "售后", "换货", "七天", "保修", "坏了"),
            DISCOUNT, List.of("优惠", "打折", "优惠券", "满减", "活动", "便宜点", "直播间价格"),
            USAGE, List.of("怎么用", "使用方法", "如何用", "续航", "充电", "功能", "能干什么")
    );

    private DigitalHumanQuestionTypes() {
    }

    /** 识别问题类型：按关键词表逐类匹配，未命中归为 generic */
    public static String detect(String content) {
        for (Map.Entry<String, List<String>> entry : TYPE_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (content.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return GENERIC;
    }
}
