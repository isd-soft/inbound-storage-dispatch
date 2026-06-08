<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-100">Warehouse Tasks</h2>
      <Button label="Create Task" icon="pi pi-plus" severity="success" />
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable :value="mockTasks" paginator :rows="10" stripedRows class="p-datatable-sm">
          <Column field="id" header="Task ID" sortable></Column>
          <Column field="type" header="Type"></Column>
          <Column field="productName" header="Product"></Column>
          <Column field="status" header="Status" sortable>
            <template #body="slotProps">
              <Tag :severity="getStatusSeverity(slotProps.data.status)" :value="slotProps.data.status" />
            </template>
          </Column>
          <Column field="operator" header="Assigned To"></Column>
          <Column header="Actions">
            <template #body="slotProps">
              <Button v-if="slotProps.data.status === 'CREATED'" icon="pi pi-user-plus" outlined rounded size="small" />
              <Button icon="pi pi-times" outlined rounded severity="danger" size="small" class="ml-2" />
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

const mockTasks = ref([
  { id: 101, type: 'REPLENISHMENT', productName: 'Cardboard Box (L)', status: 'IN_PROGRESS', operator: 'smoothOperator' },
  { id: 102, type: 'PICKING', productName: 'Scanner RS-200', status: 'CREATED', operator: 'Unassigned' },
  { id: 103, type: 'REPLENISHMENT', productName: 'Bubble Wrap', status: 'COMPLETED', operator: 'smoothOperator' }
])

const getStatusSeverity = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'CREATED') return 'info'
  return 'danger'
}
</script>
