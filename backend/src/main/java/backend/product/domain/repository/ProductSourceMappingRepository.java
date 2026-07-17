package backend.product.domain.repository;

import backend.product.model.ProductSourceMapping;

import java.util.List;
import java.util.Optional;

public interface ProductSourceMappingRepository {

    ProductSourceMapping save(ProductSourceMapping productSourceMapping);

    List<ProductSourceMapping> findByStoreIds(List<String> storeIds);

    List<ProductSourceMapping> findByProductId(String productId);

    Optional<ProductSourceMapping> findByProductMappingId(String productMappingId);

    void deleteAll();
}

