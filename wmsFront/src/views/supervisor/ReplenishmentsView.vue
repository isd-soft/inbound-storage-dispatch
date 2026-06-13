<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <h2 class="app-title text-2xl font-bold">Replenishment Management</h2>
        <p class="app-subtitle text-sm mt-1">Monitor and assign replenishment tasks across warehouse zones.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Create Replenishment" icon="pi pi-plus" severity="success" @click="openCreateDialog" />
        <Button label="Reset Filters" icon="pi pi-filter-slash" severity="secondary" outlined @click="clearFilters" />
        <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="loadData" />
      </div>
    </div>

    <Card class="app-card mb-6">
      <template #content>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="flex flex-col gap-2">
            <label class="app-subtitle text-xs font-semibold">Filter by Product</label>
            <Dropdown v-model="filters.productId" :options="products" optionLabel="name" optionValue="id" placeholder="All Products" filter showClear class="w-full" @change="applyFilters" />
          </div>
          <div class="flex flex-col gap-2">
            <label class="app-subtitle text-xs font-semibold">Filter by Destination</label>
            <Dropdown v-model="filters.destinationLocationId" :options="locations" optionLabel="barcode" optionValue="id" placeholder="All Locations" filter showClear class="w-full" @change="applyFilters" />
          </div>
          <div class="flex flex-col gap-2">
            <label class="app-subtitle text-xs font-semibold">Filter by Status</label>
            <Dropdown v-model="filters.status" :options="statuses" placeholder="All Statuses" showClear class="w-full" @change="applyFilters" />
          </div>
        </div>
      </template>
    </Card>

    <Card class="app-card">
      <template #content>
        <DataTable
          :value="replenishments"
          :loading="loading"
          paginator
          :rows="10"
          stripedRows
          class="p-datatable-sm"
          dataKey="id"
          emptyMessage="No replenishment tasks found."
        >
          <Column field="id" header="ID" sortable></Column>

          <Column header="Product" sortable>
            <template #body="slotProps">
              <span class="app-title font-semibold">{{ getProductName(slotProps.data.productId) }}</span>
            </template>
          </Column>

          <Column field="requestedQuantity" header="Requested Qty" sortable>
            <template #body="slotProps">
              <span class="text-primary font-bold">{{ slotProps.data.requestedQuantity }}</span>
            </template>
          </Column>

          <Column header="Destination Location" sortable>
            <template #body="slotProps">
              <span class="app-subtitle">{{ getLocationName(slotProps.data.destinationLocationId) }}</span>
            </template>
          </Column>

          <Column field="status" header="Status" sortable>
            <template #body="slotProps">
              <Tag :severity="getStatusSeverity(slotProps.data.status)" :value="slotProps.data.status" />
            </template>
          </Column>

          <Column field="createdAt" header="Created At" sortable>
            <template #body="slotProps">
              <span class="app-muted text-sm">{{ formatDate(slotProps.data.createdAt) }}</span>
            </template>
          </Column>

          <Column header="Actions" style="min-width: 8rem">
            <template #body="slotProps">
              <div class="flex gap-2">
                <Button icon="pi pi-pencil" outlined rounded severity="warning" size="small" @click="openEditDialog(slotProps.data)" />
                <Button v-if="slotProps.data.status === 'CREATED'" icon="pi pi-trash" outlined rounded severity="danger" size="small" @click="confirmDelete(slotProps.data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog v-model:visible="createDialogVisible" header="Create Replenishment Task" :modal="true" class="w-full max-w-md">
      <div class="flex flex-col gap-4 mt-2">
        <div class="flex flex-col gap-2">
          <label for="product" class="app-subtitle font-medium">Product</label>
          <Dropdown id="product" v-model="newReplenishment.productId" :options="products" optionLabel="name" optionValue="id" placeholder="Select a Product" filter class="w-full" />
        </div>

        <div class="flex flex-col gap-2">
          <label for="quantity" class="app-subtitle font-medium">Requested Quantity</label>
          <InputNumber id="quantity" v-model="newReplenishment.requestedQuantity" :min="1" showButtons placeholder="Enter quantity" class="w-full" />
        </div>

        <div class="flex flex-col gap-2">
          <label for="location" class="app-subtitle font-medium">Destination Location (Pick Zone)</label>
          <Dropdown id="location" v-model="newReplenishment.destinationLocationId" :options="locations" optionLabel="barcode" optionValue="id" placeholder="Select Destination Zone" filter class="w-full" />
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text severity="secondary" @click="createDialogVisible = false" />
        <Button label="Create" icon="pi pi-check" severity="success" :loading="actionLoading" @click="handleCreate" :disabled="!isCreateFormValid" />
      </template>
    </Dialog>

    <Dialog v-model:visible="editDialogVisible" header="Update Replenishment Task" :modal="true" class="w-full max-w-md">
      <div class="flex flex-col gap-4 mt-2">
        <div class="flex flex-col gap-2">
          <label class="app-muted font-medium">Product (Read Only)</label>
          <InputText :value="getProductName(editingReplenishment.productId)" disabled class="w-full" />
        </div>

        <div class="flex flex-col gap-2">
          <label for="editQuantity" class="app-subtitle font-medium">Requested Quantity</label>
          <InputNumber id="editQuantity" v-model="editingReplenishment.requestedQuantity" :min="1" showButtons class="w-full" />
        </div>

        <div class="flex flex-col gap-2">
          <label for="editLocation" class="app-subtitle font-medium">Destination Location</label>
          <Dropdown id="editLocation" v-model="editingReplenishment.destinationLocationId" :options="locations" optionLabel="barcode" optionValue="id" filter class="w-full" />
        </div>

        <div class="flex flex-col gap-2">
          <label for="editStatus" class="app-subtitle font-medium">Status</label>
          <Dropdown id="editStatus" v-model="editingReplenishment.status" :options="statuses" placeholder="Select Status" class="w-full" />
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text severity="secondary" @click="editDialogVisible = false" />
        <Button label="Save Changes" icon="pi pi-check" severity="warning" :loading="actionLoading" @click="handleUpdate" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

