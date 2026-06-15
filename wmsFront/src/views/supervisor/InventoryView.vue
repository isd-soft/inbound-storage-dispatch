<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <AppDataTable
      v-model:selection="selectedStockItems"
      :value="stockItems"
      :loading="loading"
      :filterFields="inventoryFilterFields"
      paginator
      :rows="10"
      stripedRows
      class="p-datatable-sm"
      dataKey="id"
      emptyMessage="No inventory stock found."
    >
      <template #toolbar>
        <Button icon="pi pi-refresh" size="small" severity="secondary" outlined :loading="loading" aria-label="Refresh" @click="loadInventoryData" />
        <Button v-if="canManageStock" label="Add Stock" icon="pi pi-plus" severity="success" @click="openAddDialog" />
        <Button :label="editMode ? 'Exit Edit' : 'Edit'" icon="pi pi-pencil" severity="warning" outlined @click="toggleEditMode" />
        <Button v-if="editMode && canManageStock" label="Remove" icon="pi pi-minus" severity="danger" outlined :disabled="selectedStockItems.length !== 1" @click="openStockDialog('remove', selectedStockItems[0])" />
        <Button v-if="editMode && canManageStock" label="Adjust" icon="pi pi-sliders-h" severity="warning" outlined :disabled="selectedStockItems.length !== 1" @click="openStockDialog('adjust', selectedStockItems[0])" />
        <Button v-if="editMode" label="History" icon="pi pi-history" severity="info" outlined :disabled="selectedStockItems.length !== 1" @click="viewHistory(selectedStockItems[0])" />
        <span v-if="editMode" class="app-muted text-sm">{{ selectedStockItems.length }} selected</span>
      </template>
          <Column v-if="editMode" selectionMode="multiple" headerStyle="width: 3rem" />
          <Column field="productName" header="Product" sortable filter>
            <template #body="{ data }">
              <ProductLink :product-id="data.productId" :barcode="data.barcode" :name="data.productName" class="font-semibold" />
            </template>
          </Column>

          <Column field="barcode" header="Barcode" sortable filter></Column>
          <Column field="locationBarcode" header="Location" sortable filter></Column>
          <Column field="quantity" header="Total Qty" sortable filter>
            <template #body="{ data }">
              <span class="app-title font-bold text-base">{{ data.quantity }}</span>
            </template>
          </Column>

          <Column field="reservedQuantity" header="Reserved" sortable filter>
            <template #body="{ data }">
              <span class="font-bold text-base" :class="data.reservedQuantity > 0 ? 'text-orange-500' : 'app-muted'">
                {{ data.reservedQuantity || 0 }}
              </span>
            </template>
          </Column>

          <!-- Available -->
          <Column field="availableQuantity" header="Available" sortable filter>
            <template #body="{ data }">
              <span class="font-bold text-base" :class="data.availableQuantity <= 0 ? 'text-red-500' : (data.availableQuantity < 10 ? 'text-yellow-500' : 'text-green-500')">
                {{ data.availableQuantity }}
              </span>
            </template>
          </Column>

          <Column field="manufactureDate" header="Manufactured" sortable filter>
            <template #body="{ data }">
              <span class="app-muted text-sm">{{ data.manufactureDate || '-' }}</span>
            </template>
          </Column>

          <Column field="expirationDate" header="Expires" sortable filter>
            <template #body="{ data }">
              <span class="app-muted text-sm">{{ data.expirationDate || '-' }}</span>
            </template>
          </Column>

          <!-- Inline editing and bulk delete are intentionally not enabled here because stock changes must create audit history and respect reserved quantity rules. -->
    </AppDataTable>

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
const selectedStockItems = ref([])
const products = ref([])
const locations = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const editMode = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('add')
const selectedStock = ref(null)
const canManageStock = computed(() => authStore.hasAnyRole(['ROLE_SUPERVISOR', 'ROLE_DEV']))
const inventoryFilterFields = [
  { field: 'productName', label: 'Product' },
  { field: 'barcode', label: 'Barcode' },
  { field: 'locationBarcode', label: 'Location' },
  { field: 'quantity', label: 'Total Qty' },
  { field: 'reservedQuantity', label: 'Reserved' },
  { field: 'availableQuantity', label: 'Available' },
  { field: 'manufactureDate', label: 'Manufactured' },
  { field: 'expirationDate', label: 'Expires' }
]

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

const toggleEditMode = () => {
  editMode.value = !editMode.value
  selectedStockItems.value = []
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
    selectedStockItems.value = []
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
