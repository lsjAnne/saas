import { computed, shallowRef } from 'vue';

import {
  createProductMapping,
  getProductMappingCatalog,
  getProductMappings,
  switchProductMappingSupplier
} from '@/services/productMappingCenterService';
import { getStores } from '@/services/storeChannelService';
import { getSuppliers } from '@/services/supplierCenterService';
import type {
  ProductMappingCatalogEntry,
  ProductSourceMapping,
  Store,
  Supplier
} from '@/services/apiTypes';

export interface ProductMappingCatalogItem extends ProductMappingCatalogEntry {
  validationTone: 'neutral' | 'warn' | 'success';
}

export interface ProductMappingSkuRow {
  key: string;
  platformSku: string;
  sourceSku: string;
  supplierName: string;
  mappingTypeLabel: string;
  statusLabel: string;
  tone: 'neutral' | 'warn' | 'success';
}

export interface ProductMappingValidationItem {
  code: string;
  title: string;
  detail: string;
  tone: 'neutral' | 'warn' | 'success';
}

function normalizeText(value: string | null | undefined) {
  return (value ?? '').trim().toLowerCase();
}

function toTimestamp(value: string | null | undefined) {
  return value ? new Date(value).getTime() : 0;
}

function isSupplierRisky(supplier: Supplier | null | undefined) {
  if (!supplier) {
    return false;
  }

  return supplier.blacklistFlag || normalizeText(supplier.riskLevel).includes('high');
}

function supplierPriority(supplier: Supplier) {
  if (supplier.blacklistFlag) {
    return -3;
  }

  if (normalizeText(supplier.riskLevel).includes('high')) {
    return -2;
  }

  if (supplier.primary) {
    return 3;
  }

  if (supplier.backup) {
    return 2;
  }

  if (supplier.dropshipSupportFlag) {
    return 1;
  }

  return 0;
}

