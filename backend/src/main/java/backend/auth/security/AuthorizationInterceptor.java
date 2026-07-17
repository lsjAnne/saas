package backend.auth.security;

import backend.auth.application.AuthService;
import backend.saas.application.SaasTenantService;
import backend.tenant.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final SaasTenantService saasTenantService;

    public AuthorizationInterceptor(AuthService authService,
                                    SaasTenantService saasTenantService) {
        this.authService = authService;
        this.saasTenantService = saasTenantService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePlatformRoles requirePlatformRoles = findPlatformRoles(handlerMethod);
        if (requirePlatformRoles != null) {
            PlatformAccessSupport.requireAny(requirePlatformRoles.value());
        }

        RequireTenantPermission requireTenantPermission = findTenantPermission(handlerMethod);
        if (requireTenantPermission != null) {
            TenantContext tenantContext = TenantAuthorizationSupport.requirePermission(requireTenantPermission.value());
            if (requireTenantPermission.requireSecondaryConfirmation()) {
                authService.assertSensitiveOperationConfirmed(tenantContext.operatorId(), requireTenantPermission.value());
            }
            saasTenantService.assertSensitiveOperationCompliance(tenantContext.tenantId(), requireTenantPermission.value());
        }

        return true;
    }

    private RequirePlatformRoles findPlatformRoles(HandlerMethod handlerMethod) {
        RequirePlatformRoles methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                RequirePlatformRoles.class
        );
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(),
                RequirePlatformRoles.class
        );
    }

    private RequireTenantPermission findTenantPermission(HandlerMethod handlerMethod) {
        RequireTenantPermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                RequireTenantPermission.class
        );
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(),
                RequireTenantPermission.class
        );
    }
}

