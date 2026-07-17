package backend.organization.controller;

import backend.common.api.ApiResponse;
import backend.common.trace.TraceIdHolder;
import backend.auth.security.AuthPermissionCodes;
import backend.organization.application.OrganizationService;
import backend.organization.dto.CreateOrganizationRequest;
import backend.organization.dto.InviteOrganizationMemberRequest;
import backend.organization.dto.UpdateOrganizationMemberRoleRequest;
import backend.organization.model.Organization;
import backend.organization.model.OrganizationMember;
import backend.tenant.context.TenantAccessSupport;
import backend.auth.security.RequireTenantPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequireTenantPermission(AuthPermissionCodes.ORGANIZATION_MANAGE)
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ApiResponse<List<Organization>> list() {
        return ApiResponse.success(
                organizationService.findByTenantId(TenantAccessSupport.requiredTenantId()),
                TraceIdHolder.get()
        );
    }

    @PostMapping
    public ApiResponse<Organization> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(
                organizationService.create(TenantAccessSupport.requiredTenantId(), request.organizationName()),
                TraceIdHolder.get()
        );
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<OrganizationMember>> listMembers(@PathVariable String id) {
        return ApiResponse.success(
                organizationService.listMembers(TenantAccessSupport.requiredTenantId(), id),
                TraceIdHolder.get()
        );
    }

    @PostMapping("/{id}/members/invite")
    public ApiResponse<OrganizationMember> invite(@PathVariable String id,
                                                  @Valid @RequestBody InviteOrganizationMemberRequest request) {
        return ApiResponse.success(
                organizationService.invite(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        TenantAccessSupport.requiredContext().roleCode(),
                        request
                ),
                TraceIdHolder.get()
        );
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ApiResponse<OrganizationMember> updateRole(@PathVariable String id,
                                                      @PathVariable String memberId,
                                                      @Valid @RequestBody UpdateOrganizationMemberRoleRequest request) {
        return ApiResponse.success(
                organizationService.updateRole(
                        TenantAccessSupport.requiredTenantId(),
                        id,
                        TenantAccessSupport.requiredContext().roleCode(),
                        memberId,
                        request.roleCode()
                ),
                TraceIdHolder.get()
        );
    }
}

