<template>
  <div class="p-6">
    <Toast position="top-right" />
    <ConfirmDialog />

    <Message v-if="loadError" severity="error" class="mb-6" :closable="false">
      {{ loadError }}
    </Message>

    <AppDataTable
      v-model:selection="selectedOrders"
      :value="orders"
      :loading="loading"
      :filterFields="orderFilterFields"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 25, 50]"
      v-model:expandedRows="expandedRows"
      stripedRows
      class="p-datatable-sm"
      dataKey="order.id"
      emptyMessage="No orders found."
    >
      <template #toolbar>
        <Button
          icon="pi pi-refresh"
          size="small"
          severity="secondary"
          outlined
          :loading="loading"
          aria-label="Refresh"
          @click="loadOrders"
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
          label="Delete Selected"
          icon="pi pi-trash"
          severity="danger"
          outlined
          :disabled="!selectedOrders.length"
          @click="confirmDeleteSelectedOrders"
        />
        <span v-if="editMode" class="app-muted text-sm">{{ selectedOrders.length }} selected</span>
      </template>
      <Column v-if="editMode" selectionMode="multiple" headerStyle="width: 3rem" />
      <Column expander style="width: 3rem">
        <template #body="{ data, rowTogglerCallback }">
          <Button
            v-if="hasExpandableLines(data)"
            :icon="isExpanded(data) ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
            text
            rounded
            size="small"
            aria-label="Toggle order lines"
            @click="toggleOrderExpansion(data, rowTogglerCallback, $event)"
          />
        </template>
      </Column>
      <Column field="order.logicId" header="Logic ID" sortable filter />
      <Column header="Destination" sortable>
        <template #body="{ data }">
          {{ getLocationLabel(data.order.destinationLocationId) }}
        </template>
      </Column>
      <Column header="Lines" style="width: 7rem">
        <template #body="{ data }">
          <Tag
            severity="info"
            :value="`${data.lines?.length || 0} line${(data.lines?.length || 0) === 1 ? '' : 's'}`"
          />
        </template>
      </Column>
      <Column header="Total Qty" style="width: 8rem">
        <template #body="{ data }">
          <span class="font-semibold">{{ getOrderQuantity(data) }}</span>
        </template>
      </Column>
      <Column header="Status" sortable>
        <template #body="{ data }">
          <Tag
            :severity="getStatusSeverity(data.order.status || data.order.Status)"
            :value="data.order.status || data.order.Status || 'CREATED'"
          />
        </template>
      </Column>
      <!-- Inline editing is intentionally not enabled here because orders contain nested lines and assignment changes backend task ownership. Operator assignment stays row-scoped by business design. -->
      <Column header="Assigned Operator" style="min-width: 14rem">
        <template #body="{ data }">
          <Dropdown
            v-model="assignmentByOrderId[data.order.id]"
            :options="operators"
            optionLabel="username"
            optionValue="id"
            placeholder="Select operator"
            filter
            class="w-full"
            :disabled="isAssignmentLocked(data.order)"
            @change="assignOrderToOperator(data.order.id, assignmentByOrderId[data.order.id])"
          />
        </template>
      </Column>
      <Column field="order.createdAt" header="Created" sortable filter>
        <template #body="{ data }">
          {{ formatDate(data.order.createdAt) }}
        </template>
      </Column>
      <template #expansion="{ data }">
        <div class="order-lines-expansion">
          <AppDataTable
            :value="data.lines"
            :filterFields="orderLineFilterFields"
            :showSearch="false"
            :showPaginator="false"
            class="p-datatable-sm order-lines-table"
            dataKey="orderLineId"
            emptyMessage="No order lines found."
          >
            <Column field="productId" header="Product" filter>
              <template #body="{ data: line }">
                <ProductLink
                  :product-id="line.productId"
                  :barcode="getProduct(line.productId)?.barcode"
                  :name="getProduct(line.productId)?.name || String(line.productId || '-')"
                  class="font-semibold"
                />
              </template>
            </Column>
            <Column field="requestedQuantity" header="Requested Qty" filter>
              <template #body="{ data: line }">
                <span class="font-semibold">{{ line.requestedQuantity ?? line.quantity ?? 0 }}</span>
              </template>
            </Column>
            <Column field="deliveredQuantity" header="Delivered Qty" filter>
              <template #body="{ data: line }">
                <span class="font-semibold">{{ line.deliveredQuantity ?? line.allocatedQuantity ?? 0 }}</span>
              </template>
            </Column>
            <Column field="status" header="Status" filter>
              <template #body="{ data: line }">
                <Tag :severity="getStatusSeverity(line.status)" :value="line.status || 'CREATED'" />
              </template>
            </Column>
          </AppDataTable>
        </div>
      </template>
    </AppDataTable>

    <Dialog
      v-model:visible="createDialogVisible"
      header="Create Order"
      :modal="true"
      class="w-full max-w-5xl"
    >
      <div class="flex flex-col gap-5">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="field">
            <label for="logicId" class="block text-sm font-medium mb-1">Logic ID</label>
            <InputText
              id="logicId"
              v-model.trim="formData.logicId"
              class="w-full"
              :invalid="submitted && !formData.logicId"
            />
            <small v-if="submitted && !formData.logicId" class="text-red-400"
              >Logic ID is required.</small
            >
          </div>

          <div class="field">
            <label for="location" class="block text-sm font-medium mb-1"
              >Destination Location</label
            >
            <Select
              id="location"
              v-model="formData.location"
              :options="locations"
              optionLabel="locationCode"
              optionValue="id"
              placeholder="Select a location"
              filter
              class="w-full"
              :invalid="submitted && !formData.location"
            />
            <small v-if="submitted && !formData.location" class="text-red-400"
              >Location is required.</small
            >
          </div>
        </div>

        <div class="flex justify-end">
          <Button label="Add Line" icon="pi pi-plus" outlined @click="addLine" />
        </div>
        <AppDataTable
          :value="formData.lines"
          :filterFields="orderCreateLineFilterFields"
          class="p-datatable-sm"
          dataKey="id"
          responsiveLayout="scroll"
          emptyMessage="Add at least one order line."
        >
          <Column header="Product" style="min-width: 18rem">
            <template #body="{ data }">
              <Select
                v-model="data.product"
                :options="products"
                optionLabel="name"
                optionValue="id"
                placeholder="Choose product"
                filter
                class="w-full"
                :invalid="submitted && !data.product"
                @change="data.quantity = 1"
              />
              <small v-if="submitted && !data.product" class="text-red-400"
                >Product is required.</small
              >
            </template>
          </Column>
          <Column header="Available" style="width: 9rem">
            <template #body="{ data }">
              <Tag severity="secondary" :value="getAvailableQuantity(data.product)" />
            </template>
          </Column>
          <Column header="Quantity" style="min-width: 13rem">
            <template #body="{ data }">
              <InputNumber
                v-model="data.quantity"
                :min="1"
                showButtons
                buttonLayout="horizontal"
                class="w-full"
                :invalid="submitted && (!data.quantity || data.quantity < 1)"
              />
              <small v-if="submitted && (!data.quantity || data.quantity < 1)" class="text-red-400"
                >Minimum quantity is 1.</small
              >
            </template>
          </Column>
          <Column header="Actions" style="width: 6rem">
            <template #body="{ index }">
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                aria-label="Remove line"
                @click="removeLine(index)"
              />
            </template>
          </Column>
        </AppDataTable>
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text @click="closeCreateDialog" />
        <Button
          label="Submit Order"
          icon="pi pi-check"
          severity="success"
          :loading="actionLoading"
          @click="onSubmit"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'

