<script setup lang="ts">
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type { MemberTagSummaryView, MemberView, Store } from '@/services/apiTypes';
import { formatCurrency, formatDate, formatCount } from '@/utils/formatters';

interface Props {
  stores: Store[];
  members: MemberView[];
  selectedMemberId: string | null;
  activeStoreId: string;
  levelFilter: string;
  lifecycleFilter: string;
  tagFilter: string;
  availableLevels: string[];
  availableLifecycleStages: string[];
  memberTags: MemberTagSummaryView[];
}

defineProps<Props>();

defineEmits<{
  selectStore: [storeId: string];
  selectLevel: [levelCode: string];
  selectLifecycle: [stage: string];
  selectTag: [tagCode: string];
  selectMember: [memberId: string];
}>();

function resolveTone(member: MemberView) {
  const lifecycle = member.lifecycleStage.toLowerCase();

  if (lifecycle === 'dormant' || lifecycle === 'silent') {
    return 'warn';
  }

  if (['vip', 'svip'].includes(member.levelCode.toLowerCase())) {
    return 'success';
  }

  return 'neutral';
}

const labelMap: Record<string, string> = {
  normal: '普通会员',
  silver: '白银会员',
  gold: '黄金会员',
  platinum: '铂金会员',
  vip: 'VIP',
  svip: '超级VIP',
  new: '新客',
  active: '活跃',
  dormant: '沉默',
  silent: '静默',
  churn_risk: '流失风险'
};

function labelOf(value: string) {
  return labelMap[value] ?? value;
}
</script>

<template>
  <PanelCard
    title="会员池与筛选上下文"
    description="店铺、层级、生命周期和标签筛选都先在这里确定，右侧所有 CRM 动作跟随当前池子变化。"
  >
    <div class="member-pool">
      <div class="member-pool__filters">
        <div class="member-pool__store-switcher">
          <button
            type="button"
            :class="['member-pool__store-chip', { 'member-pool__store-chip--active': activeStoreId === 'all' }]"
            @click="$emit('selectStore', 'all')"
          >
            全部店铺
          </button>
          <button
            v-for="store in stores"
            :key="store.storeId"
            type="button"
            :class="['member-pool__store-chip', { 'member-pool__store-chip--active': activeStoreId === store.storeId }]"
            @click="$emit('selectStore', store.storeId)"
          >
            {{ store.shopName }}
          </button>
        </div>

        <div class="member-pool__select-grid">
          <label class="member-pool__field">
            <span>会员层级</span>
            <select :value="levelFilter" @change="$emit('selectLevel', ($event.target as HTMLSelectElement).value)">
              <option value="all">全部</option>
              <option v-for="level in availableLevels" :key="level" :value="level">
                {{ labelOf(level) }}
              </option>
            </select>
          </label>

          <label class="member-pool__field">
            <span>生命周期</span>
            <select
              :value="lifecycleFilter"
              @change="$emit('selectLifecycle', ($event.target as HTMLSelectElement).value)"
            >
              <option value="all">全部</option>
              <option v-for="stage in availableLifecycleStages" :key="stage" :value="stage">
                {{ labelOf(stage) }}
              </option>
            </select>
          </label>

          <label class="member-pool__field">
            <span>标签</span>
            <select :value="tagFilter" @change="$emit('selectTag', ($event.target as HTMLSelectElement).value)">
              <option value="all">全部</option>
              <option v-for="tag in memberTags" :key="tag.tagCode" :value="tag.tagCode">
                {{ tag.tagName }} / {{ formatCount(tag.memberCount) }}
              </option>
            </select>
          </label>
        </div>
      </div>

      <div v-if="members.length" class="member-pool__list">
        <article
          v-for="member in members"
          :key="member.memberId"
          :class="['member-pool__item', { 'member-pool__item--active': selectedMemberId === member.memberId }]"
          @click="$emit('selectMember', member.memberId)"
        >
          <div class="member-pool__item-head">
            <div>
              <h4 class="member-pool__item-title">{{ member.nickname || member.customerId }}</h4>
              <p class="member-pool__item-meta">
                {{ labelOf(member.levelCode) }} / {{ labelOf(member.lifecycleStage) }}
              </p>
            </div>
            <StatusPill :label="labelOf(member.levelCode)" :tone="resolveTone(member)" />
          </div>

          <div class="member-pool__chips">
            <span>{{ formatCurrency(member.totalPaidAmount) }}</span>
            <span>{{ formatCount(member.totalOrderCount, ' 单') }}</span>
            <span>{{ formatCount(member.points, ' 积分') }}</span>
          </div>

          <div class="member-pool__chips">
            <span v-for="tag in member.tags.slice(0, 3)" :key="tag.memberTagId">
              {{ tag.tagName }}
            </span>
          </div>

          <p class="member-pool__item-date">最近下单 {{ formatDate(member.lastOrderAt) }}</p>
        </article>
      </div>

      <div v-else class="member-pool__empty">
        当前筛选下没有会员，先切换筛选口径或等待新的订单沉淀会员资料。
      </div>
    </div>
  </PanelCard>
</template>

<style scoped>
.member-pool,
.member-pool__list,
.member-pool__filters {
  display: grid;
  gap: 0.9rem;
}

.member-pool__store-switcher,
.member-pool__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.member-pool__select-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.member-pool__field {
  display: grid;
  gap: 0.45rem;
}

.member-pool__field span {
  color: var(--color-ink-soft);
  font-size: 0.82rem;
}

.member-pool__field select {
  min-height: 2.8rem;
  padding: 0 0.9rem;
  border-radius: 1rem;
  border: 1px solid var(--color-line);
  background: #ffffff;
  color: var(--color-ink-strong);
}

.member-pool__store-chip {
  min-height: 2.2rem;
  padding: 0 0.95rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-line);
  background: var(--color-panel-strong);
  color: var(--color-ink);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.member-pool__store-chip--active {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #ffffff;
}

.member-pool__item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
  cursor: pointer;
}

.member-pool__item--active {
  border-color: var(--color-accent);
  box-shadow: inset 0 0 0 1px var(--color-accent);
  background: var(--color-accent-soft);
}

.member-pool__item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.member-pool__item-title,
.member-pool__item-meta,
.member-pool__item-date {
  margin: 0;
}

.member-pool__item-title {
  color: var(--color-ink-strong);
  font-size: 1rem;
}

.member-pool__item-meta,
.member-pool__item-date {
  color: var(--color-ink-soft);
  line-height: 1.6;
}

.member-pool__chips span {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.8rem;
}

.member-pool__empty {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  color: var(--color-ink-soft);
  line-height: 1.7;
}

@media (max-width: 920px) {
  .member-pool__select-grid {
    grid-template-columns: 1fr;
  }
}
</style>
