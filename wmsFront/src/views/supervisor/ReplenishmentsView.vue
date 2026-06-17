<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <AppDataTable
      v-model:selection="selectedReplenishments"
      :value="replenishments"
      :loading="loading"
      :filterFields="replenishmentFilterFields"
      paginator
      :rows="10"
      stripedRows
      class="p-datatable-sm"
      dataKey="id"
      emptyMessage="No replenishment tasks found."
    >
      <template #toolbar>
        <Button
          icon="pi pi-refresh"
          size="small"
          severity="secondary"
          outlined
          :loading="loading"
          aria-label="Refresh"
          @click="loadData"
        />
        <Button label="Create" icon="pi pi-plus" severity="success" @click="openCreateDialog" />

        <Button
          :label="editMode ? 'Exit Edit' : 'Edit'"
          icon="pi pi-pencil"
          severity="warning"
          outlined
          @click="toggleEditMode"
        />

        <Button
          v-if="editMode"
          label="Edit Selected"
          icon="pi pi-pencil"
          severity="warning"
          outlined
          :disabled="selectedReplenishments.length !== 1"
          @click="openEditDialog(selectedReplenishments[0])"
        />

        <Button
          v-if="editMode"
          label="Cancel Selected"
          icon="pi pi-ban"
          severity="secondary"
          outlined
          :disabled="!canCancelSelected"
          @click="confirmCancelSelected"
        />

        <Button
          v-if="editMode"
          label="Delete Selected"
          icon="pi pi-trash"
          severity="danger"
          outlined
          :disabled="!canDeleteSelected"
          @click="confirmDeleteSelected"
        />

        <span v-if="editMode" class="app-muted text-sm">
          {{ selectedReplenishments.length }} selected
        </span>
      </template>

      <Column v-if="editMode" selectionMode="multiple" headerStyle="width: 3rem" />
      <Column field="productName" header="Product" sortable filter>
        <template #body="slotProps">
          <ProductLink
            :product-id="slotProps.data.productId"
            :name="slotProps.data.productName"
            class="font-semibold"
          />
        </template>
      </Column>

      <Column field="requestedQuantity" header="Requested Qty" sortable filter>
        <template #body="slotProps">
          <span class="text-primary font-bold">{{ slotProps.data.requestedQuantity }}</span>
        </template>
      </Column>

      <Column field="locationName" header="Destination Location" sortable filter>
        <template #body="slotProps">
          <span class="app-subtitle">{{ slotProps.data.locationName }}</span>
        </template>
      </Column>

      <Column field="status" header="Status" sortable filter>
        <template #body="slotProps">
          <Tag
            :severity="getStatusSeverity(slotProps.data.status)"
            :value="slotProps.data.status"
          />
        </template>
      </Column>

      <Column field="assignedOperatorId" header="Assigned Operator">
        <template #body="slotProps">
          <Dropdown
            v-model="assignmentByTaskId[slotProps.data.taskId]"
            :options="operators"
            optionLabel="username"
            optionValue="id"
            placeholder="Select operator"
            filter
            class="w-full"
            :disabled="isAssignmentLocked(slotProps.data)"
            @change="
              assignReplenishment(slotProps.data.id, assignmentByTaskId[slotProps.data.taskId])
            "
          />
        </template>
      </Column>

      <Column field="createdAt" header="Created" sortable filter>
        <template #body="slotProps">
          <span class="app-muted text-sm">{{ formatDate(slotProps.data.createdAt) }}</span>
        </template>
      </Column>
    </AppDataTable>

    <Dialog
      v-model:visible="createDialogVisible"
      header="Create Replenishment Task"
      :modal="true"
      class="w-full max-w-md"
    >
      <div class="flex flex-col gap-4 mt-2">
        <div class="flex flex-col gap-2">
          <label for="product" class="app-subtitle font-medium">Product</label>
          <Dropdown
            id="product"
            v-model="newReplenishment.productId"
            :options="products"
            optionLabel="name"
            optionValue="id"
            placeholder="Select a Product"
            filter
            class="w-full"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="quantity" class="app-subtitle font-medium">Requested Quantity</label>
          <InputNumber
            id="quantity"
            v-model="newReplenishment.requestedQuantity"
            :min="1"
            showButtons
            placeholder="Enter quantity"
            class="w-full"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="location" class="app-subtitle font-medium"
            >Destination Location (Pick Zone)</label
          >
          <Dropdown
            id="location"
            v-model="newReplenishment.destinationLocationId"
            :options="locations"
            optionLabel="barcode"
            optionValue="id"
            placeholder="Select Destination Zone"
            filter
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="Cancel"
          icon="pi pi-times"
          text
          severity="secondary"
          @click="createDialogVisible = false"
        />
        <Button
          label="Create"
          icon="pi pi-check"
          severity="success"
          :loading="actionLoading"
          @click="handleCreate"
          :disabled="!isCreateFormValid"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="editDialogVisible"
      header="Update Replenishment Task"
      :modal="true"
      class="w-full max-w-md"
    >
      <div class="flex flex-col gap-4 mt-2">
        <div class="flex flex-col gap-2">
          <label class="app-muted font-medium">Product (Read Only)</label>
          <InputText
            :value="getProductName(editingReplenishment.productId)"
            disabled
            class="w-full"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="editQuantity" class="app-subtitle font-medium">Requested Quantity</label>
          <InputNumber
            id="editQuantity"
            v-model="editingReplenishment.requestedQuantity"
            :min="1"
            showButtons
            class="w-full"
          />
        </div>

        <div class="flex flex-col gap-2">
          <label for="editLocation" class="app-subtitle font-medium">Destination Location</label>
          <Dropdown
            id="editLocation"
            v-model="editingReplenishment.destinationLocationId"
            :options="locations"
            optionLabel="barcode"
            optionValue="id"
            filter
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="Cancel"
          icon="pi pi-times"
          text
          severity="secondary"
          @click="editDialogVisible = false"
        />
        <Button
          label="Save Changes"
          icon="pi pi-check"
          severity="warning"
          :loading="actionLoading"
          @click="handleUpdate"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

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
import { userApi } from '@/api/userApi'
import { productApi } from '@/api/productApi'

