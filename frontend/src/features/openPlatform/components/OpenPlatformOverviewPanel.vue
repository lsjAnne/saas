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
      return 'Missing credential';
    case 'credential_revoked':
      return 'Revoked credential';
    case 'credential_expiring_soon':
      return 'Expiring credential';
    case 'webhook_missing':
      return 'No webhook';
    case 'webhook_disabled':
      return 'Webhook disabled';
    case 'webhook_partial':
      return 'Webhook partial';
    case 'traffic_rejected':
      return 'Rejected traffic';
    case 'app_disabled':
      return 'App disabled';
    default:
      return issue;
  }
}

function topEntries(entries: PluginGovernanceEntryView[] | undefined) {
  return (entries ?? []).slice(0, 3);
}
</script>

<template>
  <PanelCard
    eyebrow="Open Platform"
    title="Integration governance stays visible before release traffic expands."
    description="The dashboard now separates simple asset counts from the actual governance risks that still block a clean open-platform rollout."
    dark
  >
    <div class="open-platform-overview">
      <div class="open-platform-overview__metrics">
        <MetricCard
          label="Apps"
          :value="formatCount(overview?.appCount)"
          caption="Total registered integration apps"
        />
        <MetricCard
          label="Webhooks"
          :value="formatCount(overview?.webhookCount)"
          caption="Tenant callback subscriptions"
        />
        <MetricCard
          label="Call Logs"
          :value="formatCount(audit?.totalCallLogCount)"
          caption="Observed management and external traffic"
        />
      </div>

      <div class="open-platform-overview__metrics">
        <MetricCard
          label="Risky Apps"
          :value="formatCount(governance?.riskyApps)"
          caption="Apps still blocked by governance issues"
        />
        <MetricCard
          label="Missing Credentials"
          :value="formatCount(governance?.appsMissingCredentials)"
          caption="Apps not ready for external authorization"
        />
        <MetricCard
          label="Rejected Traffic"
          :value="formatCount(governance?.appsWithRejectedTraffic)"
          caption="Apps with recent rejected requests"
        />
      </div>

      <section class="open-platform-overview__governance">
        <div class="open-platform-overview__section-head">
          <div>
            <p class="open-platform-overview__label">Governance Actions</p>
            <h3 class="open-platform-overview__section-title">What still needs to be closed before the plugin center is healthy</h3>
          </div>
          <div class="open-platform-overview__status-cluster">
            <StatusPill
              label="active apps"
              :tone="statusTone(governance?.activeApps === governance?.totalApps ? 'active' : 'partial')"
            />
            <StatusPill
              label="webhook gaps"
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
            No immediate governance actions are pending.
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
              <StatusPill :label="entry.governanceRiskLevel" :tone="governanceTone(entry.governanceRiskLevel)" />
            </div>

            <div class="open-platform-overview__entry-facts">
              <p>Credential: {{ entry.credentialStatus }}</p>
              <p>Webhook: {{ entry.webhookStatus }} ({{ entry.enabledWebhookCount }}/{{ entry.organizationWebhookCount }})</p>
              <p>Rejected: {{ entry.rejectedCallCount }}</p>
              <p>Last activity: {{ formatDateTime(entry.lastActivityAt) }}</p>
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
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
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
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.open-platform-overview__section-title,
.open-platform-overview__entry-title {
  color: #fff;
}

.open-platform-overview__recommendation,
.open-platform-overview__entry-meta,
.open-platform-overview__entry-action,
.open-platform-overview__entry-facts p {
  color: rgba(255, 255, 255, 0.78);
}

.open-platform-overview__recommendations {
  display: grid;
  gap: 0.55rem;
}

.open-platform-overview__recommendation {
  padding: 0.8rem 0.9rem;
  border-radius: 0.9rem;
  background: rgba(255, 255, 255, 0.08);
}

.open-platform-overview__recommendation--quiet {
  color: rgba(255, 255, 255, 0.56);
}

.open-platform-overview__entry {
  display: grid;
  gap: 0.75rem;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.06);
}

.open-platform-overview__entry-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
}

.open-platform-overview__issue {
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
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
