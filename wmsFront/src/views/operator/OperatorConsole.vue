<template>
  <div class="app-shell font-sans min-h-screen">
    <header class="app-header flex justify-between items-center px-4 py-3 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-box text-2xl app-warm"></i>
        <h1 class="text-xl font-bold tracking-wide app-title">Operator Console</h1>
      </div>
      <div class="flex items-center gap-2">
        <ThemeToggle />
        <Button icon="pi pi-refresh" text rounded :loading="loading" @click="loadCurrentTask()" />
        <Button icon="pi pi-sign-out" severity="danger" text rounded @click="handleLogout" />
      </div>
    </header>

    <main class="max-w-5xl mx-auto p-4 md:p-6 flex flex-col gap-4">
      <Toast />

      <div v-if="loading" class="app-card rounded-2xl p-8 text-center">
        <ProgressSpinner strokeWidth="4" style="width: 3rem; height: 3rem" />
        <p class="app-subtitle mt-4">Loading assigned task...</p>
      </div>

      <Message v-else-if="loadError" severity="error" :closable="false">
        {{ loadError }}
      </Message>

      <div v-else-if="isEmpty" class="app-card rounded-2xl p-8 text-center">
        <i class="pi pi-inbox text-5xl app-muted"></i>
        <h2 class="app-title text-2xl font-semibold mt-4">No assigned tasks</h2>
        <p class="app-subtitle mt-2">You do not have active tasks right now.</p>
        <Button class="mt-5" icon="pi pi-refresh" label="Refresh" @click="loadCurrentTask()" />
      </div>

      <template v-else-if="summary">
        <Card class="rounded-2xl">
          <template #title>
            <div class="flex items-start justify-between gap-3 flex-wrap">
              <div>
                <div class="text-xl app-title font-semibold">{{ taskExecutionTitle }}</div>
              </div>
            </div>
          </template>
          <template #content>
            <div class="flex flex-col gap-4">
              <Message v-if="actionError" severity="error" :closable="false">
                {{ actionError }}
              </Message>

              <template v-if="currentProcess">
                <div class="app-card-muted rounded-xl p-4 flex items-start justify-between gap-3 flex-wrap">
                  <div>
                    <div class="text-sm app-muted">Current item</div>
                    <div class="text-lg font-semibold app-title">{{ currentProcess.productName }}</div>
                    <div class="text-sm app-muted font-mono">{{ currentProcess.productBarcode || 'No barcode' }}</div>
                  </div>
                  <div class="text-right flex flex-col gap-1">
                    <div>
                      <div class="text-sm app-muted">Required quantity</div>
                      <div class="text-lg font-semibold app-warm">{{ currentProcess.requiredQuantity }}</div>
                    </div>
                    <div v-if="isReplenishmentTask" class="text-sm">
                      <div class="app-muted">Move</div>
                      <div class="font-mono app-title">{{ currentProcess.sourceLocationBarcode }} → {{ currentProcess.destinationLocationBarcode }}</div>
                    </div>
                  </div>
                </div>

                <div v-if="isAwaitingStart" class="app-card rounded-2xl p-6 flex flex-col gap-4">
                  <Message severity="info" :closable="false">
                    The task is assigned. Press Start Task to begin execution.
                  </Message>
                  <div class="flex justify-end">
                    <Button
                      label="Start Task"
                      icon="pi pi-play"
                      :loading="actionLoading"
                      @click="startTask"
                    />
                  </div>
                </div>

                <Stepper v-else :value="activeStep" linear>
                  <StepList>
                    <Step
                      v-for="step in stepDefinitions"
                      :key="step.value"
                      :value="step.value"
                    >
                      {{ step.label }}
                    </Step>
                  </StepList>
                  <StepPanels>
                    <StepPanel
                      v-for="step in stepDefinitions"
                      :key="`panel-${step.value}`"
                      :value="step.value"
                    >
                      <div v-if="step.type === 'location'" class="app-card rounded-2xl p-5 flex flex-col gap-4">
                        <div>
                          <div class="text-sm app-muted">Scan source location</div>
                          <div class="font-mono font-semibold app-brand text-2xl mt-1">
                            {{ step.locationBarcode }}
                          </div>
                        </div>
                        <ScanSection
                          v-model="barcodeInput"
                          :loading="actionLoading"
                          submit-label="Verify Source"
                          placeholder="Scan or enter source barcode"
                          @submit="submitBarcodeStep"
                        />
                      </div>

                      <div v-else-if="step.type === 'product'" class="app-card rounded-2xl p-5 flex flex-col gap-4">
                        <div>
                          <div class="text-sm app-muted">Scan product barcode</div>
                          <div class="font-mono font-semibold app-warm text-2xl mt-1">
                            {{ step.process.productBarcode || 'No barcode' }}
                          </div>
                          <div class="text-sm app-muted mt-2">{{ step.process.productName }}</div>
                        </div>
                        <ScanSection
                          v-model="barcodeInput"
                          :loading="actionLoading"
                          submit-label="Verify Product"
                          placeholder="Scan or enter product barcode"
                          @submit="submitBarcodeStep"
                        />
                      </div>

                      <div v-else-if="step.type === 'quantity'" class="app-card rounded-2xl p-5 flex flex-col gap-4">
                        <div>
                          <div class="text-sm app-muted">Confirm picked quantity</div>
                          <div class="font-semibold app-title mt-1">
                            {{ step.process.productName }} · Required: {{ step.process.requiredQuantity }}
                          </div>
                        </div>
                        <InputNumber
                          v-model="pickedQuantity"
                          :min="1"
                          :max="step.process.requiredQuantity"
                          showButtons
                          fluid
                        />
                        <div class="flex justify-end">
                          <Button
                            label="Confirm Quantity"
                            icon="pi pi-check-circle"
                            :loading="actionLoading"
                            @click="confirmQuantity"
                          />
                        </div>
                      </div>

                      <div v-else-if="step.type === 'complete-move'" class="app-card rounded-2xl p-5 flex flex-col gap-4">
                        <div class="grid md:grid-cols-2 gap-3">
                          <div class="app-card-muted rounded-xl p-4">
                            <div class="text-xs app-muted">Source</div>
                            <div class="font-mono font-semibold app-brand mt-1">
                              {{ step.process.sourceLocationBarcode }}
                            </div>
                          </div>
                          <div class="app-card-muted rounded-xl p-4">
                            <div class="text-xs app-muted">Destination</div>
                            <div class="font-mono font-semibold app-success mt-1">
                              {{ step.process.destinationLocationBarcode }}
                            </div>
                          </div>
                        </div>
                        <div class="app-card-muted rounded-xl p-4">
                          <div class="text-xs app-muted">Confirmed quantity</div>
                          <div class="font-semibold app-title mt-1">
                            {{ step.process.pickedQuantity }} / {{ step.process.requiredQuantity }}
                          </div>
                        </div>
                        <div class="flex justify-end">
                          <Button
                            label="Complete Move"
                            icon="pi pi-check"
                            severity="success"
                            :loading="actionLoading"
                            @click="completeProcess"
                          />
                        </div>
                      </div>

                    </StepPanel>
                  </StepPanels>
                </Stepper>
              </template>

            </div>
          </template>
        </Card>
      </template>
    </main>

    <Dialog
      v-model:visible="summaryDialogVisible"
      modal
      :header="completionSummaryHeader"
      :style="{ width: 'min(960px, 96vw)' }"
    >
      <div v-if="completionSummary" class="flex flex-col gap-4">
        <DataTable
          :value="completionRows"
          stripedRows
          responsiveLayout="scroll"
          class="p-datatable-sm"
          dataKey="rowKey"
          emptyMessage="No summary rows found."
        >
          <Column field="productName" header="Product" />
          <Column field="productBarcode" header="Barcode" />
          <Column field="movedQuantity" :header="completionSummary?.taskType === 'REPLENISHMENT' ? 'Moved' : 'Picked'" />
          <Column field="requiredQuantity" header="Required" />
          <Column field="sourceLocation" header="Source" />
          <Column field="destinationLocation" header="Destination" />
        </DataTable>
      </div>

      <template #footer>
        <Button label="Done" severity="secondary" outlined @click="closeCompletionSummary" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { allocationApi } from '@/api/allocationApi'
