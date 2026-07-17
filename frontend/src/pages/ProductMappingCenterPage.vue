<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue';

import InlineErrorCard from '@/components/feedback/InlineErrorCard.vue';
import ProductMappingCatalogPanel from '@/features/productMappingCenter/components/ProductMappingCatalogPanel.vue';
import ProductMappingCenterHeroPanel from '@/features/productMappingCenter/components/ProductMappingCenterHeroPanel.vue';
import ProductMappingValidationPanel from '@/features/productMappingCenter/components/ProductMappingValidationPanel.vue';
import ProductMappingWorkspacePanel from '@/features/productMappingCenter/components/ProductMappingWorkspacePanel.vue';
import { useProductMappingCenterOverview } from '@/features/productMappingCenter/composables/useProductMappingCenterOverview';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();
const mappingCenter = useProductMappingCenterOverview();
const workspaceAnchor = shallowRef<HTMLElement | null>(null);

authStore.hydrate();

const selectedStoreName = computed(
  () => mappingCenter.selectedStore.value?.shopName ?? '待选择商品'
);

async function bootstrapPage() {
  if (!authStore.token) {
    return;
  }

  await mappingCenter.load(authStore.token);
}

function scrollToWorkspace() {
  workspaceAnchor.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  });
}

async function handleCreatePrimary(supplierId: string) {
  if (!authStore.token) {
    return;
  }

  await mappingCenter.createMapping('primary', supplierId, authStore.token);
}

async function handleSwitchPrimary(supplierId: string) {
  if (!authStore.token) {
    return;
  }

  await mappingCenter.switchPrimarySupplier(supplierId, authStore.token);
}

async function handleCreateBackup(supplierId: string) {
  if (!authStore.token) {
    return;
  }

  await mappingCenter.createBackupSupplier(supplierId, authStore.token);
}

onMounted(() => {
  bootstrapPage();
});
</script>

<template>
  <section class="product-mapping-page">
    <header class="product-mapping-page__header">
      <p class="product-mapping-page__eyebrow">Product Mapping Center</p>
      <h2 class="product-mapping-page__title">
        让平台商品、上游货源与校验结论在同一页完成建立、切换和兜底
      </h2>
      <p class="product-mapping-page__subtitle">
        这页严格按需求文档的商品与货源映射原型收口：左侧先挑平台商品，右侧马上完成主货源落位、备用补齐、映射回看和风险复核。
      </p>
    </header>

    <InlineErrorCard
      v-if="mappingCenter.errorMessage"
      :message="mappingCenter.errorMessage"
      @retry="bootstrapPage"
    />

    <ProductMappingCenterHeroPanel
      :selected-product="mappingCenter.selectedProduct"
      :selected-store-name="selectedStoreName"
      :summary="mappingCenter.summary"
      :active-primary-supplier="mappingCenter.activePrimarySupplier"
      :backup-suppliers="mappingCenter.backupSuppliers"
      :action-feedback="mappingCenter.actionFeedback"
      @open-primary="scrollToWorkspace"
      @open-backup="scrollToWorkspace"
    />

    <div class="product-mapping-page__grid">
      <ProductMappingCatalogPanel
        :products="mappingCenter.catalogProducts"
        :selected-product-id="mappingCenter.selectedProductId"
        @select="mappingCenter.selectProduct"
      />

      <div ref="workspaceAnchor" class="product-mapping-page__stack">
        <ProductMappingWorkspacePanel
          :selected-product="mappingCenter.selectedProduct"
          :selected-store-name="selectedStoreName"
          :suppliers="mappingCenter.selectedStoreSuppliers"
          :active-primary-supplier="mappingCenter.activePrimarySupplier"
          :backup-suppliers="mappingCenter.backupSuppliers"
          :sku-rows="mappingCenter.skuRows"
          :is-submitting="mappingCenter.isRunningAction"
          :action-error="mappingCenter.actionError"
          @create-primary="handleCreatePrimary"
          @switch-primary="handleSwitchPrimary"
          @create-backup="handleCreateBackup"
        />

        <ProductMappingValidationPanel :items="mappingCenter.validationItems" />
      </div>
    </div>

    <p v-if="mappingCenter.isLoading" class="product-mapping-page__footer-note">
      正在回读平台商品、店铺货源池与当前映射状态...
    </p>
  </section>
</template>

<style scoped>
.product-mapping-page {
  display: grid;
  gap: 1.2rem;
}

.product-mapping-page__header {
  display: grid;
  gap: 0.8rem;
}

.product-mapping-page__eyebrow,
.product-mapping-page__title,
.product-mapping-page__subtitle,
.product-mapping-page__footer-note {
  margin: 0;
}

.product-mapping-page__eyebrow {
  color: var(--color-ink-faint);
  font-size: 0.76rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.product-mapping-page__title {
  max-width: 14ch;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  line-height: 1.04;
}

.product-mapping-page__subtitle,
.product-mapping-page__footer-note {
  max-width: 64rem;
  color: var(--color-ink-soft);
  line-height: 1.75;
}

.product-mapping-page__grid {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 1rem;
  align-items: start;
}

.product-mapping-page__stack {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1180px) {
  .product-mapping-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
