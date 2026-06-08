<template>
  <div class="p-6">
    <div class="flex items-center gap-3 mb-6">
      <h2 class="text-2xl font-bold text-gray-100">Warehouse Operations</h2>
    </div>

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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'

const inventory = ref([
  { sku: 'SCN-WLS-001', name: 'Wireless Scanner', quantity: 2 },
  { sku: 'TOWELS-12R', name: 'Paper Towels', quantity: 5 }
])
</script>
