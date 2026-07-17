package backend.approval.infrastructure.persistence;

import backend.approval.domain.repository.ApprovalInstanceRepository;
import backend.approval.model.ApprovalInstance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryApprovalInstanceRepository implements ApprovalInstanceRepository {

    private final Map<String, ApprovalInstance> storage = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public List<ApprovalInstance> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .sorted(Comparator.comparing(ApprovalInstance::createdAt).reversed()
                        .thenComparing(ApprovalInstance::approvalId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<ApprovalInstance> findByApprovalId(String approvalId) {
        return Optional.ofNullable(storage.get(approvalId));
    }

    @Override
    public Optional<ApprovalInstance> findPendingByRelated(String tenantId, String relatedType, String relatedId) {
        List<ApprovalInstance> matches = new ArrayList<>(storage.values()).stream()
                .filter(item -> tenantId.equals(item.tenantId()))
                .filter(item -> relatedType.equals(item.relatedType()))
                .filter(item -> relatedId.equals(item.relatedId()))
                .filter(item -> "pending".equals(item.status()))
                .sorted(Comparator.comparing(ApprovalInstance::createdAt).reversed()
                        .thenComparing(ApprovalInstance::approvalId, Comparator.reverseOrder()))
                .toList();
        return matches.stream().findFirst();
    }

    @Override
    public ApprovalInstance save(ApprovalInstance approvalInstance) {
        ApprovalInstance stored = approvalInstance;
        if (approvalInstance.approvalId() == null || approvalInstance.approvalId().isBlank()) {
            stored = new ApprovalInstance(
                    "approval-" + sequence.getAndIncrement(),
                    approvalInstance.tenantId(),
                    approvalInstance.approvalType(),
                    approvalInstance.relatedType(),
                    approvalInstance.relatedId(),
                    approvalInstance.status(),
                    approvalInstance.currentHandlerId(),
                    approvalInstance.remark(),
                    approvalInstance.resultRemark(),
                    approvalInstance.createdAt()
            );
        }
        storage.put(stored.approvalId(), stored);
        return stored;
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(1L);
    }
}

