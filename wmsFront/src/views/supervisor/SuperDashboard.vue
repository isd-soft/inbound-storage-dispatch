<template>
  <div class="p-6">
    <div class="flex items-center gap-3 mb-6">
      <h2 class="app-title text-2xl font-bold">Warehouse Operations</h2>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <Card class="app-card">
        <template #title><span class="app-subtitle">Total Products</span></template>
        <template #content><span class="app-title text-4xl font-bold">142</span></template>
      </Card>

      <Card class="app-card">
        <template #title><span class="app-subtitle">Open Replenishments</span></template>
        <template #content><span class="app-brand text-4xl font-bold">8</span></template>
      </Card>

      <Card class="app-card">
        <template #title><span class="app-subtitle">Low Stock Alerts</span></template>
        <template #content><span class="app-danger text-4xl font-bold">3</span></template>
      </Card>
    </div>

    <Card class="app-card">
      <template #title><span class="app-subtitle">Low Stock Inventory</span></template>
      <template #content>
        <DataTable :value="inventory" class="p-datatable-sm">
          <Column field="sku" header="SKU"></Column>
          <Column field="name" header="Product Name"></Column>
          <Column field="quantity" header="Quantity">
            <template #body="slotProps">
              <span class="app-danger font-bold">{{ slotProps.data.quantity }}</span>
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
