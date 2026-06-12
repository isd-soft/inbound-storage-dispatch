<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog />

    <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-gray-100">Warehouse Locations</h2>
        <p class="text-sm text-gray-400 mt-1">Manage physical storage zones and picking racks.</p>
      </div>
      <div class="flex gap-2">
        <Button
          label="Create Location"
          icon="pi pi-plus"
          severity="success"
          @click="openCreateDialog"
        />
        <Button
          label="Refresh"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          :loading="loading"
          @click="loadLocations"
        />
      </div>
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable
          :value="locations"
          :loading="loading"
          paginator
          :rows="10"
          stripedRows
          class="p-datatable-sm"
          dataKey="id"
          emptyMessage="No locations found."
        >
          <Column field="id" header="ID" sortable></Column>

          <Column field="barcode" header="Code" sortable>
            <template #body="{ data }">
              <span class="font-bold text-blue-400">{{ data.barcode }}</span>
            </template>
          </Column>

          <Column field="zone" header="Zone" sortable></Column>

          <Column field="description" header="Description"></Column>

          <Column header="Status" sortable>
            <template #body="{ data }">
              <Tag
                :severity="data.available ? 'success' : 'danger'"
                :value="data.available ? 'AVAILABLE' : 'DISABLED'"
              />
            </template>
          </Column>

          <Column header="Actions" style="min-width: 8rem">
            <template #body="{ data }">
              <div class="flex gap-2">
                <Button
                  icon="pi pi-pencil"
                  outlined
                  rounded
                  severity="warning"
                  size="small"
                  @click="openEditDialog(data)"
                />
                <Button
                  icon="pi pi-trash"
                  outlined
                  rounded
                  severity="danger"
                  size="small"
                  @click="confirmDelete(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialogVisible"
      :header="isEditing ? 'Edit Location' : 'Create Location'"
      :modal="true"
      class="p-fluid w-full max-w-md"
    >
      <div class="field mb-4">
        <label for="barcode" class="block text-sm font-medium mb-1">Location Code *</label>
        <InputText
          id="barcode"
          v-model="formData.barcode"
          placeholder="e.g., PICK-A-01"
          required
          autofocus
        />
      </div>

      <div class="field mb-4">
        <label for="zone" class="block text-sm font-medium mb-1">Zone *</label>
        <Dropdown id="zone" v-model="formData.zone" :options="zones" placeholder="Select Zone" />
      </div>

      <div class="field mb-4">
        <label for="description" class="block text-sm font-medium mb-1">Description</label>
        <Textarea
          id="description"
          v-model="formData.description"
          rows="2"
          placeholder="Optional description..."
        />
      </div>

      <div class="field mb-4 flex items-center gap-2" v-if="isEditing">
        <InputSwitch inputId="available" v-model="formData.available" />
        <label for="available" class="text-sm font-medium">Location is Available</label>
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text @click="dialogVisible = false" />
        <Button
          :label="isEditing ? 'Save' : 'Create'"
          icon="pi pi-check"
          severity="success"
          :loading="actionLoading"
          @click="saveLocation"
          :disabled="!isFormValid"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

// PrimeVue Компоненты
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Dropdown from 'primevue/dropdown'
import InputSwitch from 'primevue/inputswitch'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'

import { locationApi } from '@/api/locationApi'

const toast = useToast()
const confirm = useConfirm()

const locations = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)

const formData = ref({
  id: null,
  barcode: '',
  zone: null,
  description: '',
  available: true,
})

const zones = ref(['PICKING', 'REPLENISHMENT', 'DISPATCH'])

const isFormValid = computed(() => {
  return formData.value.barcode?.trim() && formData.value.zone
})

const loadLocations = async () => {
  loading.value = true
  try {
    const res = await locationApi.getAll()
    locations.value = res.data
  } catch (error) {
    showError('Failed to load locations', error)
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEditing.value = false
  formData.value = { id: null, barcode: '', zone: null, description: '', available: true }
  dialogVisible.value = true
}

const openEditDialog = (data) => {
  isEditing.value = true
  formData.value = { ...data }
  dialogVisible.value = true
}

const saveLocation = async () => {
  actionLoading.value = true
  try {
    const payload = {
      name: formData.value.barcode,
      barcode: formData.value.barcode,
      zone: formData.value.zone,
      description: formData.value.description,
      available: formData.value.available,
    }

    if (isEditing.value) {
      await locationApi.update(formData.value.id, payload)
      toast.add({ severity: 'success', summary: 'Success', detail: 'Location updated', life: 3000 })
    } else {
      await locationApi.create(payload)
      toast.add({ severity: 'success', summary: 'Success', detail: 'Location created', life: 3000 })
    }

    dialogVisible.value = false
    await loadLocations()
  } catch (error) {
    showError(isEditing.value ? 'Update failed' : 'Creation failed', error)
  } finally {
    actionLoading.value = false
  }
}

const confirmDelete = (location) => {
  confirm.require({
    message: `Are you sure you want to delete ${location.barcode}?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await locationApi.delete(location.id)
        toast.add({
          severity: 'success',
          summary: 'Deleted',
          detail: 'Location deleted',
          life: 3000,
        })
        await loadLocations()
      } catch (error) {
        showError('Deletion failed', error)
      }
    },
  })
}

const showError = (summary, error) => {
  const detail =
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    'An error occurred'
  toast.add({ severity: 'error', summary, detail, life: 5000 })
}

onMounted(() => {
  loadLocations()
})
</script>
