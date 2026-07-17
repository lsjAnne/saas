package backend.saas.infrastructure.persistence;

import backend.saas.domain.repository.InvoiceRequestRepository;
import backend.saas.model.InvoiceRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryInvoiceRequestRepository implements InvoiceRequestRepository {

    private final Map<String, InvoiceRequest> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(8000);

    @Override
    public InvoiceRequest save(InvoiceRequest invoiceRequest) {
        String invoiceRequestId = invoiceRequest.invoiceRequestId();
        if (invoiceRequestId == null || invoiceRequestId.isBlank()) {
            invoiceRequestId = JdbcIdCodec.formatInvoiceRequestId(sequence.incrementAndGet());
        }
        InvoiceRequest saved = new InvoiceRequest(
                invoiceRequestId,
                invoiceRequest.tenantId(),
                invoiceRequest.billingOrderId(),
                invoiceRequest.invoiceTitle(),
                invoiceRequest.invoiceTaxNo(),
                invoiceRequest.invoiceStatus(),
                invoiceRequest.createdAt()
        );
        storage.put(saved.invoiceRequestId(), saved);
        return saved;
    }

    @Override
    public List<InvoiceRequest> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(request -> tenantId.equals(request.tenantId()))
                .sorted(Comparator.comparing(InvoiceRequest::createdAt).reversed())
                .toList();
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(8000);
    }
}

