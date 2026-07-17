package backend.product.domain.repository;

import backend.product.model.ProductDraft;

import java.util.List;
import java.util.Optional;

public interface ProductDraftRepository {

    ProductDraft save(ProductDraft productDraft);

    List<ProductDraft> findByStoreIds(List<String> storeIds);

    Optional<ProductDraft> findByProductDraftId(String productDraftId);

    void deleteAll();
}

