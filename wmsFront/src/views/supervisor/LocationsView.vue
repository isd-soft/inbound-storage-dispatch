<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-100">Warehouse Locations</h2>
      <Button label="Create Location" icon="pi pi-plus" severity="success" />
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable :value="mockLocations" stripedRows class="p-datatable-sm">
          <Column field="locationCode" header="Code" sortable></Column>
          <Column field="zone" header="Zone" sortable></Column>
          <Column field="description" header="Description"></Column>
          <Column header="Status">
            <template #body="slotProps">
              <Tag :severity="slotProps.data.available ? 'success' : 'danger'"
                   :value="slotProps.data.available ? 'AVAILABLE' : 'DISABLED'" />
            </template>
          </Column>
          <Column header="Actions" style="min-width:8rem">
            <template #body>
              <Button icon="pi pi-pencil" outlined rounded />
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
import Tag from 'primevue/tag'

const mockLocations = ref([
  { id: 1, locationCode: 'RECEIVING-01', zone: 'Inbound', description: 'Main receiving dock', available: true },
  { id: 2, locationCode: 'PICK-A1', zone: 'Storage', description: 'Aisle 1, Rack A', available: true },
  { id: 3, locationCode: 'DISPATCH-01', zone: 'Outbound', description: 'Damaged goods dock', available: false }
])
</script>
