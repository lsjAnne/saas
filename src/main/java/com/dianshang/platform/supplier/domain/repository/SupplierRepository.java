package com.dianshang.platform.supplier.domain.repository;

import com.dianshang.platform.supplier.model.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {

    Supplier save(Supplier supplier);

    List<Supplier> findByStoreIds(List<String> storeIds);

    List<Supplier> findByStoreId(String storeId);

    Optional<Supplier> findBySupplierId(String supplierId);

    Optional<Supplier> findByStoreAndPlatformSupplier(String storeId, String supplierPlatformType, String supplierPlatformId);

    void deleteAll();
}
