package com.dianshang.platform.saas.domain.repository;

import com.dianshang.platform.saas.model.InvoiceRequest;

import java.util.List;

public interface InvoiceRequestRepository {

    InvoiceRequest save(InvoiceRequest invoiceRequest);

    List<InvoiceRequest> findByTenantId(String tenantId);

    void deleteAll();
}
