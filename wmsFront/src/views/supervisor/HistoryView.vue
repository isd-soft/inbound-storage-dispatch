<template>
  <div class="p-6">
    <Toast />

    <AppDataTable
      :value="historyItems"
      :loading="loading"
      :filterFields="historyFilterFields"
      paginator
      :rows="10"
      stripedRows
      class="p-datatable-sm"
      dataKey="id"
      emptyMessage="No inventory history records found."
    >
      <template #toolbar>
        <Button icon="pi pi-refresh" size="small" severity="secondary" outlined :loading="loading" aria-label="Refresh" @click="loadHistory" />
      </template>
          <Column field="timestamp" header="Time" sortable filter>
            <template #body="slotProps">
              {{ formatTimestamp(slotProps.data.timestamp) }}
            </template>
          </Column>
          <Column field="productName" header="Product" sortable filter>
            <template #body="slotProps">
              <ProductLink :product-id="slotProps.data.productId" :barcode="slotProps.data.barcode" :name="slotProps.data.productName" class="font-semibold" />
            </template>
          </Column>
          <Column field="barcode" header="Barcode" sortable filter></Column>
          <Column field="alteredQuantity" header="Altered Qty" sortable filter>
            <template #body="slotProps">
              <span :class="slotProps.data.alteredQuantity >= 0 ? 'app-success font-bold' : 'app-danger font-bold'">
                {{ slotProps.data.alteredQuantity > 0 ? '+' : '' }}{{ slotProps.data.alteredQuantity }}
              </span>
            </template>
          </Column>
          <Column field="quantityAfterChange" header="Qty After" sortable filter></Column>
          <Column field="sourceBarcode" header="Source Location" sortable filter>
            <template #body="slotProps">
              {{ slotProps.data.sourceBarcode || '-' }}
            </template>
          </Column>
          <Column field="destinationBarcode" header="Destination Location" sortable filter>
            <template #body="slotProps">
              {{ slotProps.data.destinationBarcode || '-' }}
            </template>
          </Column>
          <Column field="operationType" header="Operation" sortable filter></Column>
          <Column field="username" header="User" sortable filter>
            <template #body="slotProps">
              {{ slotProps.data.username || `User #${slotProps.data.userId}` }}
            </template>
          </Column>
    </AppDataTable>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Toast from 'primevue/toast'

import { inventoryApi } from '@/api/inventoryApi'

const route = useRoute()
const toast = useToast()

const historyItems = ref([])
const loading = ref(false)
const historyFilterFields = [
  { field: 'timestamp', label: 'Time' },
  { field: 'productName', label: 'Product' },
  { field: 'barcode', label: 'Barcode' },
  { field: 'alteredQuantity', label: 'Altered Qty' },
  { field: 'quantityAfterChange', label: 'Qty After' },
  { field: 'sourceBarcode', label: 'Source' },
  { field: 'destinationBarcode', label: 'Destination' },
  { field: 'operationType', label: 'Operation' },
  { field: 'username', label: 'User' }
]

const getErrorMessage = (error) => {
  return error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'
}

const loadHistory = async () => {
  loading.value = true
  try {
    const stockId = route.query.stockId
    const response = stockId ? await inventoryApi.getStockHistory(stockId) : await inventoryApi.getAllHistory()
    historyItems.value = response.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'History load failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(timestamp))
}

watch(() => route.query.stockId, loadHistory)

onMounted(loadHistory)
</script>