import Button from 'primevue/button'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Dropdown from 'primevue/dropdown'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

import { orderApi } from '@/api/orderApi.js'
import { inventoryApi } from '@/api/inventoryApi.js'
import { userApi } from '@/api/userApi'
import { productApi } from '@/api/productApi'

const toast = useToast()
const confirm = useConfirm()

const orders = ref([])
const selectedOrders = ref([])
const expandedRows = ref({})
const editMode = ref(false)

const products = ref([])
const locations = ref([])
const operators = ref([])

const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const createDialogVisible = ref(false)
const submitted = ref(false)

const orderFilterFields = [
  { field: 'order.logicId', label: 'Logic ID' },
  { field: 'order.destinationLocationId', label: 'Destination' },
  { field: 'order.status', label: 'Status' },
  { field: 'order.createdAt', label: 'Created' },
]

const orderLineFilterFields = [
  { field: 'productId', label: 'Product' },
  { field: 'requestedQuantity', label: 'Requested Qty' },
  { field: 'status', label: 'Status' },
]

const orderCreateLineFilterFields = [
  { field: 'product', label: 'Product' },
  { field: 'quantity', label: 'Quantity' },
]

const formData = reactive({
  logicId: '',
  location: null,
  lines: [],
})

let nextLineId = 1

const assignmentByOrderId = reactive({})

