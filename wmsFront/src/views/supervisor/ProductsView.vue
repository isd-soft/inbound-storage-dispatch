<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <div class="flex flex-wrap items-center gap-3">
          <h2 class="app-title text-2xl font-bold">Product Management</h2>
          <Tag severity="secondary" :value="`${products.length} products`" />
        </div>
        <p class="app-subtitle text-sm mt-1">Maintain product master data and category assignments.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="loadProducts" />
        <Button v-if="canManageProducts" label="Add Category" icon="pi pi-folder-plus" severity="secondary" outlined @click="openCategoryDialog" />
        <Button v-if="canManageProducts" label="Add Product" icon="pi pi-plus" severity="success" @click="openCreateDialog" />
      </div>
    </div>

    <Card class="app-card mb-6">
      <template #content>
        <div class="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
          <div class="md:col-span-5 flex flex-col gap-2">
            <label for="productSearch" class="app-subtitle font-medium">Search by name</label>
            <InputText id="productSearch" v-model.trim="filters.name" placeholder="e.g. scanner, label, box" class="w-full" @keyup.enter="applySearch" />
          </div>
          <div class="md:col-span-4 flex flex-col gap-2">
            <label for="categoryFilter" class="app-subtitle font-medium">Category</label>
            <Dropdown
              id="categoryFilter"
              v-model="filters.categoryId"
              :options="categories"
              optionLabel="name"
              optionValue="id"
              placeholder="All categories"
              showClear
              class="w-full"
            />
          </div>
          <div class="md:col-span-3 flex flex-wrap gap-2">
            <Button label="Search" icon="pi pi-search" class="flex-1" :loading="loading" @click="applySearch" />
            <Button label="Clear" icon="pi pi-filter-slash" severity="secondary" outlined class="flex-1" @click="clearSearch" />
          </div>
        </div>
      </template>
    </Card>

    <Card class="app-card">
      <template #content>
        <DataTable
          :value="products"
          :loading="loading"
          paginator
          :rows="10"
          stripedRows
          class="p-datatable-sm"
          dataKey="id"
          emptyMessage="No products found."
        >
          <Column field="id" header="ID" sortable style="width: 5rem"></Column>
          <Column field="name" header="Product" sortable>
            <template #body="slotProps">
              <div class="flex flex-col">
                <span class="app-title font-semibold">{{ slotProps.data.name }}</span>
                <span class="app-muted text-xs">#{{ slotProps.data.id }}</span>
              </div>
            </template>
          </Column>
          <Column field="categoryId" header="Category" sortable>
            <template #body="slotProps">
              <Tag severity="info" :value="categoryName(slotProps.data.categoryId)" />
            </template>
          </Column>
          <Column field="sku" header="SKU" sortable></Column>

          <Column header="Auto Replenish">
            <template #body="slotProps">
              <div v-if="slotProps.data.autoReplenish" class="flex flex-col">
                <Tag severity="success" value="Active" class="mb-1 w-max" />
                <span class="text-xs app-muted">Min: {{ slotProps.data.minThreshold }} | Qty: {{ slotProps.data.replenishQty }}</span>
              </div>
              <span v-else class="app-muted text-sm">Disabled</span>
            </template>
          </Column>

          <Column field="createdAt" header="Created" sortable>
            <template #body="slotProps">
              <span class="app-muted text-sm">{{ formatDate(slotProps.data.createdAt) }}</span>
            </template>
          </Column>
          <Column v-if="canManageProducts" header="Actions" :exportable="false" style="min-width: 10rem">
            <template #body="slotProps">
              <div class="flex flex-wrap gap-2">
                <Button icon="pi pi-pencil" label="Edit" size="small" outlined @click="openEditDialog(slotProps.data)" />
                <Button icon="pi pi-trash" label="Delete" size="small" severity="danger" outlined @click="confirmDelete(slotProps.data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'Add Product' : 'Edit Product'"
      modal
      class="w-full max-w-2xl"
      @hide="resetForm"
    >
      <form class="flex flex-col gap-4 mt-2" @submit.prevent="submitProduct">

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="flex flex-col gap-2">
            <label for="productName" class="app-subtitle font-medium">Product Name *</label>
            <InputText id="productName" v-model.trim="form.name" placeholder="Product name" class="w-full" autofocus />
            <small v-if="submitted && !form.name" class="text-red-500">Product name is required.</small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="productSku" class="app-subtitle font-medium">SKU *</label>
            <div class="flex gap-2">
              <InputText id="productSku" v-model.trim="form.sku" placeholder="Product SKU" class="w-full" />
              <Button icon="pi pi-camera" severity="secondary" outlined aria-label="Scan SKU barcode" type="button" @click="scannerVisible = true" />
            </div>
            <small v-if="submitted && !form.sku" class="text-red-500">SKU is required.</small>
          </div>
        </div>

        <div class="flex flex-col gap-2">
          <label for="productCategory" class="app-subtitle font-medium">Category *</label>
          <div class="flex gap-2">
            <Dropdown id="productCategory" v-model="form.categoryId" :options="categories" optionLabel="name" optionValue="id" placeholder="Select category" class="w-full" filter />
            <Button icon="pi pi-plus" severity="secondary" outlined aria-label="Create category" type="button" @click="openCategoryDialog" />
          </div>
          <small v-if="submitted && !form.categoryId" class="text-red-500">Category is required.</small>
        </div>

        <div class="p-4 border border-surface-200 dark:border-surface-700 rounded-lg bg-surface-50 dark:bg-surface-900 mt-2">
          <div class="flex items-center gap-3 mb-4">
            <InputSwitch inputId="autoReplenish" v-model="form.autoReplenish" />
            <label for="autoReplenish" class="font-semibold cursor-pointer">Enable Auto-Replenishment</label>
          </div>

          <div v-if="form.autoReplenish" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="flex flex-col gap-2">
              <label class="app-subtitle text-sm font-medium">Min Threshold (Trigger)</label>
              <InputNumber v-model="form.minThreshold" :min="0" placeholder="e.g. 5" class="w-full" />
              <small class="app-muted text-xs">When total stock falls below this, a task is created.</small>
              <small v-if="submitted && form.autoReplenish && form.minThreshold === null" class="text-red-500">Required</small>
            </div>
            <div class="flex flex-col gap-2">
              <label class="app-subtitle text-sm font-medium">Replenishment Quantity</label>
              <InputNumber v-model="form.replenishQty" :min="1" placeholder="e.g. 20" class="w-full" />
              <small class="app-muted text-xs">How many items to bring from storage.</small>
              <small v-if="submitted && form.autoReplenish && !form.replenishQty" class="text-red-500">Required (>0)</small>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-2">
          <label for="productDescription" class="app-subtitle font-medium">Description</label>
          <Textarea id="productDescription" v-model.trim="form.description" placeholder="Short operational description" rows="2" class="w-full" />
        </div>
      </form>

      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="actionLoading" @click="closeDialog" />
        <Button :label="dialogMode === 'create' ? 'Create Product' : 'Save Changes'" :loading="actionLoading" @click="submitProduct" />
      </template>
    </Dialog>

    <Dialog v-model:visible="scannerVisible" header="Scan SKU Barcode" modal class="w-full max-w-lg">
      <BarcodeScanner @detected="handleSkuDetected" />
      <p class="app-muted text-sm mt-3">Point the camera at a barcode. The SKU field will be filled automatically after detection.</p>
    </Dialog>

    <Dialog v-model:visible="categoryDialogVisible" header="Create Category" modal class="w-full max-w-md" @hide="resetCategoryForm">
      <form class="flex flex-col gap-2" @submit.prevent="submitCategory">
        <label for="categoryName" class="app-subtitle font-medium">Category Name</label>
        <InputText id="categoryName" v-model.trim="categoryForm.name" placeholder="e.g. Packaging" class="w-full" autofocus />
        <small v-if="categorySubmitted && !categoryForm.name" class="app-danger">Category name is required.</small>
      </form>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="categoryLoading" @click="categoryDialogVisible = false" />
        <Button label="Create Category" icon="pi pi-check" :loading="categoryLoading" @click="submitCategory" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'

