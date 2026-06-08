<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold text-gray-100 mb-6">Inventory History</h2>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable :value="mockHistory" paginator :rows="10" stripedRows class="p-datatable-sm">
          <Column field="timestamp" header="Date/Time" sortable></Column>
          <Column field="sku" header="SKU" sortable></Column>
          <Column field="operationType" header="Operation"></Column>
          <Column field="alteredQuantity" header="Change">
            <template #body="slotProps">
              <span :class="slotProps.data.alteredQuantity > 0 ? 'text-green-400 font-bold' : 'text-red-400 font-bold'">
                {{ slotProps.data.alteredQuantity > 0 ? '+' : '' }}{{ slotProps.data.alteredQuantity }}
              </span>
            </template>
          </Column>
          <Column field="quantityAfterChange" header="Total Qty"></Column>
          <Column field="sourceLocation" header="From"></Column>
          <Column field="destinationLocation" header="To"></Column>
          <Column field="userId" header="User ID"></Column>
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

const mockHistory = ref([
  { id: 1, timestamp: '2026-06-05 14:30', sku: 'BOX-L-99', operationType: 'REPLENISHMENT', alteredQuantity: 50, quantityAfterChange: 150, sourceLocation: 'RECEIVING-01', destinationLocation: 'PICK-A1', userId: 2 },
  { id: 2, timestamp: '2026-06-05 15:45', sku: 'BOX-L-99', operationType: 'PICKING', alteredQuantity: -10, quantityAfterChange: 140, sourceLocation: 'PICK-A1', destinationLocation: 'DISPATCH-01', userId: 3 },
  { id: 3, timestamp: '2026-06-05 16:00', sku: 'SCN-01', operationType: 'MANUAL_ADJUST', alteredQuantity: 2, quantityAfterChange: 2, sourceLocation: 'NONE', destinationLocation: 'PICK-B2', userId: 1 }
])
</script>