import BarcodeScanner from '@/components/BarcodeScanner.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import Step from 'primevue/step'
import StepList from 'primevue/steplist'
import StepPanel from 'primevue/steppanel'
import StepPanels from 'primevue/steppanels'
import Stepper from 'primevue/stepper'
import Toast from 'primevue/toast'

const ScanSection = defineComponent({
  name: 'ScanSection',
  props: {
    modelValue: { type: String, default: '' },
    loading: { type: Boolean, default: false },
    submitLabel: { type: String, required: true },
    placeholder: { type: String, required: true }
  },
  emits: ['update:modelValue', 'submit'],
  setup(props, { emit }) {
    return () =>
      h('div', { class: 'flex flex-col gap-3' }, [
        h('div', { class: 'flex flex-col gap-2' }, [
          h(InputText, {
            modelValue: props.modelValue,
            'onUpdate:modelValue': (value) => emit('update:modelValue', value),
            placeholder: props.placeholder,
            onKeyup: (event) => {
              if (event.key === 'Enter') emit('submit')
            }
          }),
          h(Button, {
            label: props.submitLabel,
            icon: 'pi pi-check',
            loading: props.loading,
            onClick: () => emit('submit')
          })
        ]),
        h(BarcodeScanner, {
          onDetected: (value) => {
            emit('update:modelValue', value)
            emit('submit')
          }
        })
      ])
  }
})

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const loading = ref(true)
const actionLoading = ref(false)
const loadError = ref('')
const actionError = ref('')
const summary = ref(null)
const completionSummary = ref(null)
const pendingCompletionSummary = ref(null)
const nextSummaryAfterCompletion = ref(null)
const barcodeInput = ref('')
const pickedQuantity = ref(1)
const summaryDialogVisible = ref(false)

