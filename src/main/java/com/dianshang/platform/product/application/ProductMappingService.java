package com.dianshang.platform.product.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.product.domain.repository.ProductRepository;
import com.dianshang.platform.product.domain.repository.ProductSourceMappingRepository;
import com.dianshang.platform.product.dto.CreateProductMappingRequest;
import com.dianshang.platform.product.dto.SwitchProductMappingSupplierRequest;
import com.dianshang.platform.product.model.Product;
import com.dianshang.platform.product.model.ProductSourceMapping;
import com.dianshang.platform.store.domain.repository.StoreRepository;
import com.dianshang.platform.store.model.Store;
import com.dianshang.platform.supplier.domain.repository.SupplierRepository;
import com.dianshang.platform.supplier.model.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

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

    public ProductSourceMapping createProductMapping(String tenantId, CreateProductMappingRequest request) {
        validateMappingType(request.mappingType());
        Store store = requireOwnedStore(tenantId, request.storeId());
        Product product = requireOwnedProduct(tenantId, request.productId());
        Supplier supplier = requireOwnedSupplier(tenantId, request.supplierId());
        requireSameStore(store.storeId(), product.storeId(), "商品不属于当前店铺");
        requireSameStore(store.storeId(), supplier.storeId(), "供应商不属于当前店铺");

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
        Supplier nextSupplier = requireOwnedSupplier(tenantId, request.supplierId());
        requireSameStore(current.storeId(), nextSupplier.storeId(), "供应商不属于当前店铺");

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
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, mapping.storeId());
        return mapping;
    }

    private Product requireOwnedProduct(String tenantId, String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, product.storeId());
        return product;
    }

    private Supplier requireOwnedSupplier(String tenantId, String supplierId) {
        Supplier supplier = supplierRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new BusinessException("1003", "对象不存在", HttpStatus.NOT_FOUND));
        requireOwnedStore(tenantId, supplier.storeId());
        return supplier;
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

    private void validateMappingType(String mappingType) {
        if (!ALLOWED_MAPPING_TYPES.contains(mappingType)) {
            throw new BusinessException("7301", "映射类型非法", HttpStatus.BAD_REQUEST);
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
}
