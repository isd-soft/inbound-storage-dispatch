<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-gray-100">Live Inventory</h2>
        <p class="text-sm text-gray-400 mt-1">Monitor stock quantities and apply manual stock changes.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button v-if="canManageStock" label="Add Stock" icon="pi pi-plus" severity="success" @click="openAddDialog" />
        <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="loadInventoryData" />
      </div>
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable
          :value="stockItems"
          :loading="loading"
          paginator
          :rows="10"
          stripedRows
          class="p-datatable-sm"
          dataKey="id"
          emptyMessage="No inventory stock found."
        >
          <Column field="productName" header="Product" sortable></Column>
          <Column field="sku" header="SKU" sortable></Column>
          <Column field="locationCode" header="Location" sortable></Column>
          <Column field="quantity" header="Quantity" sortable>
            <template #body="slotProps">
              <span :class="slotProps.data.quantity < 10 ? 'text-red-400 font-bold' : 'text-green-400'">
                {{ slotProps.data.quantity }}
              </span>
            </template>
          </Column>
          <Column field="manufactureDate" header="Manufactured" sortable>
            <template #body="slotProps">
              {{ slotProps.data.manufactureDate || '-' }}
            </template>
          </Column>
          <Column field="expirationDate" header="Expires" sortable>
            <template #body="slotProps">
              {{ slotProps.data.expirationDate || '-' }}
            </template>
          </Column>
          <Column header="Actions" style="min-width:18rem">
            <template #body="slotProps">
              <div class="flex flex-wrap gap-2">
                <Button v-if="canManageStock" label="Remove" size="small" severity="danger" outlined @click="openStockDialog('remove', slotProps.data)" />
                <Button v-if="canManageStock" label="Adjust" size="small" severity="warning" outlined @click="openStockDialog('adjust', slotProps.data)" />
                <Button label="History" size="small" severity="info" outlined @click="viewHistory(slotProps.data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <StockActionDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :selectedStock="selectedStock"
      :products="products"
      :locations="locations"
      :loading="actionLoading"
      @submit="handleStockAction"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import { useRouter } from 'vue-router'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Toast from 'primevue/toast'

import StockActionDialog from '@/components/inventory/StockActionDialog.vue'
import { inventoryApi } from '@/api/inventoryApi'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()

const stockItems = ref([])
const products = ref([])
const locations = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('add')
const selectedStock = ref(null)
const canManageStock = computed(() => authStore.hasAnyRole(['ROLE_SUPERVISOR', 'ROLE_DEV']))

const getErrorMessage = (error) => {
  return error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'
}

const currentUserId = () => {
  if (authStore.user?.id) return authStore.user.id
  const storedUserId = localStorage.getItem('user_id')
  if (storedUserId) return Number(storedUserId)
  if (authStore.role === 'ROLE_DEV') return 1
  if (authStore.role === 'ROLE_SUPERVISOR') return 2
  if (authStore.role === 'ROLE_OPERATOR') return 3
  return null
}

const loadInventoryData = async () => {
  loading.value = true
  try {
    const [stockResponse, productsResponse, locationsResponse] = await Promise.all([
      inventoryApi.getAllStock(),
      inventoryApi.getProducts(),
      inventoryApi.getLocations()
    ])
    stockItems.value = stockResponse.data
    products.value = productsResponse.data
    locations.value = locationsResponse.data.filter((location) => location.available !== false)
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Inventory load failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  selectedStock.value = null
  dialogMode.value = 'add'
  dialogVisible.value = true
}

const openStockDialog = (mode, stock) => {
  selectedStock.value = stock
  dialogMode.value = mode
  dialogVisible.value = true
}

const refreshAfterAction = async () => {
  await loadInventoryData()
}

const submitAction = async (payload) => {
  const userId = currentUserId()
  if (!userId) {
    toast.add({ severity: 'error', summary: 'Missing user', detail: 'User id is required for stock changes.', life: 4000 })
    return
  }

  actionLoading.value = true
  try {
    const requestPayload = { ...payload, userId }
    if (dialogMode.value === 'add') {
      await inventoryApi.addStock(requestPayload)
      toast.add({ severity: 'success', summary: 'Stock added', detail: 'Inventory stock was added.', life: 3000 })
    } else if (dialogMode.value === 'remove') {
      await inventoryApi.removeStock(requestPayload)
      toast.add({ severity: 'success', summary: 'Stock removed', detail: 'Inventory stock was removed.', life: 3000 })
    } else {
      await inventoryApi.adjustStock(requestPayload)
      toast.add({ severity: 'success', summary: 'Stock adjusted', detail: 'Inventory quantity was updated.', life: 3000 })
    }
    dialogVisible.value = false
    await refreshAfterAction()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Stock action failed', detail: getErrorMessage(error), life: 5000 })
  } finally {
    actionLoading.value = false
  }
}

const handleStockAction = (payload) => {
  if (dialogMode.value !== 'remove') {
    submitAction(payload)
    return
  }

  confirm.require({
    message: 'Remove this quantity from the selected stock?',
    header: 'Confirm Remove Stock',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => submitAction(payload)
  })
}

const viewHistory = (stock) => {
  router.push({ name: 'history', query: { stockId: stock.id } })
}

onMounted(loadInventoryData)
</script>