const isEmpty = computed(() => !loading.value && !loadError.value && !summary.value)
const currentProcess = computed(() => summary.value?.currentProcess || null)
const orderedProcesses = computed(() => summary.value?.processes || [])
const isPickingTask = computed(() => summary.value?.taskType === 'PICKING_ORDER')
const isReplenishmentTask = computed(() => summary.value?.taskType === 'REPLENISHMENT')
const taskExecutionTitle = computed(() => {
  if (isReplenishmentTask.value) return 'Replenishment Task Execution'
  return 'Picking Task Execution'
})
const completionSummaryHeader = computed(() => {
  if (completionSummary.value?.taskType === 'REPLENISHMENT') return 'Replenishment'
  return 'Picking Order'
})
const completionRows = computed(() => {
  if (!completionSummary.value) return []

  if (completionSummary.value.taskType === 'REPLENISHMENT') {
    return (completionSummary.value.processes || []).map((process) => ({
      rowKey: process.processId,
      productName: process.productName || 'N/A',
      productBarcode: process.productBarcode || 'N/A',
      movedQuantity: process.pickedQuantity ?? process.requiredQuantity ?? 0,
      requiredQuantity: process.requiredQuantity ?? 0,
      sourceLocation: process.sourceLocationBarcode || 'N/A',
      destinationLocation: process.destinationLocationBarcode || 'N/A'
    }))
  }

  return (completionSummary.value.orderLines || []).map((line) => ({
    rowKey: line.orderLineId,
    productName: line.productName || 'N/A',
    productBarcode: line.productBarcode || 'N/A',
    movedQuantity: line.pickedQuantity ?? 0,
    requiredQuantity: line.requiredQuantity ?? 0,
    sourceLocation: (line.sourceLocationBarcodes || []).join(', ') || 'N/A',
    destinationLocation: line.destinationLocationBarcode || 'N/A'
  }))
})
const isAwaitingStart = computed(() => currentProcess.value?.status === 'ASSIGNED')
const stepDefinitions = computed(() => {
  if (isReplenishmentTask.value) {
    if (!currentProcess.value) return []
    return [
      {
        value: 1,
        type: 'location',
        label: `Source ${currentProcess.value.sourceLocationBarcode}`,
        processId: currentProcess.value.processId,
        locationBarcode: currentProcess.value.sourceLocationBarcode
      },
      {
        value: 2,
        type: 'product',
        label: currentProcess.value.productName || 'Product',
        processId: currentProcess.value.processId,
        process: currentProcess.value
      },
      {
        value: 3,
        type: 'quantity',
        label: `Qty ${currentProcess.value.productName || ''}`.trim(),
        processId: currentProcess.value.processId,
        process: currentProcess.value
      },
      {
        value: 4,
        type: 'complete-move',
        label: 'Complete Move',
        processId: currentProcess.value.processId,
        process: currentProcess.value
      }
    ]
  }

  const processes = orderedProcesses.value
  const steps = []
  let value = 1
  let previousLocation = null

  for (const process of processes) {
    const currentLocation = process.sourceLocationBarcode
    if (currentLocation !== previousLocation) {
      steps.push({
        value: value++,
        type: 'location',
        label: `Location ${currentLocation}`,
        processId: process.processId,
        locationBarcode: currentLocation
      })
      previousLocation = currentLocation
    }

    steps.push({
      value: value++,
      type: 'product',
      label: process.productName || 'Product',
      processId: process.processId,
      process
    })
    steps.push({
      value: value++,
      type: 'quantity',
      label: `Qty ${process.productName || ''}`.trim(),
      processId: process.processId,
      process
    })
  }

  return steps
})
const activeStep = computed(() => {
  if (isReplenishmentTask.value) {
    if (!currentProcess.value) return 1
    if (!currentProcess.value.sourceLocationScanned) return 1
    if (!currentProcess.value.productScanned) return 2
    if (currentProcess.value.pickedQuantity == null) return 3
    return 4
  }
  if (!currentProcess.value) return 1

  const processId = currentProcess.value.processId
  if (!currentProcess.value.sourceLocationScanned) {
    return stepDefinitions.value.find((step) => step.processId === processId && step.type === 'location')?.value || 1
  }
  if (!currentProcess.value.productScanned) {
    return stepDefinitions.value.find((step) => step.processId === processId && step.type === 'product')?.value || 1
  }
  if (currentProcess.value.pickedQuantity == null) {
    return stepDefinitions.value.find((step) => step.processId === processId && step.type === 'quantity')?.value || 1
  }
  return stepDefinitions.value.find((step) => step.processId === processId && step.type === 'quantity')?.value || 1
})
const getErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback

