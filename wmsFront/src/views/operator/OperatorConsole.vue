<template>
  <div class="app-shell font-sans min-h-screen flex flex-col items-center w-full overflow-x-hidden">
    <header class="app-header w-full flex justify-between items-center px-3 py-2 shadow-md">
      <div class="flex items-center gap-2">
        <i class="pi pi-box text-xl app-warm"></i>
        <h1 class="text-lg font-bold tracking-wide app-title">Operator Console</h1>
      </div>
      <div class="flex items-center gap-1">
        <ThemeToggle />
        <Button icon="pi pi-refresh" text rounded size="small" :loading="loading" @click="forceRefreshTask" />
        <Button icon="pi pi-sign-out" severity="danger" text rounded size="small" @click="handleLogout" />
      </div>
    </header>

    <main class="w-full max-w-xl p-2 sm:p-4 flex flex-col gap-3 text-center items-center justify-center box-border">
      <Toast />

      <div v-if="loading" class="app-card w-full rounded-2xl p-6 text-center flex flex-col items-center justify-center">
        <ProgressSpinner strokeWidth="4" style="width: 2.5rem; height: 2.5rem" />
        <p class="app-subtitle mt-3 text-sm">Loading assigned task...</p>
      </div>

      <Message v-else-if="loadError" class="w-full text-xs" severity="error" :closable="false">
        {{ loadError }}
      </Message>

      <!-- ECRAN FINAL DE SUMMARY: Afișat când showFinalSummary devine true (după completarea ultimei linii) -->
      <div v-else-if="showFinalSummary" class="app-card rounded-2xl w-full text-left shadow-md p-3 sm:p-5 flex flex-col gap-4 box-border">
        <div class="text-center px-2 py-1 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
          <div class="text-base font-bold tracking-wide py-1 flex items-center justify-center gap-2">
            <i class="pi pi-shopping-bag"></i>
            Picking Order Task
          </div>
        </div>

        <div class="w-full bg-gray-50 dark:bg-zinc-800/50 rounded-xl p-3 sm:p-4 border border-gray-100 dark:border-zinc-700/50 flex flex-col gap-3">
          <div class="flex justify-between items-center border-b border-gray-200/50 dark:border-zinc-700/50 pb-2">
            <div class="text-[11px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5">
              <i class="pi pi-check-circle text-sm"></i> Order Completion Summary
            </div>
            <span class="text-[10px] px-2 py-0.5 rounded-full font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300">
              COMPLETED
            </span>
          </div>

          <div class="flex flex-col gap-2 max-h-[260px] overflow-y-auto pr-1">
            <div
              v-for="(proc, index) in finalAllocationsSummary"
              :key="proc.allocationId || index"
              class="flex flex-col gap-1 p-2.5 bg-white dark:bg-zinc-800 rounded-lg border border-gray-100 dark:border-zinc-700 text-xs shadow-sm"
            >
              <div class="flex justify-between items-start gap-2">
                <span class="font-bold text-gray-800 dark:text-zinc-200 line-clamp-2 flex-1">
                  {{ proc.productName || 'Unknown Product' }}
                </span>
                <span class="font-mono text-[10px] text-gray-400 dark:text-zinc-500 whitespace-nowrap">
                  #{{ proc.productBarcode || 'No EAN' }}
                </span>
              </div>

              <div class="flex justify-between items-center mt-1 pt-1.5 border-t border-dashed border-gray-100 dark:border-zinc-700/50">
                <div class="flex items-center gap-1.5 font-mono text-[11px]">
                  <span class="px-1.5 py-0.5 bg-gray-100 dark:bg-zinc-700 rounded text-gray-600 dark:text-zinc-300 font-semibold">
                    {{ proc.sourceLocationBarcode || '???' }}
                  </span>
                </div>

                <div class="text-right whitespace-nowrap">
                  <span class="text-gray-400 dark:text-zinc-500 text-[11px]">Picked: </span>
                  <strong class="text-sm text-emerald-600 dark:text-emerald-400 font-extrabold">
                    {{ proc.pickedQuantity ?? proc.requiredQuantity }}
                  </strong>
                  <span class="text-[10px] text-gray-400 font-normal"> / {{ proc.requiredQuantity }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="flex items-center justify-between text-[11px] text-gray-400 dark:text-zinc-500 pt-1">
            <span>Total processed lines:</span>
            <span class="font-bold text-emerald-600 dark:text-emerald-400">{{ finalAllocationsSummary.length || 0 }}</span>
          </div>
        </div>

        <Button
          label="Finalize & Close Order"
          icon="pi pi-flag-fill"
          severity="success"
          class="p-button-lg py-3 px-6 text-lg font-bold shadow-md tracking-wider w-full"
          :loading="actionLoading"
          @click="closeFinalSummary"
        />
      </div>

      <!-- ECRAN: Lipsă task-uri active -->
      <div v-else-if="isEmpty" class="app-card w-full rounded-2xl p-6 text-center flex flex-col items-center justify-center">
        <i class="pi pi-inbox text-4xl app-muted"></i>
        <h2 class="app-title text-xl font-semibold mt-3">No assigned tasks</h2>
        <p class="app-subtitle text-xs mt-1">You do not have active tasks right now.</p>
        <Button class="mt-4 p-button-sm" icon="pi pi-refresh" label="Refresh" @click="loadCurrentTask()" />
      </div>

      <!-- Formularul activ de pași (Cât timp există un task activ pe backend) -->
      <template v-else-if="summary">
        <Card
          class="rounded-2xl w-full text-left shadow-md overflow-hidden"
          :pt="{ content: { class: 'p-2 sm:p-5' } }"
        >
          <template #title>
            <div class="text-center px-2 py-1 rounded-xl" :class="taskHeaderClass">
              <div class="text-base font-bold tracking-wide py-1 flex items-center justify-center gap-2">
                <i :class="isReplenishmentTask ? 'pi pi-sync' : 'pi pi-shopping-bag'"></i>
                {{ taskExecutionTitle }}
              </div>
            </div>
          </template>
          <template #content>
            <div class="flex flex-col gap-3 w-full">

              <!-- ECRANUL DE START -->
              <div v-if="isAwaitingStart" class="w-full flex flex-col items-center gap-4 py-1">
                <div class="w-full bg-gray-50 dark:bg-zinc-800/50 rounded-xl p-3 sm:p-4 border border-gray-100 dark:border-zinc-700/50 flex flex-col gap-3">
                  <div class="flex justify-between items-center border-b border-gray-200/50 dark:border-zinc-700/50 pb-2">
                    <div class="text-[11px] font-bold uppercase tracking-wider text-gray-400 dark:text-zinc-500 flex items-center gap-1.5">
                      <i class="pi pi-map-marker text-sm"></i> Task Itinerary & Items
                    </div>
                    <span class="text-[10px] px-2 py-0.5 rounded-full font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300">
                      {{ summary.status || 'ASSIGNED' }}
                    </span>
                  </div>

                  <div class="flex flex-col gap-2 max-h-[260px] overflow-y-auto pr-1">
                    <div
                      v-for="(proc, index) in orderedAllocations"
                      :key="proc.allocationId || index"
                      class="flex flex-col gap-1 p-2.5 bg-white dark:bg-zinc-800 rounded-lg border border-gray-100 dark:border-zinc-700 text-xs shadow-sm"
                    >
                      <div class="flex justify-between items-start gap-2">
                        <span class="font-bold text-gray-800 dark:text-zinc-200 line-clamp-2 flex-1">
                          {{ proc.productName || 'Unknown Product' }}
                        </span>
                        <span class="font-mono text-[10px] text-gray-400 dark:text-zinc-500 whitespace-nowrap">
                          #{{ proc.productBarcode || 'No EAN' }}
                        </span>
                      </div>

                      <div class="flex justify-between items-center mt-1 pt-1.5 border-t border-dashed border-gray-100 dark:border-zinc-700/50">
                        <div class="flex items-center gap-1.5 font-mono text-[11px]">
                          <span class="px-1.5 py-0.5 bg-gray-100 dark:bg-zinc-700 rounded text-gray-600 dark:text-zinc-300 font-semibold">
                            {{ proc.sourceLocationBarcode || '???' }}
                          </span>
                          <i v-if="isReplenishmentTask" class="pi pi-arrow-right text-[10px] text-gray-400"></i>
                          <span v-if="isReplenishmentTask" class="px-1.5 py-0.5 bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600 dark:text-emerald-400 rounded font-semibold">
                            {{ proc.destinationLocationBarcode || '???' }}
                          </span>
                        </div>

                        <div class="text-right whitespace-nowrap">
                          <span class="text-gray-400 dark:text-zinc-500 text-[11px]">Qty: </span>
                          <strong class="text-sm app-brand">{{ proc.requiredQuantity }}</strong>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="flex items-center justify-between text-[11px] text-gray-400 dark:text-zinc-500 pt-1">
                    <span>Total lines to process:</span>
                    <span class="font-bold text-gray-700 dark:text-zinc-300">{{ orderedAllocations.length || 0 }}</span>
                  </div>
                </div>

                <Button
                  label="Start Task"
                  icon="pi pi-play"
                  class="p-button-lg py-3 px-6 text-lg font-bold shadow-md tracking-wider w-full"
                  :severity="isReplenishmentTask ? 'primary' : 'warn'"
                  :loading="actionLoading"
                  @click="startTask"
                />
              </div>

              <!-- STEPPER DINAMIC UNITAR -->
              <Stepper v-else-if="currentAllocation" :value="activeStep" linear class="w-full">
                <StepList class="justify-center text-xs gap-3">
                  <Step
                    v-for="step in stepDefinitions"
                    :key="step.value"
                    :value="step.value"
                    class="min-w-0"
                    :pt="{
                      title: { class: 'text-xs inline ml-1 font-semibold' },
                      number: { class: 'w-7 h-7 text-xs font-bold' }
                    }"
                  >
                    {{ step.label }}
                  </Step>
                </StepList>
                <StepPanels class="w-full pt-4">
                  <StepPanel
                    v-for="step in stepDefinitions"
                    :key="`panel-${step.value}`"
                    :value="step.value"
                    class="w-full"
                  >
                    <!-- Pasul: Location -->
                    <div v-if="step.type === 'location'" class="app-card rounded-xl p-4 flex flex-col gap-3 text-center items-center w-full box-border">
                      <div>
                        <div class="text-xs app-muted">Scan source location</div>
                        <div class="font-mono font-semibold app-brand text-xl mt-0.5">
                          {{ currentAllocation.sourceLocationBarcode }}
                        </div>
                      </div>
                      <ScanSection
                        v-model="barcodeInput"
                        :loading="actionLoading"
                        :error-message="actionError"
                        submit-label="Verify Source"
                        placeholder="Scan or enter source barcode"
                        @submit="submitBarcodeStep"
                      />
                    </div>

                    <!-- Pasul: Product -->
                    <div v-if="step.type === 'product'" class="app-card rounded-xl p-4 flex flex-col gap-3 text-center items-center w-full box-border">
                      <div>
                        <div class="text-xs app-muted">Scan product barcode</div>
                        <div class="font-mono font-semibold app-warm text-xl mt-0.5">
                          {{ currentAllocation.productBarcode || 'No barcode' }}
                        </div>
                        <div class="text-xs app-muted mt-1 font-medium px-2 line-clamp-2">{{ currentAllocation.productName }}</div>
                      </div>
                      <ScanSection
                        v-model="barcodeInput"
                        :loading="actionLoading"
                        :error-message="actionError"
                        submit-label="Verify Product"
                        placeholder="Scan or enter product barcode"
                        @submit="submitBarcodeStep"
                      />
                    </div>

                    <!-- Pasul: Quantity -->
                    <div v-if="step.type === 'quantity'" class="app-card rounded-xl p-4 flex flex-col gap-3 text-center items-center w-full box-border">
                      <div>
                        <div class="text-xs app-muted">Confirm picked quantity</div>
                        <div class="font-semibold text-sm app-title mt-0.5 px-2 line-clamp-2">
                          {{ currentAllocation.productName }}
                        </div>
                        <div class="text-xs font-normal text-gray-500 mt-1">Required: <strong class="app-brand">{{ currentAllocation.requiredQuantity }}</strong></div>
                      </div>

                      <div class="w-full flex flex-col gap-2 items-center">
                        <InputNumber
                          v-model="pickedQuantity"
                          :min="1"
                          :max="currentAllocation.requiredQuantity"
                          fluid
                          readonly
                          inputClass="text-center text-lg font-bold bg-gray-100 dark:bg-zinc-800 py-2 w-full"
                        />
                        <div class="flex gap-3 w-full justify-center">
                          <Button
                            icon="pi pi-minus"
                            severity="danger"
                            class="w-12 h-12 rounded-full shadow"
                            @click="decrementQuantity"
                            :disabled="pickedQuantity <= 1"
                          />
                          <Button
                            icon="pi pi-plus"
                            severity="success"
                            class="w-12 h-12 rounded-full shadow"
                            @click="incrementQuantity(currentAllocation.requiredQuantity)"
                            :disabled="pickedQuantity >= currentAllocation.requiredQuantity"
                          />
                        </div>
                      </div>

                      <Message v-if="actionError" severity="error" :closable="false" class="w-full text-xs mt-1">
                        {{ actionError }}
                      </Message>

                      <Button
                        label="Confirm Quantity"
                        icon="pi pi-check-circle"
                        class="mt-2 w-full"
                        :loading="actionLoading"
                        @click="confirmQuantity"
                      />
                    </div>

                    <!-- Pasul: Move Review / Completion (Doar pentru Replenishment) -->
                    <div v-if="step.type === 'complete-move'" class="w-full flex flex-col gap-3 box-border">
                      <div class="text-sm font-bold text-emerald-600 dark:text-emerald-400 w-full flex items-center justify-center gap-1.5 py-1">
                        <i class="pi pi-file-edit text-base"></i> Move Review & Summary
                      </div>

                      <div class="w-full">
                        <div v-for="row in liveSummaryRows" :key="row.rowKey" class="bg-gray-50 dark:bg-zinc-800/60 rounded-xl p-3 border border-gray-200/60 dark:border-zinc-700/80 text-left shadow-sm">
                          <div class="flex flex-col gap-2">
                            <div>
                              <span class="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Product</span>
                              <p class="text-xs font-bold text-gray-800 dark:text-zinc-200 line-clamp-2 mt-0.5">{{ row.productName }}</p>
                            </div>

                            <div class="grid grid-cols-3 gap-2 border-t border-gray-200/60 dark:border-zinc-700/60 pt-2 mt-1">
                              <div>
                                <span class="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Qty</span>
                                <p class="text-sm font-extrabold text-emerald-600 dark:text-emerald-400 mt-0.5">{{ row.movedQuantity }}</p>
                              </div>
                              <div>
                                <span class="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Source</span>
                                <p class="font-mono text-xs font-semibold bg-gray-200/50 dark:bg-zinc-700 px-1.5 py-0.5 rounded text-center mt-0.5 inline-block">{{ row.sourceLocation }}</p>
                              </div>
                              <div>
                                <span class="text-[10px] uppercase font-bold text-gray-400 tracking-wider">Dest</span>
                                <p class="font-mono text-xs font-semibold bg-emerald-100/50 dark:bg-emerald-950/40 px-1.5 py-0.5 rounded text-center mt-0.5 inline-block text-emerald-700 dark:text-emerald-400">{{ row.destinationLocation }}</p>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <Message v-if="actionError" severity="error" :closable="false" class="w-full text-xs">
                        {{ actionError }}
                      </Message>

                      <Button
                        label="Complete Move & Save"
                        icon="pi pi-check-circle"
                        severity="success"
                        class="w-full font-bold py-3 shadow-md text-base mt-2"
                        :loading="actionLoading"
                        @click="completeAllocation"
                      />
                    </div>

                  </StepPanel>
                </StepPanels>
              </Stepper>
            </div>
          </template>
        </Card>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { allocationApi } from '@/api/allocationApi'