export function useProductMappingCenterOverview() {
  const stores = shallowRef<Store[]>([]);
  const catalogEntries = shallowRef<ProductMappingCatalogEntry[]>([]);
  const suppliers = shallowRef<Supplier[]>([]);
  const mappings = shallowRef<ProductSourceMapping[]>([]);
  const selectedProductId = shallowRef<string | null>(null);
  const isLoading = shallowRef(false);
  const isRunningAction = shallowRef(false);
  const errorMessage = shallowRef<string | null>(null);
  const actionError = shallowRef<string | null>(null);
  const actionFeedback = shallowRef<string | null>(null);

  const storeMap = computed(() => new Map(stores.value.map((store) => [store.storeId, store])));

  const supplierMap = computed(
    () => new Map(suppliers.value.map((supplier) => [supplier.supplierId, supplier]))
  );

  const catalogProducts = computed<ProductMappingCatalogItem[]>(() =>
    catalogEntries.value
      .map((product) => {
        const productMappings = mappings.value.filter(
          (mapping) => mapping.productId === product.productId
        );
        const hasRisk = productMappings.some((mapping) => {
          const supplier = supplierMap.value.get(mapping.supplierId);
          return mapping.active && (mapping.riskFlag || isSupplierRisky(supplier));
        });

        return {
          ...product,
          title: product.productMissing ? '待补齐商品主数据' : product.title,
          validationTone:
            product.productMissing ||
            product.activePrimaryCount === 0 ||
            hasRisk ||
            product.activeBackupCount === 0
              ? 'warn'
              : 'success'
        };
      })
      .sort((left, right) => {
        const toneWeight =
          Number(right.validationTone === 'warn') - Number(left.validationTone === 'warn');
        if (toneWeight !== 0) {
          return toneWeight;
        }

        const mappingGap = right.mappingCount - left.mappingCount;
        if (mappingGap !== 0) {
          return mappingGap;
        }

        return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
      })
  );

  const selectedProduct = computed<ProductMappingCatalogItem | null>(
    () =>
      catalogProducts.value.find((product) => product.productId === selectedProductId.value) ??
      null
  );

  const selectedStore = computed(
    () => (selectedProduct.value ? storeMap.value.get(selectedProduct.value.storeId) : null) ?? null
  );

  const selectedStoreSuppliers = computed(() =>
    suppliers.value
      .filter((supplier) => supplier.storeId === selectedProduct.value?.storeId)
      .sort((left, right) => {
        const priorityGap = supplierPriority(right) - supplierPriority(left);
        if (priorityGap !== 0) {
          return priorityGap;
        }

        return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
      })
  );

  const selectedProductMappings = computed(() =>
    mappings.value
      .filter((mapping) => mapping.productId === selectedProduct.value?.productId)
      .sort((left, right) => {
        if (left.mappingType !== right.mappingType) {
          return left.mappingType === 'primary' ? -1 : 1;
        }

        if (left.active !== right.active) {
          return Number(right.active) - Number(left.active);
        }

        return toTimestamp(right.createdAt) - toTimestamp(left.createdAt);
      })
  );

  const activePrimaryMapping = computed(
    () =>
      selectedProductMappings.value.find(
        (mapping) => mapping.mappingType === 'primary' && mapping.active
      ) ?? null
  );

  const backupMappings = computed(() =>
    selectedProductMappings.value.filter((mapping) => mapping.mappingType === 'backup')
  );

  const activeBackupMappings = computed(() =>
    backupMappings.value.filter((mapping) => mapping.active)
  );

  const activePrimarySupplier = computed(
    () =>
      (activePrimaryMapping.value
        ? supplierMap.value.get(activePrimaryMapping.value.supplierId)
        : null) ?? null
  );

  const backupSuppliers = computed(() =>
    activeBackupMappings.value
      .map((mapping) => supplierMap.value.get(mapping.supplierId) ?? null)
      .filter((supplier): supplier is Supplier => supplier !== null)
  );

  const skuRows = computed<ProductMappingSkuRow[]>(() =>
    selectedProductMappings.value.map((mapping) => {
      const supplier = supplierMap.value.get(mapping.supplierId);
      const hasRisk = mapping.riskFlag || isSupplierRisky(supplier);

      return {
        key: mapping.productMappingId,
        platformSku:
          selectedProduct.value?.platformProductId ??
          selectedProduct.value?.productId ??
          `product-${mapping.productId}`,
        sourceSku: supplier?.supplierPlatformId ?? mapping.supplierId,
        supplierName: supplier?.supplierName ?? mapping.supplierId,
        mappingTypeLabel: mapping.mappingType === 'primary' ? '主映射' : '备用映射',
        statusLabel: !mapping.active ? '已停用' : hasRisk ? '需复核' : '已连通',
        tone: !mapping.active ? 'neutral' : hasRisk ? 'warn' : 'success'
      };
    })
  );

  const validationItems = computed<ProductMappingValidationItem[]>(() => {
    if (!selectedProduct.value) {
      return [];
    }

    const items: ProductMappingValidationItem[] = [];

    if (selectedProduct.value.productMissing) {
      items.push({
        code: 'missing-product-record',
        title: '商品主数据缺失',
        detail:
          '当前映射记录仍然存在，但商品主数据已经缺失或不可读。请先回到商品中心补齐商品主数据，再继续维护主备货源。',
        tone: 'warn'
      });
    }

    if (!activePrimaryMapping.value) {
      items.push({
        code: 'missing-primary',
        title: '主货源未建立',
        detail:
          '当前商品还没有可用主映射，后续下单与履约链路会缺少主供应入口。',
        tone: 'warn'
      });
    } else {
      items.push({
        code: 'primary-ready',
        title: '主货源已落位',
        detail: `当前主货源为 ${activePrimarySupplier.value?.supplierName ?? activePrimaryMapping.value.supplierId}。`,
        tone:
          activePrimaryMapping.value.riskFlag || isSupplierRisky(activePrimarySupplier.value)
            ? 'warn'
            : 'success'
      });
    }

    if (!activeBackupMappings.value.length) {
      items.push({
        code: 'missing-backup',
        title: '备用货源不足',
        detail:
          '建议至少配置一条备用映射，避免主供应商波动时整条商品链路失去兜底。',
        tone: 'warn'
      });
    } else {
      items.push({
        code: 'backup-ready',
        title: '备用货源已配置',
        detail: `当前已激活 ${activeBackupMappings.value.length} 条备用映射。`,
        tone: 'success'
      });
    }

    const riskySuppliers = selectedProductMappings.value
      .map((mapping) => supplierMap.value.get(mapping.supplierId) ?? null)
      .filter((supplier): supplier is Supplier => supplier !== null)
      .filter((supplier) => isSupplierRisky(supplier));

    if (riskySuppliers.length) {
      items.push({
        code: 'supplier-risk',
        title: '候选货源存在高风险',
        detail: `请优先复核 ${riskySuppliers.map((supplier) => supplier.supplierName).join('、')} 的风险等级或黑名单状态。`,
        tone: 'warn'
      });
    }

    if (!selectedStoreSuppliers.value.length) {
      items.push({
        code: 'no-supplier-pool',
        title: '当前店铺还没有货源池',
        detail:
          '需要先在供应商中心补齐店铺货源，映射页才能进入可执行闭环。',
        tone: 'warn'
      });
    }

    return items;
  });

  const summary = computed(() => {
    const riskyMappingCount = mappings.value.filter((mapping) => {
      const supplier = supplierMap.value.get(mapping.supplierId);
      return mapping.active && (mapping.riskFlag || isSupplierRisky(supplier));
    }).length;

    return {
      totalProducts: catalogProducts.value.length,
      mappedProducts: catalogProducts.value.filter((product) => product.mappingCount > 0).length,
      primaryGapProducts: catalogProducts.value.filter(
        (product) => product.productMissing || product.activePrimaryCount === 0
      ).length,
      backupCoveredProducts: catalogProducts.value.filter(
        (product) => !product.productMissing && product.activeBackupCount > 0
      ).length,
      riskyMappings: riskyMappingCount
    };
  });

  function selectProduct(productId: string) {
    selectedProductId.value = productId;
    actionError.value = null;
  }

  function resolveNextSelectedProduct(preferredProductId?: string) {
    if (
      preferredProductId &&
      catalogProducts.value.some((product) => product.productId === preferredProductId)
    ) {
      return preferredProductId;
    }

    if (
      selectedProductId.value &&
      catalogProducts.value.some((product) => product.productId === selectedProductId.value)
    ) {
      return selectedProductId.value;
    }

    return catalogProducts.value[0]?.productId ?? null;
  }

  async function load(token: string, preferredProductId?: string) {
    isLoading.value = true;
    errorMessage.value = null;

    try {
      const [storesPayload, suppliersPayload, catalogPayload, mappingsPayload] =
        await Promise.all([
          getStores(token),
          getSuppliers(token),
          getProductMappingCatalog(token),
          getProductMappings(token)
        ]);

      stores.value = storesPayload;
      suppliers.value = suppliersPayload;
      catalogEntries.value = catalogPayload;
      mappings.value = mappingsPayload;
      selectedProductId.value = resolveNextSelectedProduct(preferredProductId);
    } catch (error) {
      errorMessage.value =
        error instanceof Error ? error.message : '商品映射中心加载失败，请稍后重试。';
      throw error;
    } finally {
      isLoading.value = false;
    }
  }

  async function runAction(token: string, executor: () => Promise<void>) {
    isRunningAction.value = true;
    actionError.value = null;
    actionFeedback.value = null;

    try {
      await executor();
    } catch (error) {
      actionError.value =
        error instanceof Error ? error.message : '映射操作执行失败，请稍后重试。';
      throw error;
    } finally {
      isRunningAction.value = false;
    }
  }

  async function ensureSelectedProduct() {
    if (!selectedProduct.value) {
      throw new Error('请先选择需要操作的商品。');
    }

    if (selectedProduct.value.productMissing) {
      throw new Error(
        '当前映射只剩货源关系，商品主数据已缺失。请先补齐商品主数据后再继续维护主备货源。'
      );
    }

    return selectedProduct.value;
  }

  async function createMapping(
    mappingType: 'primary' | 'backup',
    supplierId: string,
    token: string
  ) {
    const product = await ensureSelectedProduct();

    await runAction(token, async () => {
      await createProductMapping(
        {
          storeId: product.storeId,
          productId: product.productId,
          supplierId,
          mappingType
        },
        token
      );

      actionFeedback.value =
        mappingType === 'primary'
          ? '已建立主货源映射，并回读最新校验结果。'
          : '已补充备用货源，并回读最新映射状态。';
      await load(token, product.productId);
    });
  }

  async function switchPrimarySupplier(supplierId: string, token: string) {
    const product = await ensureSelectedProduct();

    if (!activePrimaryMapping.value) {
      await createMapping('primary', supplierId, token);
      return;
    }

    await runAction(token, async () => {
      await switchProductMappingSupplier(
        activePrimaryMapping.value.productMappingId,
        {
          supplierId
        },
        token
      );

      actionFeedback.value = '已切换主货源，并完成当前映射回读。';
      await load(token, product.productId);
    });
  }

  async function createBackupSupplier(supplierId: string, token: string) {
    if (backupMappings.value.some((mapping) => mapping.supplierId === supplierId)) {
      actionFeedback.value = '该供应商已经在备用货源列表中，无需重复添加。';
      return;
    }

    await createMapping('backup', supplierId, token);
  }

  return {
    stores,
    selectedProductId,
    selectedProduct,
    selectedStore,
    selectedStoreSuppliers,
    selectedProductMappings,
    activePrimarySupplier,
    backupSuppliers,
    skuRows,
    validationItems,
    summary,
    catalogProducts,
    isLoading,
    isRunningAction,
    errorMessage,
    actionError,
    actionFeedback,
    load,
    selectProduct,
    createMapping,
    switchPrimarySupplier,
    createBackupSupplier
  };
}
