<script setup lang="ts">
import MetricCard from '@/components/cards/MetricCard.vue';
import PanelCard from '@/components/cards/PanelCard.vue';
import StatusPill from '@/components/feedback/StatusPill.vue';
import type {
  IntegrationAuditOverviewView,
  OpenPlatformOverviewView,
  PluginGovernanceEntryView,
  PluginGovernanceOverviewView
} from '@/services/apiTypes';
import { formatCount, formatDateTime } from '@/utils/formatters';

interface Props {
  overview: OpenPlatformOverviewView | null;
  governance: PluginGovernanceOverviewView | null;
  audit: IntegrationAuditOverviewView | null;
}

defineProps<Props>();

function governanceTone(level: string) {
  if (level === 'high') {
    return 'warn';
  }
  if (level === 'medium') {
    return 'neutral';
  }
  return 'success';
}

function statusTone(status: string) {
  return status === 'active' || status === 'ready' ? 'success' : 'neutral';
}

function issueLabel(issue: string) {
  switch (issue) {
    case 'credential_missing':
      return '缺少凭证';
    case 'credential_revoked':
      return '凭证已吊销';
    case 'credential_expiring_soon':
      return '凭证即将过期';
    case 'webhook_missing':
      return '未配置回调';
    case 'webhook_disabled':
      return '回调已停用';
    case 'webhook_partial':
      return '回调不完整';
    case 'traffic_rejected':
      return '请求被拒';
    case 'app_disabled':
      return '应用已停用';
    default:
      return issue;
  }
}

const riskLevelMap: Record<string, string> = {
  high: '高风险',
  medium: '中风险',
  low: '低风险'
};

function topEntries(entries: PluginGovernanceEntryView[] | undefined) {
  return (entries ?? []).slice(0, 3);
}
</script>

<template>
  <PanelCard
    title="开放平台概览"
    description="应用、凭证、回调与被拒流量的治理风险一览。"
  >
    <div class="open-platform-overview">
      <div class="open-platform-overview__metrics">
        <MetricCard
          label="应用数"
          :value="formatCount(overview?.appCount)"
          caption="已注册集成应用总数"
        />
        <MetricCard
          label="回调数"
          :value="formatCount(overview?.webhookCount)"
          caption="租户回调订阅数"
        />
        <MetricCard
          label="调用日志"
          :value="formatCount(audit?.totalCallLogCount)"
          caption="观测到的管理与外部流量"
        />
      </div>

      <div class="open-platform-overview__metrics">
        <MetricCard
          label="风险应用"
          :value="formatCount(governance?.riskyApps)"
          caption="仍被治理问题阻断的应用"
        />
        <MetricCard
          label="缺凭证应用"
          :value="formatCount(governance?.appsMissingCredentials)"
          caption="未就绪可外部授权的应用"
        />
        <MetricCard
          label="被拒流量"
          :value="formatCount(governance?.appsWithRejectedTraffic)"
          caption="近期存在被拒请求的应用"
        />
      </div>

      <section class="open-platform-overview__governance">
        <div class="open-platform-overview__section-head">
          <div>
            <p class="open-platform-overview__label">治理待办</p>
            <h3 class="open-platform-overview__section-title">插件中心健康前仍需闭环的事项</h3>
          </div>
          <div class="open-platform-overview__status-cluster">
            <StatusPill
              label="活跃应用"
              :tone="statusTone(governance?.activeApps === governance?.totalApps ? 'active' : 'partial')"
            />
            <StatusPill
              label="回调缺口"
              :tone="statusTone((governance?.organizationsWithoutWebhooks ?? 0) === 0 ? 'ready' : 'partial')"
            />
          </div>
        </div>

        <div class="open-platform-overview__recommendations">
          <p
            v-for="action in governance?.recommendedActions ?? []"
            :key="action"
            class="open-platform-overview__recommendation"
          >
            {{ action }}
          </p>
          <p
            v-if="!(governance?.recommendedActions?.length)"
            class="open-platform-overview__recommendation open-platform-overview__recommendation--quiet"
          >
            暂无待处理的治理事项。
          </p>
        </div>

        <div class="open-platform-overview__entries">
          <article
            v-for="entry in topEntries(governance?.entries)"
            :key="entry.appId"
            class="open-platform-overview__entry"
          >
            <div class="open-platform-overview__entry-head">
              <div>
                <h4 class="open-platform-overview__entry-title">{{ entry.appName }}</h4>
                <p class="open-platform-overview__entry-meta">
                  {{ entry.appType }} · {{ entry.permissionScope.join(', ') }}
                </p>
              </div>
              <StatusPill :label="riskLevelMap[entry.governanceRiskLevel] || entry.governanceRiskLevel" :tone="governanceTone(entry.governanceRiskLevel)" />
            </div>

            <div class="open-platform-overview__entry-facts">
              <p>凭证：{{ entry.credentialStatus }}</p>
              <p>回调：{{ entry.webhookStatus }} ({{ entry.enabledWebhookCount }}/{{ entry.organizationWebhookCount }})</p>
              <p>被拒：{{ entry.rejectedCallCount }}</p>
              <p>最近活动：{{ formatDateTime(entry.lastActivityAt) }}</p>
            </div>

            <div class="open-platform-overview__issue-list">
              <span
                v-for="issue in entry.issues"
                :key="issue"
                class="open-platform-overview__issue"
              >
                {{ issueLabel(issue) }}
              </span>
            </div>

            <p class="open-platform-overview__entry-action">{{ entry.recommendedAction }}</p>
          </article>
        </div>
      </section>
    </div>
  </PanelCard>
</template>

<style scoped>
.open-platform-overview {
  display: grid;
  gap: 1rem;
}

.open-platform-overview__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.open-platform-overview__governance,
.open-platform-overview__entries {
  display: grid;
  gap: 0.9rem;
}

.open-platform-overview__governance {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.open-platform-overview__section-head,
.open-platform-overview__entry-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: flex-start;
}

.open-platform-overview__status-cluster,
.open-platform-overview__issue-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.open-platform-overview__label,
.open-platform-overview__section-title,
.open-platform-overview__recommendation,
.open-platform-overview__entry-title,
.open-platform-overview__entry-meta,
.open-platform-overview__entry-action,
.open-platform-overview__entry-facts p {
  margin: 0;
}

.open-platform-overview__label {
  color: var(--color-accent);
  font-size: 0.78rem;
  font-weight: 600;
}

.open-platform-overview__section-title,
.open-platform-overview__entry-title {
  color: var(--color-ink-strong);
}

.open-platform-overview__recommendation,
.open-platform-overview__entry-meta,
.open-platform-overview__entry-action,
.open-platform-overview__entry-facts p {
  color: var(--color-ink-soft);
}

.open-platform-overview__recommendations {
  display: grid;
  gap: 0.55rem;
}

.open-platform-overview__recommendation {
  padding: 0.8rem 0.9rem;
  border-radius: 0.9rem;
  background: var(--color-surface);
}

.open-platform-overview__recommendation--quiet {
  color: var(--color-ink-faint);
}

.open-platform-overview__entry {
  display: grid;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-line);
  background: var(--color-surface);
}

.open-platform-overview__entry-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
}

.open-platform-overview__issue {
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-size: 0.82rem;
}

@media (max-width: 960px) {
  .open-platform-overview__metrics,
  .open-platform-overview__entry-facts {
    grid-template-columns: 1fr;
  }

  .open-platform-overview__section-head,
  .open-platform-overview__entry-head {
    flex-direction: column;
  }
}
</style>