const getErrorMessage = (error) =>
  error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'

const normalizeOrder = (order) => ({
  order: order.order || order,
  lines: order.lines || [],
})

const loadOrders = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const response = await orderApi.getAll()
    orders.value = (response.data || []).map(normalizeOrder)
    const currentOrderIds = new Set(orders.value.map((entry) => entry.order?.id).filter(Boolean))
    expandedRows.value = Object.fromEntries(
      Object.entries(expandedRows.value).filter(([orderId, isExpanded]) => isExpanded && currentOrderIds.has(Number(orderId))),
    )

    orders.value.forEach((entry) => {
      assignmentByOrderId[entry.order.id] = entry.order.assignedOperatorId || null
    })
  } catch (error) {
    orders.value = []
    expandedRows.value = {}
    loadError.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

const toggleEditMode = () => {
  editMode.value = !editMode.value
  selectedOrders.value = []
}

const loadOrderCreateData = async () => {
  const [productsResponse, locationsResponse, usersResponse] = await Promise.all([
    productApi.getAllProductsWithQuantityInZone('PICKING'),
    inventoryApi.getLocations(),
    userApi.getAll(),
  ])

  products.value = (productsResponse.data || []).map((p) => ({
    id: p.id,
    name: p.name,
    barcode: p.barcode,
    quantity: Number(p.quantity || 0),
  }))

  locations.value = (locationsResponse.data || [])
    .filter((location) => location.zone === 'DISPATCH')
    .map((location) => ({
      ...location,
      locationCode:
        location.locationCode || location.barcode || location.code || location.location || '',
    }))

  operators.value = (usersResponse.data || []).filter((user) => user.userRole === 'ROLE_OPERATOR')
}

const resetForm = () => {
  formData.logicId = ''
  formData.location = null
  formData.lines = []
  submitted.value = false
  nextLineId = 1
  addLine()
}

const openCreateDialog = async () => {
  resetForm()
  createDialogVisible.value = true

  try {
    await loadOrderCreateData()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Order data load failed',
      detail: getErrorMessage(error),
      life: 4000,
    })
  }
}

const closeCreateDialog = () => {
  createDialogVisible.value = false
  resetForm()
}

const addLine = () => {
  formData.lines.push({
    id: nextLineId++,
    product: null,
    quantity: 1,
  })
}

const removeLine = (index) => {
  formData.lines.splice(index, 1)
}

const getProduct = (productId) => products.value.find((product) => product.id === productId)

const getAvailableQuantity = (productId) => {
  const product = getProduct(productId)
  return Number(product?.quantity ?? 0)
}

const getOrderQuantity = (order) =>
  (order.lines || []).reduce(
    (total, line) => total + Number(line.requestedQuantity ?? line.quantity ?? 0),
    0,
  )

const hasExpandableLines = (order) => (order.lines || []).length > 0

const isExpanded = (order) => Boolean(expandedRows.value[order.order.id])

const toggleOrderExpansion = (order, rowTogglerCallback, event) => {
  if (!order?.order?.id || !hasExpandableLines(order)) return
  rowTogglerCallback?.(event)
}

const getLocationLabel = (locationId) => {
  const location = locations.value.find((item) => Number(item.id) === Number(locationId))
  return location?.locationCode || location?.barcode || locationId || '-'
}

