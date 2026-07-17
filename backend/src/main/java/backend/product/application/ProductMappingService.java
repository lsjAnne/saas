package backend.product.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.product.domain.repository.ProductRepository;
import backend.product.domain.repository.ProductSourceMappingRepository;
import backend.product.dto.CreateProductMappingRequest;
import backend.product.dto.SwitchProductMappingSupplierRequest;
import backend.product.model.Product;
import backend.product.model.ProductSourceMapping;
import backend.store.domain.repository.StoreRepository;
import backend.store.model.Store;
import backend.supplier.domain.repository.SupplierRepository;
import backend.supplier.model.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductMappingService {

    private static final List<String> ALLOWED_MAPPING_TYPES = List.of("primary", "backup");

    private final AuditLogService auditLogService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSourceMappingRepository productSourceMappingRepository;

    public ProductMappingService(AuditLogService auditLogService,
                                 StoreRepository storeRepository,
                                 ProductRepository productRepository,
                                 SupplierRepository supplierRepository,
                                 ProductSourceMappingRepository productSourceMappingRepository) {
        this.auditLogService = auditLogService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.productSourceMappingRepository = productSourceMappingRepository;
    }

    public List<ProductSourceMapping> listProductMappings(String tenantId) {
        return productSourceMappingRepository.findByStoreIds(ownedStoreIds(tenantId));
    }

    public List<ProductMappingCatalogView> listProductMappingCatalog(String tenantId) {
        List<Store> ownedStores = storeRepository.findByTenantId(tenantId);
        List<String> ownedStoreIds = ownedStores.stream()
                .map(Store::storeId)
                .toList();
        Map<String, Store> storeById = ownedStores.stream()
                .collect(Collectors.toMap(Store::storeId, Function.identity()));
        Map<String, Product> productById = productRepository.findByStoreIds(ownedStoreIds).stream()
                .collect(Collectors.toMap(Product::productId, Function.identity()));
        Map<String, List<ProductSourceMapping>> mappingsByProductId = productSourceMappingRepository.findByStoreIds(ownedStoreIds).stream()
                .collect(Collectors.groupingBy(ProductSourceMapping::productId));

        return java.util.stream.Stream.concat(
                        productById.keySet().stream(),
                        mappingsByProductId.keySet().stream()
                )
                .distinct()
                .map(productId -> toCatalogView(productId, productById.get(productId), mappingsByProductId.get(productId), storeById))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProductMappingCatalogView::productMissing).reversed()
                        .thenComparing(ProductMappingCatalogView::mappingCount, Comparator.reverseOrder())
                        .thenComparing(ProductMappingCatalogView::createdAt, Comparator.reverseOrder()))
                .toList();
    }

    public ProductSourceMapping createProductMapping(String tenantId, CreateProductMappingRequest request) {
        validateMappingType(request.mappingType());
        Store store = requireOwnedStore(tenantId, request.storeId());
        Product product = requireOwnedProduct(tenantId, request.productId());
        Supplier supplier = requireOwnedSupplier(tenantId, request.supplierId());
        requireSameStore(store.storeId(), product.storeId(), "product does not belong to the target store");
        requireSameStore(store.storeId(), supplier.storeId(), "supplier does not belong to the target store");

        if ("primary".equals(request.mappingType())) {
            deactivateExistingPrimaryMappings(product.productId());
        }

        ProductSourceMapping productSourceMapping = productSourceMappingRepository.save(new ProductSourceMapping(
                null,
                request.storeId(),
                request.productId(),
                request.supplierId(),
                request.mappingType(),
                true,
                false,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_PRODUCT_MAPPING", "product_source_mapping", productSourceMapping.productMappingId());
        return productSourceMapping;
    }

    public ProductSourceMapping switchSupplier(String tenantId,
                                               String productMappingId,
                                               SwitchProductMappingSupplierRequest request) {
        ProductSourceMapping current = requireOwnedProductMapping(tenantId, productMappingId);
        requireOwnedProduct(tenantId, current.productId());
        Supplier nextSupplier = requireOwnedSupplier(tenantId, request.supplierId());
        requireSameStore(current.storeId(), nextSupplier.storeId(), "supplier does not belong to the target store");

        productSourceMappingRepository.save(current.withActive(false));
        ProductSourceMapping target = productSourceMappingRepository.findByProductId(current.productId()).stream()
                .filter(mapping -> request.supplierId().equals(mapping.supplierId()))
                .filter(mapping -> current.mappingType().equals(mapping.mappingType()))
                .findFirst()
                .map(mapping -> productSourceMappingRepository.save(mapping.withActive(true)))
                .orElseGet(() -> productSourceMappingRepository.save(new ProductSourceMapping(
                        null,
                        current.storeId(),
                        current.productId(),
                        request.supplierId(),
                        current.mappingType(),
                        true,
                        false,
                        OffsetDateTime.now()
                )));
        auditLogService.recordForTenant(tenantId, "SWITCH_PRODUCT_MAPPING_SUPPLIER", "product_source_mapping", target.productMappingId());
        return target;
    }

    public void clear() {
        productSourceMappingRepository.deleteAll();
    }

    private ProductSourceMapping requireOwnedProductMapping(String tenantId, String productMappingId) {
        ProductSourceMapping mapping = productSourceMappingRepository.findByProductMappingId(productMappingId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, mapping.storeId());
        return mapping;
    }

    private Product requireOwnedProduct(String tenantId, String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, product.storeId());
        return product;
    }

    private Supplier requireOwnedSupplier(String tenantId, String supplierId) {
        Supplier supplier = supplierRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, supplier.storeId());
        return supplier;
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private List<String> ownedStoreIds(String tenantId) {
        return storeRepository.findByTenantId(tenantId).stream()
                .map(Store::storeId)
                .toList();
    }

    private void validateMappingType(String mappingType) {
        if (!ALLOWED_MAPPING_TYPES.contains(mappingType)) {
            throw new BusinessException("7301", "unsupported mapping type", HttpStatus.BAD_REQUEST);
        }
    }

    private void deactivateExistingPrimaryMappings(String productId) {
        for (ProductSourceMapping mapping : productSourceMappingRepository.findByProductId(productId)) {
            if ("primary".equals(mapping.mappingType()) && mapping.active()) {
                productSourceMappingRepository.save(mapping.withActive(false));
            }
        }
    }

    private void requireSameStore(String expectedStoreId, String actualStoreId, String message) {
        if (!expectedStoreId.equals(actualStoreId)) {
            throw new BusinessException("7302", message, HttpStatus.BAD_REQUEST);
        }
    }

    private ProductMappingCatalogView toCatalogView(String productId,
                                                    Product product,
                                                    List<ProductSourceMapping> productMappings,
                                                    Map<String, Store> storeById) {
        List<ProductSourceMapping> mappings = productMappings == null ? List.of() : productMappings;
        Store store = product != null
                ? storeById.get(product.storeId())
                : mappings.isEmpty() ? null : storeById.get(mappings.get(0).storeId());
        if (store == null) {
            return null;
        }

        int activePrimaryCount = (int) mappings.stream()
                .filter(ProductSourceMapping::active)
                .filter(mapping -> "primary".equals(mapping.mappingType()))
                .count();
        int activeBackupCount = (int) mappings.stream()
                .filter(ProductSourceMapping::active)
                .filter(mapping -> "backup".equals(mapping.mappingType()))
                .count();

        boolean productMissing = product == null;
        return new ProductMappingCatalogView(
                productId,
                store.storeId(),
                store.shopName(),
                product == null ? null : product.platformProductId(),
                product == null ? "product master data missing" : product.title(),
                product == null ? "mapping_orphaned" : product.status(),
                product == null ? null : product.healthScore(),
                product == null ? null : product.publishedAt(),
                product == null
                        ? mappings.stream()
                        .map(ProductSourceMapping::createdAt)
                        .max(Comparator.naturalOrder())
                        .orElse(store.createdAt())
                        : product.createdAt(),
                mappings.size(),
                activePrimaryCount,
                activeBackupCount,
                productMissing
        );
    }

    public record ProductMappingCatalogView(
            String productId,
            String storeId,
            String storeName,
            String platformProductId,
            String title,
            String status,
            Integer healthScore,
            OffsetDateTime publishedAt,
            OffsetDateTime createdAt,
            int mappingCount,
            int activePrimaryCount,
            int activeBackupCount,
            boolean productMissing
    ) {
    }
}
