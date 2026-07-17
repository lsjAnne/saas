package backend.tenant.context;

import backend.auth.security.AuthTokenService;
import backend.auth.application.support.AuthUserAccessService;
import backend.auth.application.support.AuthenticatedUserView;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String OPERATOR_ID_HEADER = "X-Operator-Id";
    public static final String OPERATOR_TYPE_HEADER = "X-Operator-Type";

    private final AuthTokenService authTokenService;
    private final AuthUserAccessService authUserAccessService;
    private final boolean allowLegacyHeaderContext;

    public TenantContextFilter(AuthTokenService authTokenService,
                               AuthUserAccessService authUserAccessService,
                               @org.springframework.beans.factory.annotation.Value("${app.auth.allow-legacy-header-context:${APP_AUTH_ALLOW_LEGACY_HEADER_CONTEXT:false}}")
                               boolean allowLegacyHeaderContext) {
        this.authTokenService = authTokenService;
        this.authUserAccessService = authUserAccessService;
        this.allowLegacyHeaderContext = allowLegacyHeaderContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        resolveAuthorization(request).ifPresent(TenantContextHolder::set);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Optional<TenantContext> resolveAuthorization(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return resolveLegacyHeaders(request);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            return Optional.empty();
        }
        return authTokenService.parseUserId(token)
                .map(authUserAccessService::load)
                .map(this::toTenantContext);
    }

    private Optional<TenantContext> resolveLegacyHeaders(HttpServletRequest request) {
        if (!allowLegacyHeaderContext) {
            return Optional.empty();
        }
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        String operatorId = request.getHeader(OPERATOR_ID_HEADER);
        String operatorType = request.getHeader(OPERATOR_TYPE_HEADER);
        if ((tenantId == null || tenantId.isBlank())
                && (operatorId == null || operatorId.isBlank())
                && (operatorType == null || operatorType.isBlank())) {
            return Optional.empty();
        }
        return Optional.ofNullable(toTenantContext(authUserAccessService.loadLegacy(
                tenantId == null || tenantId.isBlank() ? "platform" : tenantId,
                operatorId == null || operatorId.isBlank() ? "anonymous" : operatorId,
                operatorType == null || operatorType.isBlank() ? "tenant-user" : operatorType
        )));
    }

    private TenantContext toTenantContext(AuthenticatedUserView userView) {
        if (userView == null) {
            return null;
        }
        return new TenantContext(
                userView.authUser().tenantId() == null || userView.authUser().tenantId().isBlank()
                        ? "platform"
                        : userView.authUser().tenantId(),
                userView.authUser().userId(),
                userView.authUser().operatorType(),
                userView.authUser().organizationId(),
                userView.authUser().roleCode(),
                userView.authUser().displayName(),
                userView.permissionCodes() == null ? Set.of() : Set.copyOf(userView.permissionCodes())
        );
    }
}