import BarcodeScanner from '@/components/BarcodeScanner.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
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
    errorMessage: { type: String, default: '' },
    submitLabel: { type: String, required: true },
    placeholder: { type: String, required: true }
  },
  emits: ['update:modelValue', 'submit'],
  setup(props, { emit }) {
    const isScannerOpen = ref(false)
    const toggleScanner = () => { isScannerOpen.value = !isScannerOpen.value }
    const handleDetected = (value) => {
      emit('update:modelValue', value ? value.toUpperCase() : '')
      isScannerOpen.value = false
    }

    return () =>
      h('div', { class: 'flex flex-col gap-2 w-full items-center' }, [
        h('div', { class: 'flex flex-col gap-1.5 w-full text-center' }, [
          h(InputText, {
            modelValue: props.modelValue,
            'onUpdate:modelValue': (value) => emit('update:modelValue', value ? value.toUpperCase() : ''),
            placeholder: props.placeholder,
            class: 'text-center w-full p-inputtext-sm',
            onKeyup: (event) => { if (event.key === 'Enter') emit('submit') }
          }),
          h(Button, {
            label: props.submitLabel,
            icon: 'pi pi-check',
            class: 'w-full p-button-sm',
            loading: props.loading,
            onClick: () => emit('submit')
          })
        ]),
        h(Button, {
          label: isScannerOpen.value ? 'Close Camera' : 'Scan Barcode',
          icon: isScannerOpen.value ? 'pi pi-times' : 'pi pi-camera',
          severity: isScannerOpen.value ? 'secondary' : 'info',
          class: 'w-full text-xs font-semibold py-2 p-button-sm',
          onClick: toggleScanner
        }),
        isScannerOpen.value ? h('div', { class: 'w-full border-2 border-dashed border-sky-400 rounded-xl overflow-hidden p-1 bg-black/5' }, [h(BarcodeScanner, { onDetected: handleDetected })]) : null,
        props.errorMessage ? h(Message, { severity: 'error', closable: false, class: 'w-full text-xs text-left mt-0.5' }, () => props.errorMessage) : null
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
const barcodeInput = ref('')
const pickedQuantity = ref(1)

// Stocăm local lista de linii ca să o avem disponibilă pe ecranul final chiar dacă backend-ul întoarce 204
const finalAllocationsSummary = ref([])
const showFinalSummary = ref(false)

const incrementQuantity = (max) => { if (pickedQuantity.value < max) pickedQuantity.value++ }
const decrementQuantity = () => { if (pickedQuantity.value > 1) pickedQuantity.value-- }

const isEmpty = computed(() => !loading.value && !loadError.value && !summary.value && !showFinalSummary.value)
const currentAllocation = computed(() => summary.value?.currentAllocation || null)
const orderedAllocations = computed(() => summary.value?.allocations || [])
const isPickingTask = computed(() => summary.value?.taskType === 'PICKING_ORDER')
const isReplenishmentTask = computed(() => summary.value?.taskType === 'REPLENISHMENT')

const taskExecutionTitle = computed(() => isReplenishmentTask.value ? 'Replenishment Task' : 'Picking Order Task')
const taskHeaderClass = computed(() => isReplenishmentTask.value ? 'bg-blue-500/10 text-blue-600 dark:text-blue-400' : 'bg-amber-500/10 text-amber-600 dark:text-amber-400')
const isAwaitingStart = computed(() => summary.value?.status === 'ASSIGNED' || currentAllocation.value?.status === 'ASSIGNED')

const liveSummaryRows = computed(() => {
  if (!currentAllocation.value) return []
  return [{
    rowKey: currentAllocation.value.allocationId,
    productName: currentAllocation.value.productName || 'N/A',
    movedQuantity: pickedQuantity.value ?? currentAllocation.value.pickedQuantity ?? 0,
    sourceLocation: currentAllocation.value.sourceLocationBarcode || 'N/A',
    destinationLocation: currentAllocation.value.destinationLocationBarcode || 'N/A'
  }]
})

const stepDefinitions = computed(() => {
  if (!currentAllocation.value) return []
  if (isReplenishmentTask.value) {
    return [
      { value: 1, type: 'location', label: 'Location' },
      { value: 2, type: 'product', label: 'Product' },
      { value: 3, type: 'quantity', label: 'Quantity' },
      { value: 4, type: 'complete-move', label: 'Execution' }
    ]
  }
  return [
    { value: 1, type: 'location', label: 'Location' },
    { value: 2, type: 'product', label: 'Product' },
    { value: 3, type: 'quantity', label: 'Quantity' }
  ]
})

const activeStep = computed(() => {
  if (!currentAllocation.value) return 1
  if (!currentAllocation.value.sourceLocationScanned) return 1
  if (!currentAllocation.value.productScanned) return 2
  if (isReplenishmentTask.value) {
    if (currentAllocation.value.pickedQuantity == null) return 3
    return 4
  }
  return 3
})

const getErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback

const hydrateState = (payload) => {
  summary.value = payload
  actionError.value = ''
  barcodeInput.value = ''
  pickedQuantity.value = payload?.currentAllocation?.pickedQuantity ?? payload?.currentAllocation?.requiredQuantity ?? 1

  // Salvăm istoricul complet al liniilor local
  if (payload && payload.allocations) {
    finalAllocationsSummary.value = JSON.parse(JSON.stringify(payload.allocations))
  }
}

const loadCurrentTask = async () => {
  loading.value = true
  try {
    const response = await allocationApi.getCurrentTaskSummary()
    if (response.status === 204 || !response.data) {
      // Dacă aveam un task activ de picking și acum s-a terminat, activăm ecranul final
      if (summary.value && isPickingTask.value) {
        showFinalSummary.value = true
      }
      summary.value = null
      return
    }
    hydrateState(response.data)
  } catch (error) {
    if (error?.response?.status === 204) {
      if (summary.value && isPickingTask.value) {
        showFinalSummary.value = true
      }
      summary.value = null
      return
    }
    loadError.value = getErrorMessage(error, 'Failed to load operator task.')
    summary.value = null
  } finally {
    loading.value = false
  }
}

// Butonul de Refresh din Header va forța curățarea stării finale ca operatorul să poată trage task-uri noi
const forceRefreshTask = () => {
  showFinalSummary.value = false
  loadCurrentTask()
}

// Închide Summary-ul final manual și reîncarcă consola golită
const closeFinalSummary = () => {
  showFinalSummary.value = false
  loadCurrentTask()
}

const startTask = async () => {
  actionLoading.value = true
  actionError.value = ''
  try {
    const response = await allocationApi.startCurrentTask()
    hydrateState(response.data)
    toast.add({ severity: 'success', summary: 'Task started', life: 1500 })
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Failed to start task.')
  } finally {
    actionLoading.value = false
  }
}

const submitBarcodeStep = async () => {
  if (!currentAllocation.value || !barcodeInput.value?.trim()) {
    actionError.value = 'Barcode is required.'
    return
  }
  actionLoading.value = true
  actionError.value = ''
  const cleanBarcode = barcodeInput.value.trim().toUpperCase()

  try {
    if (!currentAllocation.value.sourceLocationScanned) {
      await allocationApi.scanSourceLocation(currentAllocation.value.allocationId, cleanBarcode)
      toast.add({ severity: 'success', summary: 'Source verified', life: 1500 })
    } else if (!currentAllocation.value.productScanned) {
      await allocationApi.scanProduct(currentAllocation.value.allocationId, cleanBarcode)
      toast.add({ severity: 'success', summary: 'Product verified', life: 1500 })
    }
    barcodeInput.value = ''
    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Barcode validation failed.')
  } finally {
    actionLoading.value = false
  }
}

const confirmQuantity = async () => {
  if (!currentAllocation.value) return
  actionLoading.value = true
  actionError.value = ''

  try {
    const allocationId = currentAllocation.value.allocationId

    // Actualizăm cantitatea culeasă în tabloul local înainte de salvarea finală a liniei
    const localIdx = finalAllocationsSummary.value.findIndex(p => p.allocationId === allocationId)
    if (localIdx !== -1) {
      finalAllocationsSummary.value[localIdx].pickedQuantity = pickedQuantity.value
    }

    await allocationApi.confirmPickedQuantity(allocationId, pickedQuantity.value)

    if (isPickingTask.value) {
      await allocationApi.completeAssignedAllocation(allocationId)
      toast.add({ severity: 'success', summary: 'Line saved successfully', life: 1500 })
    } else {
      toast.add({ severity: 'success', summary: 'Quantity confirmed', life: 1500 })
    }

    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Invalid picked quantity.')
  } finally {
    actionLoading.value = false
  }
}

const completeAllocation = async () => {
  if (!currentAllocation.value) return
  actionLoading.value = true
  actionError.value = ''
  try {
    await allocationApi.completeAssignedAllocation(currentAllocation.value.allocationId)
    toast.add({ severity: 'success', summary: 'Task completed and saved', life: 2000 })
    await loadCurrentTask()
  } catch (error) {
    actionError.value = getErrorMessage(error, 'Failed to complete allocation.')
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