const toast = useToast()
const confirm = useConfirm()

const replenishments = ref([])
const selectedReplenishments = ref([])
const products = ref([])
const locations = ref([])
const operators = ref([])

const loading = ref(false)
const actionLoading = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editMode = ref(false)

const filters = ref({ productId: null, destinationLocationId: null, status: null })

const hasSelection = computed(() => selectedReplenishments.value.length > 0)

const isUniformSelection = computed(() => {
  if (!hasSelection.value) return false
  const firstStatus = selectedReplenishments.value[0].status
  return selectedReplenishments.value.every(task => task.status === firstStatus)
})

const unifiedStatus = computed(() => isUniformSelection.value ? selectedReplenishments.value[0].status : null)

const canDeleteSelected = computed(() => {
  return isUniformSelection.value && unifiedStatus.value === 'CREATED'
})

const canCancelSelected = computed(() => {
  return isUniformSelection.value && ['CREATED', 'ASSIGNED', 'IN_PROGRESS'].includes(unifiedStatus.value)
})

const assignmentByTaskId = ref({})

const replenishmentFilterFields = [
  { field: 'productName', label: 'Product' },
  { field: 'requestedQuantity', label: 'Requested Qty' },
  { field: 'locationName', label: 'Destination' },
  { field: 'status', label: 'Status' },
  { field: 'createdAt', label: 'Created' },
]

const newReplenishment = ref({
  productId: null,
  requestedQuantity: null,
  destinationLocationId: null,
})

const editingReplenishment = ref({
  id: null,
  taskId: null,
  productId: null,
  requestedQuantity: null,
  destinationLocationId: null,
})

const isCreateFormValid = computed(
  () =>
    newReplenishment.value.productId &&
    newReplenishment.value.requestedQuantity > 0 &&
    newReplenishment.value.destinationLocationId,
)

const getErrorMessage = (error) =>
  error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'

const getProductName = (id) => products.value.find((p) => p.id === id)?.name || `Product #${id}`

const getLocationName = (id) =>
  locations.value.find((l) => l.id === id)?.barcode || `Location #${id}`

const getOperatorName = (id) => operators.value.find((o) => o.id === id)?.username || ''

const formatDate = (ts) => {
  if (!ts) return '-'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(ts))
}

const getStatusSeverity = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'CANCELED':
      return 'secondary'
    case 'IN_PROGRESS':
    case 'ASSIGNED':
      return 'warning'
    case 'CREATED':
      return 'info'
    default:
      return 'danger'
  }
}

const isAssignmentLocked = (task) => ['COMPLETED', 'CANCELED', 'CANCELLED'].includes(task.status)

