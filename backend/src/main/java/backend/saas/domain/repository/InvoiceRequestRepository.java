package backend.saas.domain.repository;

import backend.saas.model.InvoiceRequest;

import java.util.List;

public interface InvoiceRequestRepository {

    InvoiceRequest save(InvoiceRequest invoiceRequest);

    List<InvoiceRequest> findByTenantId(String tenantId);

    void deleteAll();
}