const cloneSummary = (payload) => JSON.parse(JSON.stringify(payload))

const buildFinalSummarySnapshot = () => {
  if (!summary.value) return null

  const snapshot = cloneSummary(summary.value)

  if (snapshot.taskType === 'PICKING_ORDER') {
    snapshot.orderStatus = 'COMPLETED'
    snapshot.readyForCompletion = true
    snapshot.completedProcesses = snapshot.totalProcesses
    snapshot.currentProcess = snapshot.currentProcess
      ? {
          ...snapshot.currentProcess,
          status: 'COMPLETED',
          pickedQuantity: snapshot.currentProcess.requiredQuantity ?? snapshot.currentProcess.pickedQuantity
        }
      : null
    snapshot.orderLines = (snapshot.orderLines || []).map((line) => ({
      ...line,
      pickedQuantity: line.requiredQuantity ?? line.pickedQuantity ?? 0,
      status: 'COMPLETED'
    }))
    snapshot.processes = (snapshot.processes || []).map((process) => ({
      ...process,
      pickedQuantity: process.requiredQuantity ?? process.pickedQuantity ?? 0,
      status: 'COMPLETED'
    }))
    return snapshot
  }

  snapshot.completedProcesses = snapshot.totalProcesses
  snapshot.currentProcess = snapshot.currentProcess
    ? {
        ...snapshot.currentProcess,
        status: 'COMPLETED',
        pickedQuantity: snapshot.currentProcess.requiredQuantity ?? snapshot.currentProcess.pickedQuantity
      }
    : null
  snapshot.processes = (snapshot.processes || []).map((process) => ({
    ...process,
    pickedQuantity: process.requiredQuantity ?? process.pickedQuantity ?? 0,
    status: 'COMPLETED'
  }))

  return snapshot
}

const queueCompletionSummary = () => {
  if (summary.value) {
    pendingCompletionSummary.value = buildFinalSummarySnapshot()
  }
}

const closeCompletionSummary = async () => {
  const nextSummary = nextSummaryAfterCompletion.value
  completionSummary.value = null
  pendingCompletionSummary.value = null
  nextSummaryAfterCompletion.value = null
  summaryDialogVisible.value = false

  if (nextSummary) {
    hydrateState(nextSummary)
    return
  }

  await loadCurrentTask()
}

