package com.dianshang.platform.product.domain.repository;

import com.dianshang.platform.product.model.CandidateProduct;

import java.util.List;
import java.util.Optional;

public interface CandidateProductRepository {

    CandidateProduct save(CandidateProduct candidateProduct);

    List<CandidateProduct> findByStoreIds(List<String> storeIds);

    Optional<CandidateProduct> findByCandidateProductId(String candidateProductId);

    Optional<CandidateProduct> findByStoreAndSourceUrlHash(String storeId, String sourceUrlHash);

    void deleteAll();
}