import { replenishmentApi } from '@/api/replenishmentApi'
import { inventoryApi } from '@/api/inventoryApi'

const toast = useToast()
const confirm = useConfirm()

const replenishments = ref([])
const products = ref([])
const locations = ref([])
const statuses = ref(['CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELED'])

const loading = ref(false)
const actionLoading = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)

const filters = ref({ productId: null, destinationLocationId: null, status: null })

const newReplenishment = ref({ productId: null, requestedQuantity: null, destinationLocationId: null })
const editingReplenishment = ref({ id: null, taskId: null, productId: null, requestedQuantity: null, status: null, destinationLocationId: null })

const isCreateFormValid = computed(() => {
  return newReplenishment.value.productId && newReplenishment.value.requestedQuantity > 0 && newReplenishment.value.destinationLocationId
})

const getErrorMessage = (error) => error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'
const getProductName = (id) => products.value.find(p => p.id === id)?.name || `Product #${id}`
const getLocationName = (id) => locations.value.find(l => l.id === id)?.barcode || `Location #${id}`
const formatDate = (ts) => ts ? new Date(ts).toLocaleString() : '-'

const getStatusSeverity = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'IN_PROGRESS':
    case 'ASSIGNED': return 'warning'
    case 'CREATED': return 'info'
    default: return 'danger'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [productsRes, locsRes] = await Promise.all([
      inventoryApi.getProducts(),
      inventoryApi.getLocations()
    ])
    products.value = productsRes.data
    locations.value = locsRes.data.filter(l => l.available !== false && l.zone === 'PICKING')

    await applyFilters()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Load Failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const applyFilters = async () => {
  loading.value = true
  try {
    const cleanFilters = Object.fromEntries(Object.entries(filters.value).filter(([, v]) => v !== null && v !== ''))
    const res = await replenishmentApi.filter(cleanFilters)
    replenishments.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Filtering Failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const clearFilters = () => {
  filters.value = { productId: null, destinationLocationId: null, status: null }
  applyFilters()
}

const openCreateDialog = () => {
  newReplenishment.value = { productId: null, requestedQuantity: null, destinationLocationId: null }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  actionLoading.value = true
  try {
    await replenishmentApi.create(newReplenishment.value)
    toast.add({ severity: 'success', summary: 'Success', detail: 'Replenishment task created.', life: 3000 })
    createDialogVisible.value = false
    await applyFilters()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Creation Failed', detail: getErrorMessage(error), life: 5000 })
  } finally {
    actionLoading.value = false
  }
}

const openEditDialog = (data) => {
  editingReplenishment.value = { ...data }
  editDialogVisible.value = true
}

const handleUpdate = async () => {
  actionLoading.value = true
  try {
    const payload = {
      productId: editingReplenishment.value.productId,
      requestedQuantity: editingReplenishment.value.requestedQuantity,
      status: editingReplenishment.value.status,
      destinationLocationId: editingReplenishment.value.destinationLocationId
    }
    await replenishmentApi.update(editingReplenishment.value.id, payload)
    toast.add({ severity: 'success', summary: 'Updated', detail: 'Replenishment updated successfully.', life: 3000 })
    editDialogVisible.value = false
    await applyFilters()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Update Failed', detail: getErrorMessage(error), life: 5000 })
  } finally {
    actionLoading.value = false
  }
}

const confirmDelete = (task) => {
  confirm.require({
    message: `Are you sure you want to delete Replenishment #${task.id}?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await replenishmentApi.delete(task.id)
        toast.add({ severity: 'success', summary: 'Deleted', detail: 'Task has been deleted.', life: 3000 })
        await applyFilters()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Deletion Failed', detail: getErrorMessage(error), life: 4000 })
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>