const hydrateState = (payload) => {
  summary.value = payload
  actionError.value = ''
  barcodeInput.value = ''
  pickedQuantity.value = payload?.currentProcess?.pickedQuantity ?? payload?.currentProcess?.requiredQuantity ?? 1
}

const loadCurrentTask = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const response = await allocationApi.getCurrentTaskSummary()
    if (response.status === 204 || !response.data) {
      summary.value = null
      if (pendingCompletionSummary.value) {
        completionSummary.value = pendingCompletionSummary.value
        pendingCompletionSummary.value = null
        summaryDialogVisible.value = true
      } else if (!completionSummary.value) {
        summaryDialogVisible.value = false
      }
      nextSummaryAfterCompletion.value = null
      return
    }

    if (pendingCompletionSummary.value) {
      completionSummary.value = pendingCompletionSummary.value
      nextSummaryAfterCompletion.value = cloneSummary(response.data)
      pendingCompletionSummary.value = null
      summary.value = null
      summaryDialogVisible.value = true
      return
    }

    hydrateState(response.data)
    pendingCompletionSummary.value = null
    nextSummaryAfterCompletion.value = null
  } catch (error) {
    if (error?.response?.status === 204) {
      summary.value = null
      if (pendingCompletionSummary.value) {
        completionSummary.value = pendingCompletionSummary.value
        pendingCompletionSummary.value = null
        summaryDialogVisible.value = true
      } else if (!completionSummary.value) {
        summaryDialogVisible.value = false
      }
      nextSummaryAfterCompletion.value = null
      return
    }
    loadError.value = getErrorMessage(error, 'Failed to load operator task.')
    summary.value = null
  } finally {
    loading.value = false
  }
}

const startTask = async () => {
  actionLoading.value = true
  actionError.value = ''

  try {
    const response = await allocationApi.startCurrentTask()
    hydrateState(response.data)
    toast.add({ severity: 'success', summary: 'Task started', life: 2500 })
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Failed to start task.')
  } finally {
    actionLoading.value = false
  }
}

const submitBarcodeStep = async () => {
  if (!currentProcess.value || !barcodeInput.value?.trim()) {
    actionError.value = 'Barcode is required.'
    return
  }

  actionLoading.value = true
  actionError.value = ''

  try {
    if (!currentProcess.value.sourceLocationScanned) {
      await allocationApi.scanSourceLocation(currentProcess.value.processId, barcodeInput.value.trim())
      toast.add({ severity: 'success', summary: 'Source verified', life: 2500 })
    } else if (!currentProcess.value.productScanned) {
      await allocationApi.scanProduct(currentProcess.value.processId, barcodeInput.value.trim())
      toast.add({ severity: 'success', summary: 'Product verified', life: 2500 })
    }

    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Barcode validation failed.')
  } finally {
    actionLoading.value = false
  }
}

const confirmQuantity = async () => {
  if (!currentProcess.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    const processId = currentProcess.value.processId
    await allocationApi.confirmPickedQuantity(processId, pickedQuantity.value)
    if (isPickingTask.value) {
      const completionResponse = await allocationApi.completeAssignedAllocation(processId)
      if (completionResponse.data?.status === 'COMPLETED') {
        queueCompletionSummary()
      }
      toast.add({ severity: 'success', summary: 'Product confirmed', life: 2500 })
    } else {
      toast.add({ severity: 'success', summary: 'Quantity confirmed', life: 2500 })
    }
    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Invalid picked quantity.')
  } finally {
    actionLoading.value = false
  }
}

const completeProcess = async () => {
  if (!currentProcess.value) return

  actionLoading.value = true
  actionError.value = ''

  try {
    const completionResponse = await allocationApi.completeAssignedAllocation(currentProcess.value.processId)
    if (isReplenishmentTask.value || completionResponse.data?.status === 'COMPLETED') {
      queueCompletionSummary()
    }
    toast.add({ severity: 'success', summary: isReplenishmentTask.value ? 'Move completed' : 'Process completed', life: 2500 })
    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Failed to complete process.')
  } finally {
    actionLoading.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}

onMounted(() => {
  loadCurrentTask()
})
</script>
