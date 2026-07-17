package backend.support.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.support.domain.repository.SupportSessionRepository;
import backend.support.dto.CreateSupportSessionRequest;
import backend.support.model.SupportSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SupportSessionService {

    private final AuditLogService auditLogService;
    private final SupportSessionRepository supportSessionRepository;

    public SupportSessionService(AuditLogService auditLogService,
                                 SupportSessionRepository supportSessionRepository) {
        this.auditLogService = auditLogService;
        this.supportSessionRepository = supportSessionRepository;
    }

    public SupportSession create(CreateSupportSessionRequest request) {
        SupportSession session = supportSessionRepository.save(new SupportSession(
                UUID.randomUUID().toString(),
                request.tenantId(),
                null,
                request.reason(),
                request.approver(),
                OffsetDateTime.parse(request.expiresAt()),
                "approved",
                "support session created automatically",
                true,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(request.tenantId(), "CREATE_SUPPORT_SESSION", "support_session", session.id());
        return session;
    }

    public List<SupportSession> findAll() {
        return supportSessionRepository.findAll();
    }

    public List<SupportSession> findByTenantId(String tenantId) {
        return supportSessionRepository.findAll().stream()
                .filter(session -> tenantId.equals(session.tenantId()))
                .toList();
    }

    public SupportSession apply(String tenantId, String requesterId, String reason, String requestedExpiresAt) {
        SupportSession session = supportSessionRepository.save(new SupportSession(
                UUID.randomUUID().toString(),
                tenantId,
                requesterId,
                reason,
                null,
                OffsetDateTime.parse(requestedExpiresAt),
                "pending",
                null,
                false,
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "REQUEST_SUPPORT_SESSION", "support_session", session.id());
        return session;
    }

    public SupportSession approve(String id, String approver, String expiresAt, String approvalRemark) {
        SupportSession session = requireSession(id);
        if (!"pending".equals(session.status())) {
            throw new BusinessException("1008", "support session status does not allow approve", HttpStatus.BAD_REQUEST);
        }
        SupportSession approved = supportSessionRepository.save(
                session.approve(approver, OffsetDateTime.parse(expiresAt), approvalRemark)
        );
        auditLogService.recordForTenant(session.tenantId(), "APPROVE_SUPPORT_SESSION", "support_session", id);
        return approved;
    }

    public SupportSession reject(String id, String approver, String approvalRemark) {
        SupportSession session = requireSession(id);
        if (!"pending".equals(session.status())) {
            throw new BusinessException("1008", "support session status does not allow reject", HttpStatus.BAD_REQUEST);
        }
        SupportSession rejected = supportSessionRepository.save(session.reject(approver, approvalRemark));
        auditLogService.recordForTenant(session.tenantId(), "REJECT_SUPPORT_SESSION", "support_session", id);
        return rejected;
    }

    public SupportSession close(String id) {
        SupportSession session = requireSession(id);
        if (!"approved".equals(session.status()) && !"closed".equals(session.status())) {
            throw new BusinessException("1008", "support session status does not allow close", HttpStatus.BAD_REQUEST);
        }
        SupportSession closed = supportSessionRepository.save(session.close());
        auditLogService.recordForTenant(session.tenantId(), "CLOSE_SUPPORT_SESSION", "support_session", id);
        return closed;
    }

    public void clear() {
        supportSessionRepository.deleteAll();
    }

    private SupportSession requireSession(String id) {
        return supportSessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("1003", "support session not found", HttpStatus.NOT_FOUND));
    }
}
