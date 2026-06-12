<template>
  <div class="p-6">
    <Toast />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <h2 class="app-title text-2xl font-bold">Inventory History</h2>
        <p class="app-subtitle text-sm mt-1">Review inventory movement and manual stock modification records.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="All History" icon="pi pi-list" severity="secondary" outlined @click="showAllHistory" />
        <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="loadHistory" />
      </div>
    </div>

    <Card class="app-card">
      <template #content>
        <DataTable
          :value="historyItems"
          :loading="loading"
          paginator
          :rows="10"
          stripedRows
          class="p-datatable-sm"
          dataKey="id"
          emptyMessage="No inventory history records found."
        >
          <Column field="timestamp" header="Date/Time" sortable>
            <template #body="slotProps">
              {{ formatTimestamp(slotProps.data.timestamp) }}
            </template>
          </Column>
          <Column field="productName" header="Product" sortable></Column>
          <Column field="barcode" header="barcode" sortable></Column>
          <Column field="alteredQuantity" header="Altered Qty" sortable>
            <template #body="slotProps">
              <span :class="slotProps.data.alteredQuantity >= 0 ? 'app-success font-bold' : 'app-danger font-bold'">
                {{ slotProps.data.alteredQuantity > 0 ? '+' : '' }}{{ slotProps.data.alteredQuantity }}
              </span>
            </template>
          </Column>
          <Column field="quantityAfterChange" header="Qty After" sortable></Column>
          <Column field="sourceBarcode" header="Source Location" sortable>
            <template #body="slotProps">
              {{ slotProps.data.sourceBarcode || '-' }}
            </template>
          </Column>
          <Column field="destinationBarcode" header="Destination Location" sortable>
            <template #body="slotProps">
              {{ slotProps.data.destinationBarcode || '-' }}
            </template>
          </Column>
          <Column field="operationType" header="Operation" sortable></Column>
          <Column field="username" header="User" sortable>
            <template #body="slotProps">
              {{ slotProps.data.username || `User #${slotProps.data.userId}` }}
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Toast from 'primevue/toast'

import { inventoryApi } from '@/api/inventoryApi'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const historyItems = ref([])
const loading = ref(false)

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
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(timestamp))
}

const showAllHistory = () => {
  router.push({ name: 'history' })
}

watch(() => route.query.stockId, loadHistory)

onMounted(loadHistory)
</script>
