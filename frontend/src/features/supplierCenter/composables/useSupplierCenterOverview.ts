import { computed, shallowRef } from 'vue';

import {
  createSupplier,
  createSupplierDeliveryAppointment,
  createSupplierRiskEvent,
  createSupplierSettlementStatement,
  getSupplier,
  getSupplierSettlementStatements,
  getSuppliers,
  getSupplierSrmLinkage,
  reviewSupplierAdmission,
  saveSupplierScorecard,
  setBackupSupplier,
  setPrimarySupplier,
  updateSupplier
} from '@/services/supplierCenterService';
import { getStores } from '@/services/storeChannelService';
import type {
  CreateSupplierAdmissionReviewPayload,
  CreateSupplierDeliveryAppointmentPayload,
  CreateSupplierPayload,
  CreateSupplierRiskEventPayload,
  CreateSupplierScorecardPayload,
  CreateSupplierSettlementStatementPayload,
  Store,
  Supplier,
  SupplierAdmissionReview,
  SupplierDeliveryAppointment,
  SupplierRiskEvent,
  SupplierScorecard,
  SupplierSettlementStatement,
  SupplierSrmLinkage,
  UpdateSupplierPayload
} from '@/services/apiTypes';

export type SupplierProfileMode = 'create' | 'edit';

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function supplierPriority(supplier: Supplier) {
  if (supplier.primary) {
    return 5;
  }

  if (normalizeText(supplier.riskLevel).includes('high')) {
    return 4;
  }

  if (supplier.backup) {
    return 3;
  }

  if (supplier.blacklistFlag) {
    return 1;
  }

  return 2;
}

