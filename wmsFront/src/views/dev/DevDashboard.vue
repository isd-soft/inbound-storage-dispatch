<template>
  <div class="min-h-screen bg-gray-900 text-gray-100 font-sans">

    <header class="flex justify-between items-center p-4 bg-gray-800 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-server text-2xl text-blue-400"></i>
        <h1 class="text-xl font-bold tracking-wide">System Overview</h1>
      </div>
      <Button icon="pi pi-sign-out" label="Logout" severity="danger" text @click="handleLogout" />
    </header>

    <main class="p-4 max-w-7xl mx-auto mt-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Backend API</span></template>
          <template #content>
            <Tag severity="success" value="Online"></Tag>
            <p class="mt-2 text-sm text-gray-500">v1.0.0 | Java 21</p>
          </template>
        </Card>

        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Database</span></template>
          <template #content>
            <Tag severity="success" value="Connected"></Tag>
            <p class="mt-2 text-sm text-gray-500">PostgreSQL 16</p>
          </template>
        </Card>

        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Active Tokens</span></template>
          <template #content>
            <span class="text-3xl font-bold text-blue-400">12</span>
          </template>
        </Card>
      </div>

      <Card class="bg-gray-800 border-none shadow-lg">
        <template #title><span class="text-gray-300">Recent System Errors</span></template>
        <template #content>
          <DataTable :value="systemLogs" class="p-datatable-sm">
            <Column field="timestamp" header="Time"></Column>
            <Column field="level" header="Level">
              <template #body="slotProps">
                <Tag :severity="slotProps.data.level === 'ERROR' ? 'danger' : 'warn'" :value="slotProps.data.level"></Tag>
              </template>
            </Column>
            <Column field="message" header="Message"></Column>
          </DataTable>
        </template>
      </Card>
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
import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

const systemLogs = ref([
  { timestamp: '2026-06-05 10:15:00', level: 'WARN', message: 'Token expiration approaching for user: operator' },
  { timestamp: '2026-06-05 09:42:11', level: 'ERROR', message: 'Failed to resolve location code: Z-99' }
])
</script>