import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import InputSwitch from 'primevue/inputswitch'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import Toast from 'primevue/toast'

import BarcodeScanner from '@/components/BarcodeScanner.vue'
import { productApi } from '@/api/productApi'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const products = ref([])
const categories = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)
const scannerVisible = ref(false)
const categoryDialogVisible = ref(false)
const categoryLoading = ref(false)
const dialogMode = ref('create')
const selectedProductId = ref(null)
const submitted = ref(false)
const categorySubmitted = ref(false)

const filters = reactive({
  name: '',
  categoryId: null
})

const form = reactive({
  name: '',
  sku: '',
  description: '',
  categoryId: null,
  autoReplenish: false,
  minThreshold: null,
  replenishQty: null
})

const categoryForm = reactive({ name: '' })

const canManageProducts = computed(() => authStore.hasAnyRole(['ROLE_SUPERVISOR', 'ROLE_DEV']))

const getErrorMessage = (error) => error.response?.data?.message || error.response?.data?.error || error.message || 'Request failed.'
const categoryName = (categoryId) => categories.value.find((c) => c.id === categoryId)?.name || `Category #${categoryId}`

const formatDate = (dateValue) => {
  if (!dateValue) return '-'
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(dateValue))
}

const loadCategories = async () => {
  const response = await productApi.getCategories()
  categories.value = response.data
}

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await productApi.getAllProducts()
    products.value = response.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Products load failed', detail: getErrorMessage(error), life: 4000 })
  } finally {
    loading.value = false
  }
}

