<template>
  <div class="min-h-screen bg-gray-900 text-gray-100 font-sans">

    <header class="flex justify-between items-center p-4 bg-gray-800 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-chart-bar text-2xl text-green-400"></i>
        <h1 class="text-xl font-bold tracking-wide">Warehouse Operations</h1>
      </div>
      <Button icon="pi pi-sign-out" label="Logout" severity="danger" text @click="handleLogout" />
    </header>

    <main class="p-4 max-w-7xl mx-auto mt-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Total Products</span></template>
          <template #content><span class="text-4xl font-bold text-gray-100">142</span></template>
        </Card>

        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Open Replenishments</span></template>
          <template #content><span class="text-4xl font-bold text-blue-400">8</span></template>
        </Card>

        <Card class="bg-gray-800 border-none shadow-lg">
          <template #title><span class="text-gray-300">Low Stock Alerts</span></template>
          <template #content><span class="text-4xl font-bold text-red-500">3</span></template>
        </Card>
      </div>

      <Card class="bg-gray-800 border-none shadow-lg">
        <template #title><span class="text-gray-300">Low Stock Inventory</span></template>
        <template #content>
          <DataTable :value="inventory" class="p-datatable-sm">
            <Column field="sku" header="SKU"></Column>
            <Column field="name" header="Product Name"></Column>
            <Column field="quantity" header="Quantity">
              <template #body="slotProps">
                <span class="text-red-400 font-bold">{{ slotProps.data.quantity }}</span>
              </template>
            </Column>
            <Column header="Action">
              <template #body>
                <Button label="Create Task" icon="pi pi-plus" size="small" severity="success" outlined />
              </template>
            </Column>
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

const inventory = ref([
  { sku: 'SCN-WLS-001', name: 'Wireless Scanner', quantity: 2 },
  { sku: 'TOWELS-12R', name: 'Paper Towels', quantity: 5 }
])
</script>
