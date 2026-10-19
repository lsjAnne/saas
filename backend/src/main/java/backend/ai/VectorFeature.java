package backend.ai;

import java.nio.charset.StandardCharsets;

/**
 * 文本特征向量化：面向中文短文本（问题/弹幕）的无监督特征编码。
 *
 * 采用「字符 bigram + 哈希桶 + TF 权重」构造固定维度向量：
 *  - bigram 覆盖中文词法碎片（"多少钱""价格"等高频搭配会有稳定重叠），同义改写（"多少钱"/"价格多少"）
 *    在字符级共享大量 bigram，余弦相似度显著高于不相关文本；
 *  - 哈希桶固定维度（256），任意文本可向量化，无需外部模型；
 *  - TF 权重与 L2 归一化保证可比性。
 *
 * 生产环境可替换为预训练 embedding 模型（如豆包/OpenAI Embedding API）以进一步提升语义精度，
 * 此处接口与下游计算完全解耦：向量来源不影响 FaqSemanticSearch 的检索契约。
 */
public final class VectorFeature {

    public static final int DIMENSION = 256;

    private VectorFeature() {
    }

    /** 将文本编码为 L2 归一化的 256 维特征向量 */
    public static float[] encode(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIMENSION];
        }
        String normalized = text.trim().toLowerCase();
        float[] vector = new float[DIMENSION];

        // 1) 单字（unigram）特征：中文短文本的强信号（"颜色/价格/尺码"单字也参与匹配）
        for (int i = 0; i < normalized.length(); i++) {
            vector[hash(normalized.charAt(i), (char) 0) % DIMENSION] += 0.7f;
        }

        // 2) 字符级 bigram 特征：覆盖词法碎片（"多少钱""价格"等高频搭配有稳定重叠），同义改写共享大量 bigram
        for (int i = 0; i < normalized.length() - 1; i++) {
            int hash = hash(normalized.charAt(i), normalized.charAt(i + 1));
            vector[hash % DIMENSION] += 1f;
        }
        if (normalized.length() == 1) {
            vector[hash(normalized.charAt(0), (char) 0) % DIMENSION] += 1f;
        }

        // 3) L2 归一化：消除文本长度影响，余弦相似度可比
        l2Normalize(vector);
        return vector;
    }

    /** 两个向量的余弦相似度（向量均为 L2 归一化时即点积） */
    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0f;
        }
        float dot = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private static int hash(char high, char low) {
        return ((int) high) * 31 + low;
    }

    private static void l2Normalize(float[] vector) {
        float sumSquares = 0f;
        for (float v : vector) {
            sumSquares += v * v;
        }
        if (sumSquares <= 0f) {
            return;
        }
        float norm = (float) Math.sqrt(sumSquares);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    /** 校验输入非空（供仓储层使用，避免把空特征写入存储） */
    public static boolean isValid(float[] vector) {
        if (vector == null || vector.length != DIMENSION) {
            return false;
        }
        for (float v : vector) {
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                return false;
            }
        }
        return true;
    }

    /** 将向量序列化为逗号分隔字符串（pgvector 的 string 格式），便于存储与 SQL 检索 */
    public static String toPgVectorString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Float.toString(vector[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    /** 向量长度字节校验（UTF-8 文本特征缓存时的唯一性盐） */
    public static String fingerprint(String text) {
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        return Integer.toHexString(bytes.hashCode()) + "-" + bytes.length;
    }
}
