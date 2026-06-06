package com.dianshang.platform.tenant;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext tenantContext) {
        HOLDER.set(tenantContext);
    }

    public static TenantContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
