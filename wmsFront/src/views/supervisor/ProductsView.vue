<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h2 class="app-title text-2xl font-bold">Product Management</h2>
      <Button label="Add Product" icon="pi pi-plus" severity="success" />
    </div>

    <Card class="app-card">
      <template #content>
        <DataTable :value="mockProducts" stripedRows class="p-datatable-sm">
          <Column field="id" header="ID" sortable></Column>
          <Column field="name" header="Name" sortable></Column>
          <Column field="categoryName" header="Category" sortable></Column>
          <Column field="description" header="Description"></Column>
          <Column header="Actions" :exportable="false" style="min-width:8rem">
            <template #body>
              <Button icon="pi pi-pencil" outlined rounded class="mr-2" />
              <Button icon="pi pi-trash" outlined rounded severity="danger" />
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

const mockProducts = ref([
  { id: 1, name: 'Scanner RS-200', categoryName: 'Electronics', description: 'Handheld barcode scanner' },
  { id: 2, name: 'Cardboard Box (L)', categoryName: 'Packaging', description: 'Large packing box 50x50x50' },
  { id: 3, name: 'Bubble Wrap', categoryName: 'Packaging', description: '100m roll' }
])
</script>
