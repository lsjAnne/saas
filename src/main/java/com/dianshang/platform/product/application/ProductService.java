package com.dianshang.platform.product.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.product.domain.repository.CandidateProductRepository;
import com.dianshang.platform.product.domain.repository.ProductDraftRepository;
import com.dianshang.platform.product.domain.repository.ProductRepository;
import com.dianshang.platform.product.dto.CreateCandidateProductRequest;
import com.dianshang.platform.product.dto.GenerateProductDraftRequest;
import com.dianshang.platform.product.dto.PublishProductDraftRequest;
import com.dianshang.platform.product.dto.UpdateCandidateProductRequest;
import com.dianshang.platform.product.dto.UpdateProductDraftRequest;
import com.dianshang.platform.product.model.CandidateProduct;
import com.dianshang.platform.product.model.Product;
import com.dianshang.platform.product.model.ProductDraft;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final List<String> ALLOWED_CANDIDATE_STATUSES = List.of(
            "pending_review", "testable", "not_recommended", "in_pool"
    );

    private static final List<String> ALLOWED_DRAFT_STATUSES = List.of(
            "drafting", "pending_fill", "pending_publish", "rejected", "published"
    );

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final CandidateProductRepository candidateProductRepository;
    private final ProductDraftRepository productDraftRepository;
    private final ProductRepository productRepository;

    public ProductService(AuditLogService auditLogService,
                          StoreRepository storeRepository,
                          CandidateProductRepository candidateProductRepository,
                          ProductDraftRepository productDraftRepository,
                          ProductRepository productRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.candidateProductRepository = candidateProductRepository;
        this.productDraftRepository = productDraftRepository;
        this.productRepository = productRepository;
    }

    public List<CandidateProduct> listCandidateProducts(String tenantId) {
        return candidateProductRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public CandidateProduct createCandidateProduct(String tenantId, CreateCandidateProductRequest request) {
        requireOwnedStore(tenantId, request.storeId());
        String sourceHash = hashSourceUrl(request.sourceUrl());
        candidateProductRepository.findByStoreAndSourceUrlHash(request.storeId(), sourceHash)
                .ifPresent(existing -> {
                    throw new BusinessException("7101", "候选商品已存在", HttpStatus.BAD_REQUEST);
                });
        CandidateProduct candidateProduct = candidateProductRepository.save(new CandidateProduct(
                null,
                request.storeId(),
                request.sourceType(),
                request.sourceUrl(),
                sourceHash,
                request.title(),
                request.category(),
                "pending_review",
                request.estimatedProfit(),
                request.riskLevel(),
                request.recommendationReason(),
                request.aiSummary(),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_CANDIDATE_PRODUCT", "candidate_product", candidateProduct.candidateProductId());
        return candidateProduct;
    }

    public CandidateProduct getCandidateProduct(String tenantId, String candidateProductId) {
        CandidateProduct candidateProduct = requireCandidateProduct(candidateProductId);
        requireOwnedStore(tenantId, candidateProduct.storeId());
        return candidateProduct;
    }

    public CandidateProduct updateCandidateProduct(String tenantId,
                                                   String candidateProductId,
                                                   UpdateCandidateProductRequest request) {
        CandidateProduct current = getCandidateProduct(tenantId, candidateProductId);
        String sourceHash = hashSourceUrl(request.sourceUrl());
        candidateProductRepository.findByStoreAndSourceUrlHash(current.storeId(), sourceHash)
                .filter(existing -> !candidateProductId.equals(existing.candidateProductId()))
                .ifPresent(existing -> {
                    throw new BusinessException("7101", "候选商品已存在", HttpStatus.BAD_REQUEST);
                });
        CandidateProduct updated = candidateProductRepository.save(current.withUpdatedFields(
                request.sourceUrl(),
                sourceHash,
                request.title(),
                request.category(),
                request.estimatedProfit(),
                request.riskLevel(),
                request.recommendationReason(),
                request.aiSummary()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_CANDIDATE_PRODUCT", "candidate_product", candidateProductId);
        return updated;
    }

    public CandidateProduct updateCandidateProductStatus(String tenantId, String candidateProductId, String status) {
        validateCandidateStatus(status);
        CandidateProduct current = getCandidateProduct(tenantId, candidateProductId);
        CandidateProduct updated = candidateProductRepository.save(current.withStatus(status));
        auditLogService.recordForTenant(tenantId, "UPDATE_CANDIDATE_PRODUCT_STATUS", "candidate_product", candidateProductId);
        return updated;
    }

    public List<ProductDraft> listProductDrafts(String tenantId) {
        return productDraftRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public ProductDraft generateProductDraft(String tenantId, GenerateProductDraftRequest request) {
        CandidateProduct candidateProduct = getCandidateProduct(tenantId, request.candidateProductId());
        BigDecimal suggestedPrice = request.suggestedPrice() != null
                ? request.suggestedPrice()
                : defaultSuggestedPrice(candidateProduct.estimatedProfit());
        ProductDraft productDraft = productDraftRepository.save(new ProductDraft(
                null,
                candidateProduct.storeId(),
                candidateProduct.candidateProductId(),
                candidateProduct.title() + "丨精选款",
                "核心卖点：" + fallback(candidateProduct.recommendationReason(), "适合快速上架测试"),
                "详情说明：" + fallback(candidateProduct.aiSummary(), "系统已生成基础详情草稿，请继续补充素材与规格信息。"),
                "FAQ：支持七天无理由、默认48小时内发货。",
                suggestedPrice,
                "pending_publish",
                fallback(request.aiVersion(), "ai-v1"),
                OffsetDateTime.now()
        ));
        if (!"in_pool".equals(candidateProduct.status())) {
            candidateProductRepository.save(candidateProduct.withStatus("in_pool"));
        }
        auditLogService.recordForTenant(tenantId, "GENERATE_PRODUCT_DRAFT", "product_draft", productDraft.productDraftId());
        return productDraft;
    }

    public ProductDraft getProductDraft(String tenantId, String productDraftId) {
        ProductDraft productDraft = requireProductDraft(productDraftId);
        requireOwnedStore(tenantId, productDraft.storeId());
        return productDraft;
    }

    public ProductDraft updateProductDraft(String tenantId,
                                           String productDraftId,
                                           UpdateProductDraftRequest request) {
        validateDraftStatus(request.status());
        ProductDraft current = getProductDraft(tenantId, productDraftId);
        ProductDraft updated = productDraftRepository.save(current.withUpdatedFields(
                request.title(),
                request.sellingPoints(),
                request.detailContent(),
                request.faqContent(),
                request.suggestedPrice(),
                request.status()
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_PRODUCT_DRAFT", "product_draft", productDraftId);
        return updated;
    }

    public Product publishProductDraft(String tenantId, String productDraftId, PublishProductDraftRequest request) {
        ProductDraft draft = getProductDraft(tenantId, productDraftId);
        Product product = productRepository.save(new Product(
                null,
                draft.storeId(),
                fallback(request.platformProductId(), "platform-" + UUID.randomUUID()),
                draft.productDraftId(),
                draft.title(),
                "online",
                85,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));
        productDraftRepository.save(draft.withStatus("published"));
        auditLogService.recordForTenant(tenantId, "PUBLISH_PRODUCT_DRAFT", "product", product.productId());
        return product;
    }

    public void clear() {
        productRepository.deleteAll();
        productDraftRepository.deleteAll();
        candidateProductRepository.deleteAll();
    }

    private CandidateProduct requireCandidateProduct(String candidateProductId) {
        return candidateProductRepository.findByCandidateProductId(candidateProductId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
    }

    private ProductDraft requireProductDraft(String productDraftId) {
        return productDraftRepository.findByProductDraftId(productDraftId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "租户上下文非法切换", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private void validateCandidateStatus(String status) {
        if (!ALLOWED_CANDIDATE_STATUSES.contains(status)) {
            throw new BusinessException("7102", "候选商品状态非法", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDraftStatus(String status) {
        if (!ALLOWED_DRAFT_STATUSES.contains(status)) {
            throw new BusinessException("7103", "商品草稿状态非法", HttpStatus.BAD_REQUEST);
        }
    }

    private String hashSourceUrl(String sourceUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(sourceUrl.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("source url hash failed", exception);
        }
    }

    private BigDecimal defaultSuggestedPrice(BigDecimal estimatedProfit) {
        if (estimatedProfit == null) {
            return BigDecimal.valueOf(99.90);
        }
        return estimatedProfit.add(BigDecimal.valueOf(79.90));
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
