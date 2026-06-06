package com.dianshang.platform.organization.controller;

import com.dianshang.platform.common.api.ApiResponse;
import com.dianshang.platform.common.trace.TraceIdHolder;
import com.dianshang.platform.auth.AuthPermissionCodes;
import com.dianshang.platform.organization.application.OrganizationService;
import com.dianshang.platform.organization.dto.CreateOrganizationRequest;
import com.dianshang.platform.organization.dto.InviteOrganizationMemberRequest;
import com.dianshang.platform.organization.dto.UpdateOrganizationMemberRoleRequest;
import com.dianshang.platform.organization.model.Organization;
import com.dianshang.platform.organization.model.OrganizationMember;
import com.dianshang.platform.tenant.TenantAccessSupport;
import com.dianshang.platform.tenant.security.RequireTenantPermission;
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