const isAssignmentLocked = (order) =>
  ['IN_PROGRESS', 'COMPLETED', 'CANCELED', 'CANCELLED', 'PARTIALLY_COMPLETED'].includes(order?.status || order?.Status)

const assignOrderToOperator = async (orderId, operatorId) => {
  if (!operatorId) return

  try {
    await orderApi.assign(orderId, operatorId)
    toast.add({
      severity: 'success',
      summary: 'Order assigned',
      detail: `Order #${orderId} assigned to operator.`,
      life: 3000,
    })
    await loadOrders()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Assign failed',
      detail: getErrorMessage(error),
      life: 5000,
    })
  }
}

const confirmDeleteSelectedOrders = () => {
  confirm.require({
    message: `Delete ${selectedOrders.value.length} selected order(s)?`,
    header: 'Delete Selected Orders',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: deleteSelectedOrders,
  })
}

const deleteSelectedOrders = async () => {
  actionLoading.value = true

  try {
    await Promise.all(selectedOrders.value.map((entry) => orderApi.delete(entry.order.id)))

    toast.add({
      severity: 'success',
      summary: 'Orders deleted',
      detail: `${selectedOrders.value.length} order(s) deleted.`,
      life: 3000,
    })

    selectedOrders.value = []
    await loadOrders()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Delete failed',
      detail: getErrorMessage(error),
      life: 5000,
    })
  } finally {
    actionLoading.value = false
  }
}

const getStatusSeverity = (status) =>
  ({
    CREATED: 'info',
    ASSIGNED: 'warning',
    IN_PROGRESS: 'warning',
    ALLOCATED: 'success',
    PARTIALLY_COMPLETED: 'warning',
    SHORTAGE: 'warning',
    COMPLETED: 'success',
    CANCELED: 'danger',
    CANCELLED: 'danger',
  })[status] || 'secondary'

const formatDate = (value) =>
  value
    ? new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value))
    : '-'

const onSubmit = async () => {
  submitted.value = true

  const isTopValid = Boolean(formData.logicId && formData.location)
  const areLinesValid =
    formData.lines.length > 0 && formData.lines.every((line) => line.product && line.quantity >= 1)

  if (!isTopValid || !areLinesValid) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fill in all required fields.',
      life: 4000,
    })
    return
  }

  const payload = {
    order: {
      logicId: formData.logicId,
      destinationLocationId: formData.location,
    },
    lines: formData.lines.map((line) => ({
      orderId: null,
      productId: line.product,
      requestedQuantity: line.quantity,
    })),
  }

  actionLoading.value = true

  try {
    const order = await orderApi.create(payload)

    toast.add({
      severity: 'success',
      summary: `Order submitted with id ${order.data.id}`,
      detail: `${formData.lines.length} line(s) added.`,
      life: 5000,
    })

    closeCreateDialog()
    await loadOrders()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Order creation failed',
      detail: getErrorMessage(error),
      life: 5000,
    })
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOrders(), loadOrderCreateData()])
})
</script>

<style scoped>
.order-lines-expansion {
  animation: order-lines-enter 0.22s ease-out;
  background:
    linear-gradient(
      90deg,
      color-mix(in srgb, var(--brand-primary) 14%, transparent),
      transparent 15rem
    ),
    color-mix(in srgb, var(--surface-ground) 84%, black);
  border-left: 3px solid color-mix(in srgb, var(--brand-primary) 68%, transparent);
  margin: -0.75rem -1rem;
  padding: 0.5rem 0 0.5rem 0.75rem;
  transform-origin: top;
}

.order-lines-table :deep(.p-datatable-table) {
  width: 100%;
}

.order-lines-table :deep(.p-datatable-thead > tr > th),
.order-lines-table :deep(.p-datatable-tbody > tr > td) {
  background: transparent !important;
  border-color: color-mix(in srgb, var(--border-subtle) 55%, transparent);
}

.order-lines-table :deep(.p-datatable-tbody > tr:hover > td) {
  background: color-mix(in srgb, var(--surface-hover) 72%, transparent) !important;
}

@keyframes order-lines-enter {
  from {
    opacity: 0;
    transform: translateY(-0.35rem) scaleY(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scaleY(1);
  }
}
</style>
