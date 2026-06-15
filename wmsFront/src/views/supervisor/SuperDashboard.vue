<template>
  <div class="p-6">
    <div class="flex flex-col md:flex-row md:justify-between md:items-start gap-4 mb-6">
      <div>
        <h2 class="app-title text-2xl font-bold">Dashboard</h2>
        <p class="app-subtitle text-sm mt-1">Warehouse activity overview</p>
      </div>
      <div class="flex flex-col sm:flex-row sm:items-center gap-3">
        <span v-if="lastUpdatedLabel" class="app-muted text-xs">Last updated: {{ lastUpdatedLabel }}</span>
        <Button icon="pi pi-refresh" size="small" severity="secondary" outlined :loading="loading" aria-label="Refresh" @click="loadDashboard" />
      </div>
    </div>

    <Message v-if="errorMessage" severity="error" class="mb-6" :closable="false">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <span>{{ errorMessage }}</span>
        <Button label="Retry" icon="pi pi-refresh" size="small" severity="danger" outlined :loading="loading" @click="loadDashboard" />
      </div>
    </Message>

    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
      <DashboardMetricCard
        v-for="card in kpiCards"
        :key="card.key"
        :title="card.title"
        :value="card.value"
        :icon="card.icon"
        :icon-tone="card.iconTone"
        :description="card.description"
        :loading="loading"
        :warning="card.warning"
      />
    </div>

    <Card class="app-card border-none shadow-lg mt-6">
      <template #title>
        <div class="flex items-center justify-between gap-3">
          <span class="app-subtitle">Low Stock Alerts</span>
          <Tag :severity="summary.lowStockAlerts > 0 ? 'warning' : 'success'" :value="lowStockStatusLabel" />
        </div>
      </template>
      <template #content>
        <Message v-if="!loading && !errorMessage && isEmptySummary" severity="info" :closable="false">
          No dashboard activity is available yet. Metrics will appear after warehouse data is recorded.
        </Message>
        <p v-else class="app-muted text-sm">
          Products or stock records below the configured minimum threshold are counted here. Detailed alert rows can be connected when the backend exposes them.
        </p>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

import DashboardMetricCard from '@/components/dashboard/DashboardMetricCard.vue'
import { dashboardService } from '@/services/dashboardService'

const emptySummary = {
  totalInventory: 0,
  openTasks: 0,
  completedTasks: 0,
  lowStockAlerts: 0,
  lastUpdated: null
}

const summary = ref({ ...emptySummary })
const loading = ref(false)
const errorMessage = ref('')

const kpiDefinitions = [
  {
    key: 'totalInventory',
    title: 'Total Inventory',
    description: 'Total stock quantity available in the warehouse.',
    icon: 'pi pi-box',
    iconTone: 'dashboard-metric-card__icon--primary'
  },
  {
    key: 'openTasks',
    title: 'Open Tasks',
    description: 'Created, assigned, or in-progress warehouse tasks.',
    icon: 'pi pi-list',
    iconTone: 'dashboard-metric-card__icon--warning'
  },
  {
    key: 'completedTasks',
    title: 'Completed Tasks',
    description: 'Warehouse tasks completed successfully.',
    icon: 'pi pi-check-circle',
    iconTone: 'dashboard-metric-card__icon--success'
  },
  {
    key: 'lowStockAlerts',
    title: 'Low Stock Alerts',
    description: 'Stock records below the configured minimum threshold.',
    icon: 'pi pi-exclamation-triangle',
    iconTone: 'dashboard-metric-card__icon--danger',
    warning: true
  }
]

const kpiCards = computed(() =>
  kpiDefinitions.map((definition) => ({
    ...definition,
    value: summary.value[definition.key] ?? 0
  }))
)

const isEmptySummary = computed(() =>
  !summary.value.totalInventory &&
  !summary.value.openTasks &&
  !summary.value.completedTasks &&
  !summary.value.lowStockAlerts
)

const lowStockStatusLabel = computed(() => {
  const alerts = summary.value.lowStockAlerts || 0
  return alerts > 0 ? `${alerts} alert${alerts === 1 ? '' : 's'}` : 'No alerts'
})

const lastUpdatedLabel = computed(() => {
  if (!summary.value.lastUpdated) return ''
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(summary.value.lastUpdated))
})

const getErrorMessage = (error) => {
  return error.response?.data?.message || error.response?.data?.error || 'Unable to load dashboard data. Please try again.'
}

const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    summary.value = await dashboardService.getDashboardSummary()
  } catch (error) {
    summary.value = { ...emptySummary }
    errorMessage.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>
