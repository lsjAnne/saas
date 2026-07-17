import { computed, shallowRef } from 'vue';

import type {
  IntegrationReadinessDescriptor,
  ObservabilityOverview,
  ExternalSystemConnectivitySnapshot
} from '@/services/apiTypes';

const EMPTY_READINESS: IntegrationReadinessDescriptor = {
  label: '状态未知',
  tone: 'neutral',
  detail: '暂时无法获取当前接线状态'
};

export function useObservabilityReadiness() {
  const overview = shallowRef<ObservabilityOverview | null>(null);
  const observabilityError = shallowRef<string | null>(null);

  const readyCount = computed(() => overview.value?.readyExternalSystemCount ?? 0);
  const selectedCount = computed(() => overview.value?.requiredExternalSystemCount ?? 0);

  function setOverview(payload: ObservabilityOverview) {
    overview.value = payload;
    observabilityError.value = null;
  }

  function setObservabilityError(message: string) {
    observabilityError.value = message;
  }

  function describeSystem(
    systemCode: string,
    selectedCodes: string[]
  ): IntegrationReadinessDescriptor {
    if (!selectedCodes.includes(systemCode)) {
      return {
        label: '未接入',
        tone: 'neutral',
        detail: '当前租户未将该系统纳入交付范围'
      };
    }

    if (!overview.value) {
      return EMPTY_READINESS;
    }

    const connectivity = overview.value.externalIntegrationConnectivity[systemCode as keyof typeof overview.value.externalIntegrationConnectivity];

    if (!connectivity || typeof connectivity !== 'object' || !('configured' in connectivity)) {
      return EMPTY_READINESS;
    }

    return mapConnectivity(connectivity as ExternalSystemConnectivitySnapshot);
  }

  return {
    overview,
    observabilityError,
    readyCount,
    selectedCount,
    setOverview,
    setObservabilityError,
    describeSystem
  };
}

function mapConnectivity(snapshot: ExternalSystemConnectivitySnapshot): IntegrationReadinessDescriptor {
  if (!snapshot.configured) {
    return {
      label: '已选择，待配置',
      tone: 'warn',
      detail: snapshot.detail || '尚未完成 endpoint 或凭据配置'
    };
  }

  if (snapshot.configured && !snapshot.reachable) {
    return {
      label: '已配置，待联通',
      tone: 'warn',
      detail: snapshot.detail || '配置已存在，但当前探测尚未通过'
    };
  }

  return {
    label: '已就绪',
    tone: 'success',
    detail: snapshot.detail || '当前配置与探测结果均正常'
  };
}