const loadData = async () => {
  loading.value = true
  try {
    const [productsRes, locsRes, usersRes] = await Promise.all([
      productApi.getAllProductsWithQuantityInZone('REPLENISHMENT'),
      inventoryApi.getLocations(),
      userApi.getAll(),
    ])

    products.value = productsRes.data
    locations.value = locsRes.data.filter((l) => l.available !== false && l.zone === 'PICKING')
    operators.value = (usersRes.data || []).filter((u) => u.userRole === 'ROLE_OPERATOR')

    await applyFilters()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Load Failed',
      detail: getErrorMessage(error),
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

const applyFilters = async () => {
  loading.value = true
  try {
    const cleanFilters = Object.fromEntries(
      Object.entries(filters.value).filter(([, v]) => v !== null && v !== ''),
    )

    const res = await replenishmentApi.filter(cleanFilters)

    replenishments.value = res.data.map((task) => ({
      ...task,
      productName: getProductName(task.productId),
      locationName: getLocationName(task.destinationLocationId),
      assignedOperatorName: getOperatorName(task.assignedOperatorId),
    }))

    assignmentByTaskId.value = Object.fromEntries(
      replenishments.value.map((task) => [task.taskId, task.assignedOperatorId || null]),
    )
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Filtering Failed',
      detail: getErrorMessage(error),
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  newReplenishment.value = {
    productId: null,
    requestedQuantity: null,
    destinationLocationId: null,
  }
  createDialogVisible.value = true
}

const toggleEditMode = () => {
  editMode.value = !editMode.value
  selectedReplenishments.value = []
}

const handleCreate = async () => {
  actionLoading.value = true
  try {
    await replenishmentApi.create(newReplenishment.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Replenishment task created.',
      life: 3000,
    })
    createDialogVisible.value = false
    await applyFilters()
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
      destinationLocationId: editingReplenishment.value.destinationLocationId,
    }

    await replenishmentApi.update(editingReplenishment.value.id, payload)

    toast.add({
      severity: 'success',
      summary: 'Updated',
      detail: 'Replenishment updated successfully.',
      life: 3000,
    })

    editDialogVisible.value = false
    await applyFilters()
  } finally {
    actionLoading.value = false
  }
}

const assignReplenishment = async (id, operatorId) => {
  if (!operatorId) return

  actionLoading.value = true
  try {
    await replenishmentApi.assign(id, operatorId)

    toast.add({
      severity: 'success',
      summary: 'Assigned',
      detail: `Replenishment #${id} assigned to operator.`,
      life: 3000,
    })

    await applyFilters()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Assign failed',
      detail: getErrorMessage(error),
      life: 5000,
    })
  } finally {
    actionLoading.value = false
  }
}

const confirmDeleteSelected = () => {
  confirm.require({
    message: `Permanently delete ${selectedReplenishments.value.length} selected task(s)?`,
    header: 'Delete Selected Replenishments',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: deleteSelectedReplenishments,
  })
}

const deleteSelectedReplenishments = async () => {
  loading.value = true
  let successCount = 0
  let failCount = 0

  for (const task of selectedReplenishments.value) {
    try {
      await replenishmentApi.delete(task.id)
      successCount++
    } catch (error) {
      console.error(`Failed to delete task ${task.id}:`, error)
      failCount++
    }
  }

  if (failCount === 0) {
    toast.add({ severity: 'success', summary: 'Deleted', detail: `${successCount} task(s) permanently deleted.`, life: 3000 })
  } else {
    toast.add({ severity: 'warn', summary: 'Partial Delete', detail: `${successCount} deleted, ${failCount} failed.`, life: 5000 })
  }

  selectedReplenishments.value = []
  await applyFilters()
  loading.value = false
}

const confirmCancelSelected = () => {
  confirm.require({
    message: `Cancel ${selectedReplenishments.value.length} selected task(s)? This will release the reserved stock.`,
    header: 'Cancel Selected Replenishments',
    icon: 'pi pi-info-circle',
    acceptClass: 'p-button-secondary',
    accept: cancelSelectedReplenishments,
  })
}

const cancelSelectedReplenishments = async () => {
  loading.value = true
  let successCount = 0
  let failCount = 0

  for (const task of selectedReplenishments.value) {
    try {
      await replenishmentApi.cancel(task.id)
      successCount++
    } catch (error) {
      console.error(`Failed to cancel task ${task.id}:`, error)
      failCount++
    }
  }

  if (failCount === 0) {
    toast.add({ severity: 'success', summary: 'Canceled', detail: `${successCount} task(s) canceled and stock released.`, life: 3000 })
  } else {
    toast.add({ severity: 'warn', summary: 'Partial Cancel', detail: `${successCount} canceled, ${failCount} failed.`, life: 5000 })
  }

  selectedReplenishments.value = []
  await applyFilters()
  loading.value = false
}

onMounted(loadData)
</script>