<template>
  <div class="p-6">
    <Toast />

    <AppDataTable
      :value="allocations"
      :loading="loading"
      paginator
      :rows="10"
      dataKey="processId"
      stripedRows
      emptyMessage="No active allocations found."
    >
      <template #toolbar>
        <Button
          icon="pi pi-refresh"
          size="small"
          severity="secondary"
          outlined
          :loading="loading"
          @click="loadAllocations"
        />
      </template>

      <Column field="type" header="Task Type" sortable>
        <template #body="{ data }">
          <Tag :value="data.type" :severity="data.type === 'REPLENISHMENT' ? 'info' : 'success'" />
        </template>
      </Column>

      <Column header="Reference">
        <template #body="{ data }">
          <AllocationReferenceLink
            :type="data.type"
            :order-id="data.orderId"
            :replenishment-id="data.replenishmentId"
          />
        </template>
      </Column>

      <Column field="productName" header="Product" sortable filter>
        <template #body="{ data }">
          {{ data.productName }}
        </template>
      </Column>

      <Column field="locationName" header="Location" sortable filter />

      <Column field="quantity" header="Quantity" sortable>
        <template #body="{ data }">
          <span class="font-bold">
            {{ data.quantity }}
          </span>
        </template>
      </Column>

      <Column field="pickedQuantity" header="Picked Qty" sortable>
        <template #body="{ data }">
          <span class="font-bold">
            {{ data.pickedQuantity ?? 0 }}
          </span>
        </template>
      </Column>

      <Column field="sourceLocationScanned" header="Location Scan">
        <template #body="{ data }">
          <i
            class="pi"
            :class="
              data.sourceLocationScanned
                ? 'pi-check-circle text-green-500'
                : 'pi-times-circle text-red-500'
            "
          />
        </template>
      </Column>

      <Column field="productScanned" header="Product Scan">
        <template #body="{ data }">
          <i
            class="pi"
            :class="
              data.productScanned
                ? 'pi-check-circle text-green-500'
                : 'pi-times-circle text-red-500'
            "
          />
        </template>
      </Column>

      <Column field="status" header="Status" sortable>
        <template #body="{ data }">
          <Tag :value="data.status" :severity="statusSeverity(data.status)" />
        </template>
      </Column>
    </AppDataTable>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useToast } from 'primevue/usetoast'

import Button from 'primevue/button'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'

import { allocationApi } from '@/api/allocationApi.js'
import AllocationReferenceLink from '@/components/AllocationReferenceLink.vue'

const toast = useToast()

const allocations = ref([])
const loading = ref(false)

const getErrorMessage = (error) => {
  return (
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    'Request failed.'
  )
}

const statusSeverity = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'IN_PROGRESS':
      return 'warn'
    case 'ASSIGNED':
      return 'info'
    case 'CREATED':
      return 'secondary'
    default:
      return 'contrast'
  }
}

const loadAllocations = async () => {
  loading.value = true

  try {
    const response = await allocationApi.getSupervisorAllocations()
    allocations.value = response.data

  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Allocation load failed',
      detail: getErrorMessage(error),
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadAllocations)
</script>
