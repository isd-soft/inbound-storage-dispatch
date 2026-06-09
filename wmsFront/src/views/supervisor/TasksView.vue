<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-gray-100">Warehouse Tasks (Replenishments)</h2>
        <p class="text-sm text-gray-400 mt-1">Manage stock replenishment requests for operators.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Create Task" icon="pi pi-plus" severity="success" @click="openCreateDialog" />
        <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="loadData" />
      </div>
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable
          :value="tasks"
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
              <span class="font-semibold">{{ getProductName(slotProps.data.productId) }}</span>
            </template>
          </Column>

          <Column field="requestedQuantity" header="Requested Qty" sortable>
            <template #body="slotProps">
              <span class="text-blue-400 font-bold">{{ slotProps.data.requestedQuantity }}</span>
            </template>
          </Column>

          <Column header="Destination" sortable>
            <template #body="slotProps">
              {{ getLocationName(slotProps.data.destinationLocationId) }}
            </template>
          </Column>

          <Column field="status" header="Status" sortable>
            <template #body="slotProps">
              <Tag :severity="getStatusSeverity(slotProps.data.status)" :value="slotProps.data.status" />
            </template>
          </Column>

          <Column header="Actions" style="min-width: 8rem">
            <template #body="slotProps">
              <Button
                v-if="slotProps.data.status === 'CREATED'"
                icon="pi pi-trash"
                outlined
                rounded
                severity="danger"
                size="small"
                @click="confirmDelete(slotProps.data)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog v-model:visible="dialogVisible" header="Create Replenishment Task" :modal="true" class="p-fluid w-full max-w-md">
      <div class="field mb-4">
        <label for="product" class="block text-sm font-medium mb-1">Product</label>
        <Dropdown
          id="product"
          v-model="newTask.productId"
          :options="products"
          optionLabel="name"
          optionValue="id"
          placeholder="Select a Product"
          filter
        />
      </div>

      <div class="field mb-4">
        <label for="quantity" class="block text-sm font-medium mb-1">Requested Quantity</label>
        <InputNumber
          id="quantity"
          v-model="newTask.requestedQuantity"
          :min="1"
          showButtons
          placeholder="Enter quantity"
        />
      </div>

      <div class="field mb-4">
        <label for="location" class="block text-sm font-medium mb-1">Destination Location</label>
        <Dropdown
          id="location"
          v-model="newTask.destinationLocationId"
          :options="locations"
          optionLabel="locationCode"
          optionValue="id"
          placeholder="Select Destination Zone"
          filter
        />
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text @click="dialogVisible = false" />
        <Button label="Create Task" icon="pi pi-check" severity="success" :loading="actionLoading" @click="createTask" :disabled="!isFormValid" />
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
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'


import { replenishmentApi } from '@/api/replenishmentApi'
import { inventoryApi } from '@/api/inventoryApi'

const toast = useToast()
const confirm = useConfirm()

const tasks = ref([])
const products = ref([])
const locations = ref([])

const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)

const newTask = ref({
  productId: null,
  requestedQuantity: null,
  destinationLocationId: null
})

const isFormValid = computed(() => {
  return newTask.value.productId &&
    newTask.value.requestedQuantity > 0 &&
    newTask.value.destinationLocationId
})

const getErrorMessage = (error) => {
  return error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'
}

const getStatusSeverity = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS' || status === 'ASSIGNED') return 'warning'
  if (status === 'CREATED') return 'info'
  return 'danger'
}

const getProductName = (id) => {
  const product = products.value.find(p => p.id === id)
  return product ? product.name : `Product #${id}`
}

const getLocationName = (id) => {
  const loc = locations.value.find(l => l.id === id)
  return loc ? loc.locationCode : `Location #${id}`
}

const loadData = async () => {
  loading.value = true
  try {
    const [tasksRes, productsRes, locsRes] = await Promise.all([
      replenishmentApi.getAll(),
      inventoryApi.getProducts(),
      inventoryApi.getLocations()
    ])

    tasks.value = tasksRes.data
    products.value = productsRes.data
    locations.value = locsRes.data.filter(l => l.available !== false) // Only show available locations
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Load Failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  newTask.value = { productId: null, requestedQuantity: null, destinationLocationId: null }
  dialogVisible.value = true
}

const createTask = async () => {
  actionLoading.value = true
  try {
    await replenishmentApi.create(newTask.value)
    toast.add({ severity: 'success', summary: 'Task Created', detail: 'Replenishment task has been created successfully.', life: 3000 })
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Creation Failed', detail: getErrorMessage(error), life: 5000 })
  } finally {
    actionLoading.value = false
  }
}

const confirmDelete = (task) => {
  confirm.require({
    message: `Are you sure you want to delete Task #${task.id}?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await replenishmentApi.delete(task.id)
        toast.add({ severity: 'success', summary: 'Deleted', detail: 'Task has been deleted.', life: 3000 })
        await loadData()
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