export function useSupplierCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const suppliers = shallowRef<Supplier[]>([]);
  const activeStoreId = shallowRef('all');
  const selectedSupplierId = shallowRef<string | null>(null);
  const selectedSupplierDetail = shallowRef<Supplier | null>(null);
  const profileMode = shallowRef<SupplierProfileMode>('edit');
  const srmLinkage = shallowRef<SupplierSrmLinkage | null>(null);
  const latestAdmissionReview = shallowRef<SupplierAdmissionReview | null>(null);
  const latestScorecard = shallowRef<SupplierScorecard | null>(null);
  const latestDeliveryAppointment = shallowRef<SupplierDeliveryAppointment | null>(null);
  const latestRiskEvent = shallowRef<SupplierRiskEvent | null>(null);
  const settlementStatements = shallowRef<SupplierSettlementStatement[]>([]);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const sortedStores = computed(() =>
    [...stores.value].sort(
      (left, right) => toTimestamp(right.createdAt) - toTimestamp(left.createdAt)
    )
  );

  const sortedSuppliers = computed(() =>
    [...suppliers.value].sort((left, right) => {
      const priorityGap = supplierPriority(right) - supplierPriority(left);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
    })
  );

  const filteredSuppliers = computed(() => {
    if (activeStoreId.value === 'all') {
      return sortedSuppliers.value;
    }

    return sortedSuppliers.value.filter(
      (supplier) => supplier.storeId === activeStoreId.value
    );
  });

  const selectedSupplier = computed(() => {
    const source = selectedSupplierDetail.value ?? null;

    if (source && source.supplierId === selectedSupplierId.value) {
      return source;
    }

    return (
      filteredSuppliers.value.find(
        (supplier) => supplier.supplierId === selectedSupplierId.value
      ) ??
      sortedSuppliers.value.find(
        (supplier) => supplier.supplierId === selectedSupplierId.value
      ) ??
      null
    );
  });

  const selectedStoreName = computed(() => {
    const storeId =
      selectedSupplier.value?.storeId ??
      (activeStoreId.value !== 'all' ? activeStoreId.value : '');

    if (!storeId) {
      return '全部店铺';
    }

    return (
      sortedStores.value.find((store) => store.storeId === storeId)?.shopName ??
      storeId
    );
  });

  const summary = computed(() => ({
    totalSuppliers: suppliers.value.length,
    primarySuppliers: suppliers.value.filter((supplier) => supplier.primary).length,
    backupSuppliers: suppliers.value.filter((supplier) => supplier.backup).length,
    highRiskSuppliers: suppliers.value.filter((supplier) =>
      normalizeText(supplier.riskLevel).includes('high')
    ).length,
    dropshipSuppliers: suppliers.value.filter(
      (supplier) => supplier.dropshipSupportFlag
    ).length,
    blacklistedSuppliers: suppliers.value.filter(
      (supplier) => supplier.blacklistFlag
    ).length
  }));

  function resetSessionViews() {
    latestAdmissionReview.value = null;
    latestScorecard.value = null;
    latestDeliveryAppointment.value = null;
    latestRiskEvent.value = null;
    settlementStatements.value = [];
    srmLinkage.value = null;
    selectedSupplierDetail.value = null;
  }

  function applySelection(preferredSupplierId?: string | null) {
    const nextSupplierId =
      preferredSupplierId &&
      suppliers.value.some((supplier) => supplier.supplierId === preferredSupplierId)
        ? preferredSupplierId
        : filteredSuppliers.value[0]?.supplierId ??
          sortedSuppliers.value[0]?.supplierId ??
          null;

    selectedSupplierId.value = nextSupplierId;
    resetSessionViews();
  }

  async function refreshSelectedContext(token: string) {
    if (!selectedSupplierId.value) {
      resetSessionViews();
      return;
    }

    const fallbackSupplier =
      suppliers.value.find(
        (supplier) => supplier.supplierId === selectedSupplierId.value
      ) ?? null;

    if (!fallbackSupplier) {
      resetSessionViews();
      return;
    }

    const [detailPayload, linkagePayload, statementsPayload] = await Promise.all([
      getSupplier(selectedSupplierId.value, token),
      getSupplierSrmLinkage(selectedSupplierId.value, token),
      getSupplierSettlementStatements(fallbackSupplier.storeId, token)
    ]);

    selectedSupplierDetail.value = detailPayload;
    srmLinkage.value = linkagePayload;
    settlementStatements.value = statementsPayload.filter(
      (statement) => statement.supplierId === selectedSupplierId.value
    );
  }

  async function load(token: string, preferredSupplierId?: string | null) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, suppliersPayload] = await Promise.all([
        getStores(token),
        getSuppliers(token)
      ]);

      stores.value = storesPayload;
      suppliers.value = suppliersPayload;

      if (
        activeStoreId.value !== 'all' &&
        !storesPayload.some((store) => store.storeId === activeStoreId.value)
      ) {
        activeStoreId.value = storesPayload[0]?.storeId ?? 'all';
      }

      if (activeStoreId.value === 'all' && storesPayload.length === 1) {
        activeStoreId.value = storesPayload[0].storeId;
      }

      applySelection(preferredSupplierId ?? selectedSupplierId.value);
      await refreshSelectedContext(token);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '供应商中心加载失败，请稍后重试';
    } finally {
      isLoading.value = false;
    }
  }

  async function selectStore(storeId: string, token: string) {
    activeStoreId.value = storeId;
    applySelection(selectedSupplierId.value);
    await refreshSelectedContext(token);
  }

  async function selectSupplier(supplierId: string, token: string) {
    selectedSupplierId.value = supplierId;
    resetSessionViews();
    await refreshSelectedContext(token);
  }

  function openCreateProfile() {
    profileMode.value = 'create';
    actionError.value = null;
  }

  function openEditProfile() {
    profileMode.value = 'edit';
    actionError.value = null;
  }

  async function runAction(token: string, callback: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;

    try {
      await callback();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '供应商动作执行失败，请稍后重试';
    } finally {
      isRunningAction.value = false;
    }
  }

  async function submitCreateSupplier(payload: CreateSupplierPayload, token: string) {
    await runAction(token, async () => {
      const created = await createSupplier(payload, token);
      actionFeedback.value = `已新增供应商 ${created.supplierName}，正在回读主档与联动摘要。`;
      profileMode.value = 'edit';
      if (activeStoreId.value === 'all' && payload.storeId) {
        activeStoreId.value = payload.storeId;
      }
      await load(token, created.supplierId);
    });
  }

  async function submitUpdateSupplier(payload: UpdateSupplierPayload, token: string) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      const updated = await updateSupplier(selectedSupplierId.value!, payload, token);
      actionFeedback.value = `已更新供应商 ${updated.supplierName} 的主档信息。`;
      await load(token, updated.supplierId);
    });
  }

  async function markPrimarySupplier(token: string) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      const updated = await setPrimarySupplier(selectedSupplierId.value!, token);
      actionFeedback.value = `已将 ${updated.supplierName} 设为主供应商。`;
      await load(token, updated.supplierId);
    });
  }

  async function markBackupSupplier(token: string) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      const updated = await setBackupSupplier(selectedSupplierId.value!, token);
      actionFeedback.value = `已将 ${updated.supplierName} 设为备选供应商。`;
      await load(token, updated.supplierId);
    });
  }

  async function submitAdmissionReview(
    payload: CreateSupplierAdmissionReviewPayload,
    token: string
  ) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      latestAdmissionReview.value = await reviewSupplierAdmission(
        selectedSupplierId.value!,
        payload,
        token
      );
      actionFeedback.value = '准入评审已提交，联动摘要已刷新。';
      await refreshSelectedContext(token);
    });
  }

  async function submitScorecard(
    payload: CreateSupplierScorecardPayload,
    token: string
  ) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      latestScorecard.value = await saveSupplierScorecard(
        selectedSupplierId.value!,
        payload,
        token
      );
      actionFeedback.value = '供应商评分卡已保存。';
      await refreshSelectedContext(token);
    });
  }

  async function submitDeliveryAppointment(
    payload: CreateSupplierDeliveryAppointmentPayload,
    token: string
  ) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      latestDeliveryAppointment.value = await createSupplierDeliveryAppointment(
        selectedSupplierId.value!,
        payload,
        token
      );
      actionFeedback.value = '到货预约已创建。';
      await refreshSelectedContext(token);
    });
  }

  async function submitRiskEvent(
    payload: CreateSupplierRiskEventPayload,
    token: string
  ) {
    if (!selectedSupplierId.value) {
      return;
    }

    await runAction(token, async () => {
      latestRiskEvent.value = await createSupplierRiskEvent(
        selectedSupplierId.value!,
        payload,
        token
      );
      actionFeedback.value = '风险事件已记录。';
      await refreshSelectedContext(token);
    });
  }

  async function submitSettlementStatement(
    payload: CreateSupplierSettlementStatementPayload,
    token: string
  ) {
    await runAction(token, async () => {
      const statement = await createSupplierSettlementStatement(payload, token);
      actionFeedback.value = `已生成 ${statement.statementPeriod} 对账结算单。`;
      await refreshSelectedContext(token);
    });
  }

  return {
    stores: sortedStores,
    suppliers: sortedSuppliers,
    filteredSuppliers,
    activeStoreId,
    selectedSupplierId,
    selectedSupplier,
    selectedSupplierDetail,
    selectedStoreName,
    summary,
    profileMode,
    srmLinkage,
    latestAdmissionReview,
    latestScorecard,
    latestDeliveryAppointment,
    latestRiskEvent,
    settlementStatements,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectStore,
    selectSupplier,
    openCreateProfile,
    openEditProfile,
    submitCreateSupplier,
    submitUpdateSupplier,
    markPrimarySupplier,
    markBackupSupplier,
    submitAdmissionReview,
    submitScorecard,
    submitDeliveryAppointment,
    submitRiskEvent,
    submitSettlementStatement
  };
}
