import { shallowRef } from 'vue';

import {
  getComplianceAcceptances,
  getPrivacyPolicy,
  getUserAgreement
} from '@/services/tenantCenterService';
import type { ComplianceAcceptance, ComplianceDocument } from '@/services/apiTypes';

export function useSystemSettingsOverview() {
  const privacyPolicy = shallowRef<ComplianceDocument | null>(null);
  const userAgreement = shallowRef<ComplianceDocument | null>(null);
  const acceptances = shallowRef<ComplianceAcceptance[]>([]);
  const isLoading = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);

  async function load(token: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [privacyPayload, agreementPayload, acceptancePayload] = await Promise.all([
        getPrivacyPolicy(token),
        getUserAgreement(token),
        getComplianceAcceptances(token)
      ]);

      privacyPolicy.value = privacyPayload;
      userAgreement.value = agreementPayload;
      acceptances.value = acceptancePayload;
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '系统设置中的合规文档加载失败，请稍后重试';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    privacyPolicy,
    userAgreement,
    acceptances,
    isLoading,
    errorMessage,
    load
  };
}
