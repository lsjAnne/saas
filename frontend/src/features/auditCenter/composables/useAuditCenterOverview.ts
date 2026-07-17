import { shallowRef } from 'vue';

import { getTenantAuditLogs } from '@/services/auditCenterService';
import { getTenantDataExports } from '@/services/tenantCenterService';
import type { AuditLogRecord, TenantDataExportTaskSummary } from '@/services/apiTypes';

export function useAuditCenterOverview() {
  const auditLogs = shallowRef<AuditLogRecord[]>([]);
  const exportTasks = shallowRef<TenantDataExportTaskSummary[]>([]);
  const isLoading = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [auditPayload, exportPayload] = await Promise.all([
        getTenantAuditLogs(token),
        getTenantDataExports(token)
      ]);

      auditLogs.value = auditPayload;
      exportTasks.value = exportPayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '审计与导出中心数据加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    auditLogs,
    exportTasks,
    isLoading,
    errorMessage,
    load
  };
}
