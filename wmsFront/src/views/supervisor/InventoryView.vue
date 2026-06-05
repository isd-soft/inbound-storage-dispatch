<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-100">Live Inventory</h2>
      <Button label="Manual Adjustment" icon="pi pi-sliders-h" severity="warning" outlined />
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable :value="mockInventory" paginator :rows="10" stripedRows class="p-datatable-sm">
          <Column field="sku" header="SKU (Barcode)" sortable></Column>
          <Column field="productName" header="Product" sortable></Column>
          <Column field="locationCode" header="Location" sortable></Column>
          <Column field="quantity" header="Quantity" sortable>
            <template #body="slotProps">
              <span :class="slotProps.data.quantity < 10 ? 'text-red-400 font-bold' : 'text-green-400'">
                {{ slotProps.data.quantity }}
              </span>
            </template>
          </Column>
          <Column header="Actions">
            <template #body>
              <Button label="Replenish" size="small" severity="info" outlined />
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

const mockInventory = ref([
  { id: 1, sku: 'SCN-WLS-001', productName: 'Scanner RS-200', locationCode: 'PICK-A1', quantity: 2 },
  { id: 2, sku: 'BOX-L-99', productName: 'Cardboard Box (L)', locationCode: 'RECEIVING-01', quantity: 150 },
  { id: 3, sku: 'TOWELS-12R', productName: 'Paper Towels', locationCode: 'PICK-B2', quantity: 45 }
])
</script>
