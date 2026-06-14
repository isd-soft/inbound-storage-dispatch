<template>
  <div class="app-shell font-sans">

    <header class="app-header flex justify-between items-center p-4 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-server text-2xl app-brand"></i>
        <h1 class="text-xl font-bold tracking-wide">System Overview</h1>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ThemeToggle />
        <Button label="Supervisor" icon="pi pi-chart-bar" severity="secondary" text @click="router.push('/supervisor')" />
        <Button icon="pi pi-sign-out" label="Logout" severity="danger" text @click="handleLogout" />
      </div>
    </header>

    <main class="p-4 max-w-7xl mx-auto mt-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card class="app-card">
          <template #title><span class="app-subtitle">Backend API</span></template>
          <template #content>
            <Tag severity="success" value="Online"></Tag>
            <p class="app-muted mt-2 text-sm">v1.0.0 | Java 21</p>
          </template>
        </Card>

        <Card class="app-card">
          <template #title><span class="app-subtitle">Database</span></template>
          <template #content>
            <Tag severity="success" value="Connected"></Tag>
            <p class="app-muted mt-2 text-sm">PostgreSQL 16</p>
          </template>
        </Card>

        <Card class="app-card">
          <template #title><span class="app-subtitle">Active Tokens</span></template>
          <template #content>
            <span class="app-brand text-3xl font-bold">12</span>
          </template>
        </Card>
      </div>

          <AppDataTable :value="systemLogs" :filterFields="systemLogFilterFields" class="p-datatable-sm">
            <Column field="timestamp" header="Time" filter></Column>
            <Column field="level" header="Level" filter>
              <template #body="slotProps">
                <Tag :severity="slotProps.data.level === 'ERROR' ? 'danger' : 'warn'" :value="slotProps.data.level"></Tag>
              </template>
            </Column>
            <Column field="message" header="Message" filter></Column>
          </AppDataTable>
    </main>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import Card from 'primevue/card'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import ThemeToggle from '@/components/ThemeToggle.vue'
import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}

const systemLogs = ref([
  { timestamp: '2026-06-05 10:15:00', level: 'WARN', message: 'Token expiration approaching for user: operator' },
  { timestamp: '2026-06-05 09:42:11', level: 'ERROR', message: 'Failed to resolve location Barcode: Z-99' }
])
const systemLogFilterFields = [
  { field: 'timestamp', label: 'Time' },
  { field: 'level', label: 'Level' },
  { field: 'message', label: 'Message' }
]
</script>
