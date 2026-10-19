<script setup lang="ts">
import type { DigitalHumanOverview } from '@/features/digitalHuman/types';

defineProps<{
  overview: DigitalHumanOverview | null;
}>();
</script>

<template>
  <section class="dh-panel dh-hero">
    <div class="dh-hero__head">
      <div>
        <h2 class="dh-hero__title">数字人无人直播概览</h2>
        <p class="dh-hero__subline">
          形象、话术与无人排班一屏掌握，开播会话与成交数据实时回读。
        </p>
      </div>
      <span class="dh-hero__badge">mock 模式可跑通闭环</span>
    </div>

    <div class="dh-hero__grid">
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">数字人形象</span>
        <span class="dh-hero__metric-value">{{ overview?.avatarCount ?? 0 }}</span>
        <span class="dh-hero__metric-note">可用的数字人主播资产</span>
      </div>
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">话术剧本</span>
        <span class="dh-hero__metric-value">{{ overview?.scriptCount ?? 0 }}</span>
        <span class="dh-hero__metric-note">商品讲解与直播引导话术</span>
      </div>
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">无人排班</span>
        <span class="dh-hero__metric-value">{{ overview?.scheduleCount ?? 0 }}</span>
        <span class="dh-hero__metric-note">7×24 无人值守计划</span>
      </div>
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">在播会话</span>
        <span class="dh-hero__metric-value">{{ overview?.liveSessionCount ?? 0 }}</span>
        <span class="dh-hero__metric-note">当前正在自动开播的场次</span>
      </div>
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">今日成交</span>
        <span class="dh-hero__metric-value">¥{{ overview?.todayGmv ?? 0 }}</span>
        <span class="dh-hero__metric-note">无人直播带来的成交额</span>
      </div>
      <div class="dh-hero__metric">
        <span class="dh-hero__metric-label">今日订单</span>
        <span class="dh-hero__metric-value">{{ overview?.todayOrders ?? 0 }}单</span>
        <span class="dh-hero__metric-note">无人直播产生的订单</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.dh-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
}
.dh-hero {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.dh-hero__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.dh-hero__title {
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--color-ink-strong);
  margin: 0 0 6px;
}
.dh-hero__subline {
  font-size: 0.875rem;
  color: var(--color-ink-muted);
  margin: 0;
}
.dh-hero__badge {
  flex: none;
  font-size: 0.75rem;
  color: var(--color-accent);
  background: var(--color-accent-soft);
  border-radius: 999px;
  padding: 4px 12px;
}
.dh-hero__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.dh-hero__metric {
  background: var(--color-panel-strong);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dh-hero__metric-label {
  font-size: 0.8125rem;
  color: var(--color-ink-muted);
}
.dh-hero__metric-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-ink-strong);
}
.dh-hero__metric-note {
  font-size: 0.75rem;
  color: var(--color-ink-soft);
}
</style>
