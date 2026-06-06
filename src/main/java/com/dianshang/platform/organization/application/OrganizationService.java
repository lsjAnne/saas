package com.dianshang.platform.organization.application;

import com.dianshang.platform.audit.AuditLogService;
import com.dianshang.platform.auth.AuthUserAccessService;
import com.dianshang.platform.auth.AuthUserProvisioningService;
import com.dianshang.platform.common.exception.BusinessException;
import com.dianshang.platform.organization.domain.repository.OrganizationMemberRepository;
import com.dianshang.platform.organization.domain.repository.OrganizationRepository;
import com.dianshang.platform.organization.dto.InviteOrganizationMemberRequest;
import com.dianshang.platform.organization.model.Organization;
import com.dianshang.platform.organization.model.OrganizationMember;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrganizationService {

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AuthUserProvisioningService authUserProvisioningService;
    private final AuthUserAccessService authUserAccessService;

    public OrganizationService(AuditLogService auditLogService,
                               OrganizationRepository organizationRepository,
                               OrganizationMemberRepository organizationMemberRepository,
                               AuthUserProvisioningService authUserProvisioningService,
                               AuthUserAccessService authUserAccessService) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.authUserProvisioningService = authUserProvisioningService;
        this.authUserAccessService = authUserAccessService;
    }

    public Organization createDefaultOrganization(String tenantId, String ownerName, String mobile) {
        Organization organization = createInternal(tenantId, "默认组织");
        addMemberInternal(
                organization.id(),
                mobile == null || mobile.isBlank() ? "owner-" + organization.id() : mobile,
                ownerName,
                mobile,
                "owner"
        );
        return organization;
    }

    public Organization create(String tenantId, String organizationName) {
        Organization organization = createInternal(tenantId, organizationName);
        auditLogService.record("CREATE_ORGANIZATION", "organization", organization.id());
        return organization;
    }

    public List<Organization> findByTenantId(String tenantId) {
        return organizationRepository.findByTenantId(tenantId);
    }

    public List<OrganizationMember> listMembers(String tenantId, String organizationId) {
        requireOwnedOrganization(tenantId, organizationId);
        return organizationMemberRepository.findByOrganizationId(organizationId);
    }

    public OrganizationMember invite(String tenantId,
                                     String organizationId,
                                     String operatorRoleCode,
                                     InviteOrganizationMemberRequest request) {
        requireOwnedOrganization(tenantId, organizationId);
        String normalizedRoleCode = normalizeAndAuthorizeRole(operatorRoleCode, request.roleCode());
        OrganizationMember member = addMemberInternal(
                organizationId,
                request.userId(),
                request.userName(),
                request.mobile(),
                normalizedRoleCode
        );
        authUserProvisioningService.provisionOrganizationMember(
                tenantId,
                organizationId,
                request.userId(),
                request.userName(),
                request.mobile(),
                normalizedRoleCode
        );
        auditLogService.record("INVITE_ORGANIZATION_MEMBER", "organization_member", member.memberId());
        return member;
    }

    public OrganizationMember updateRole(String tenantId,
                                         String organizationId,
                                         String operatorRoleCode,
                                         String memberId,
                                         String roleCode) {
        requireOwnedOrganization(tenantId, organizationId);
        OrganizationMember current = organizationMemberRepository.findByMemberId(memberId)
                .filter(member -> organizationId.equals(member.organizationId()))
                .orElseThrow(() -> new IllegalArgumentException("organization member不存在"));
        String normalizedRoleCode = normalizeAndAuthorizeRole(operatorRoleCode, roleCode);
        if ("owner".equals(current.roleCode()) && !"owner".equals(normalizedRoleCode)
                && !"owner".equals(operatorRoleCode)) {
            throw new BusinessException("1009", "权限不足", HttpStatus.FORBIDDEN);
        }
        OrganizationMember updated = organizationMemberRepository.save(current.withRole(normalizedRoleCode));
        authUserProvisioningService.provisionOrganizationMember(
                tenantId,
                organizationId,
                updated.userId(),
                updated.userName(),
                updated.mobile(),
                normalizedRoleCode
        );
        auditLogService.record("UPDATE_MEMBER_ROLE", "organization_member", memberId);
        return updated;
    }

    public void clear() {
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private Organization createInternal(String tenantId, String organizationName) {
        return organizationRepository.save(new Organization(
                null,
                tenantId,
                organizationName,
                "active",
                OffsetDateTime.now()
        ));
    }

    private OrganizationMember addMemberInternal(String organizationId,
                                                 String userId,
                                                 String userName,
                                                 String mobile,
                                                 String roleCode) {
        return organizationMemberRepository.save(new OrganizationMember(
                null,
                organizationId,
                userId,
                userName,
                mobile,
                roleCode,
                "active",
                OffsetDateTime.now()
        ));
    }

    private String normalizeAndAuthorizeRole(String operatorRoleCode, String targetRoleCode) {
        String normalizedRoleCode;
        try {
            normalizedRoleCode = authUserAccessService.normalizeTenantRole(targetRoleCode);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("1002", "unsupported tenant role", HttpStatus.BAD_REQUEST);
        }
        if (!authUserAccessService.canAssignTenantRole(operatorRoleCode, normalizedRoleCode)) {
            throw new BusinessException("1009", "权限不足", HttpStatus.FORBIDDEN);
        }
        return normalizedRoleCode;
    }

    private Organization requireOwnedOrganization(String tenantId, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId).orElse(null);
        if (organization == null || !tenantId.equals(organization.tenantId())) {
            throw new IllegalArgumentException("organization不存在或不属于当前租户");
        }
        return organization;
    }
}