const applySearch = async () => {
  const name = filters.name?.trim()
  const categoryId = filters.categoryId

  if (!name && !categoryId) {
    await loadProducts()
    return
  }

  loading.value = true
  try {
    const response = await productApi.searchProducts({ ...(name ? { name } : {}), ...(categoryId ? { categoryId } : {}) })
    products.value = response.data
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Search failed',
      detail: getErrorMessage(error),
      life: 4000
    })
  } finally {
    loading.value = false
  }
}

const clearSearch = async () => {
  filters.name = ''
  filters.categoryId = null
  await loadProducts()
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  selectedProductId.value = null
  resetForm()
  dialogVisible.value = true
}

const openCategoryDialog = () => {
  resetCategoryForm()
  categoryDialogVisible.value = true
}

const openEditDialog = (product) => {
  dialogMode.value = 'edit'
  selectedProductId.value = product.id
  form.name = product.name || ''
  form.sku = product.sku || ''
  form.description = product.description || ''
  form.categoryId = product.categoryId || null
  form.autoReplenish = product.autoReplenish || false
  form.minThreshold = product.minThreshold ?? null
  form.replenishQty = product.replenishQty ?? null
  submitted.value = false
  dialogVisible.value = true
}

const closeDialog = () => {
  scannerVisible.value = false
  dialogVisible.value = false
}

const resetForm = () => {
  form.name = ''
  form.sku = ''
  form.description = ''
  form.categoryId = null
  form.autoReplenish = false
  form.minThreshold = null
  form.replenishQty = null
  submitted.value = false
}

const resetCategoryForm = () => {
  categoryForm.name = ''
  categorySubmitted.value = false
}

const handleSkuDetected = (sku) => {
  form.sku = sku
  scannerVisible.value = false
  toast.add({
    severity: 'success',
    summary: 'Barcode scanned',
    detail: `SKU set to ${sku}`,
    life: 2500
  })
}

const submitCategory = async () => {
  categorySubmitted.value = true
  if (!categoryForm.name) return

  categoryLoading.value = true
  try {
    const response = await productApi.createCategory({ name: categoryForm.name })
    await loadCategories()
    form.categoryId = response.data?.id ?? form.categoryId
    categoryDialogVisible.value = false
    toast.add({
      severity: 'success',
      summary: 'Category created',
      detail: response.data?.name || categoryForm.name,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Category creation failed',
      detail: getErrorMessage(error),
      life: 4000
    })
  } finally {
    categoryLoading.value = false
  }
}

const submitProduct = async () => {
  submitted.value = true

  // Дополнительная валидация для автопополнения
  const isAutoValid = !form.autoReplenish || (form.minThreshold !== null && form.replenishQty !== null && form.replenishQty > 0)

  if (!form.name || !form.sku || !form.categoryId || !isAutoValid) return

  actionLoading.value = true
  const payload = {
    name: form.name,
    sku: form.sku,
    description: form.description || null,
    categoryId: form.categoryId,
    autoReplenish: form.autoReplenish,
    minThreshold: form.autoReplenish ? form.minThreshold : null,
    replenishQty: form.autoReplenish ? form.replenishQty : null
  }

  try {
    if (dialogMode.value === 'create') {
      await productApi.createProduct(payload)
      toast.add({ severity: 'success', summary: 'Product created', detail: form.name, life: 3000 })
    } else {
      await productApi.updateProduct(selectedProductId.value, payload)
      toast.add({ severity: 'success', summary: 'Product updated', detail: form.name, life: 3000 })
    }
    dialogVisible.value = false
    await applySearch()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getErrorMessage(error),
      life: 4000
    })
  } finally {
    actionLoading.value = false
  }
}

const confirmDelete = (product) => {
  confirm.require({
    message: `Delete product "${product.name}"? This action cannot be undone.`,
    header: 'Delete Product',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteProduct(product)
  })
}

const deleteProduct = async (product) => {
  actionLoading.value = true
  try {
    await productApi.deleteProduct(product.id)
    toast.add({ severity: 'success', summary: 'Product deleted', detail: product.name, life: 3000 })
    await applySearch()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Delete failed',
      detail: getErrorMessage(error),
      life: 4000
    })
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadCategories(), loadProducts()])
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Products setup failed',
      detail: getErrorMessage(error),
      life: 4000
    })
  } finally {
    loading.value = false
  }
})
</script>
