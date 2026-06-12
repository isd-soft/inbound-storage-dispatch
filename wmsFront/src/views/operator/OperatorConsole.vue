<template>
  <div class="app-shell font-sans">
    <!-- Header -->
    <header class="app-header flex justify-between items-center px-4 py-3 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-box text-2xl app-warm"></i>
        <h1 class="text-xl font-bold tracking-wide app-title">Operator</h1>
      </div>
      <div class="flex items-center gap-2">
        <ThemeToggle />
        <Button icon="pi pi-sign-out" severity="danger" text rounded @click="handleLogout" />
      </div>
    </header>

    <!-- ══════════════════════════════════════
         IDLE
    ══════════════════════════════════════ -->
    <main
      v-if="screen === 'idle'"
      class="p-4 max-w-md mx-auto mt-16 flex flex-col items-center gap-6"
    >
      <div class="text-center">
        <div
          class="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-4"
          style="
            background: color-mix(in srgb, var(--brand-warm) 12%, transparent);
            border: 2px solid color-mix(in srgb, var(--brand-warm) 30%, transparent);
          "
        >
          <i class="pi pi-box text-4xl app-warm"></i>
        </div>
        <h2 class="app-title text-2xl font-bold">Ready for Work</h2>
        <p class="app-subtitle mt-1 text-sm">Press Start to receive your next process</p>
      </div>

      <Button
        label="Start"
        icon="pi pi-play"
        iconPos="right"
        size="large"
        raised
        class="w-full"
        :loading="starting"
        @click="startNext"
      />
      <Button
        label="Report Issue"
        icon="pi pi-exclamation-triangle"
        severity="danger"
        outlined
        class="w-full"
      />

      <Message v-if="startError" severity="error" :closable="false" class="w-full">{{
        startError
      }}</Message>
    </main>

    <!-- ══════════════════════════════════════
         EXECUTE — REPLENISHMENT
    ══════════════════════════════════════ -->
    <main v-else-if="screen === 'replenishment'" class="p-4 max-w-md mx-auto mt-4">
      <!-- Task info bar -->
      <div class="app-muted-panel rounded-xl px-4 py-3 mb-4 flex items-center justify-between">
          <div>
            <span class="app-muted text-xs font-mono">Process #{{ activeProcess.id }}</span>
          <p class="font-semibold text-sm app-title">{{ activeProcess.productName || activeProcess.product?.name }}</p>
          <p class="text-xs app-muted font-mono">{{ activeProcess.productSku || activeProcess.sku || activeProcess.product?.barcode || activeProcess.product?.sku || activeProcess.product?.code }}</p>
          <p class="text-xs app-muted">{{ activeProcess.quantity }} units</p>
        </div>
        <Tag value="Replenishment" severity="info" />
      </div>

      <!-- Steps: source → product → dest → done -->
      <StepBar
        :steps="['Source', 'Product', 'Destination', 'Done']"
        :current="replStep"
        class="mb-5"
      />

      <!-- Step 0: scan source -->
      <div v-if="replStep === 0" class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div>
            <p class="app-subtitle text-sm mb-1">Go to source location and scan:</p>
          <p class="text-2xl font-bold font-mono app-brand">{{ activeProcess.locationCode || activeProcess.location?.barcode || activeProcess.location?.locationCode || activeProcess.location?.code }}</p>
          </div>
        <ScanInput :key="'r0'" @submit="onReplSource" :error="stepError" :loading="stepLoading" />
      </div>

      <!-- Step 1: scan product -->
      <div v-else-if="replStep === 1" class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div>
            <p class="app-subtitle text-sm mb-1">Scan product barcode (SKU):</p>
          <p class="text-xl font-bold font-mono app-warm">{{ activeProcess.productSku || activeProcess.sku || activeProcess.product?.barcode || activeProcess.product?.sku || activeProcess.product?.code }}</p>
          <p class="text-xs app-muted mt-1">Qty: {{ activeProcess.quantity }} units</p>
          </div>
        <ScanInput :key="'r1'" @submit="onReplProduct" :error="stepError" :loading="stepLoading" />
      </div>

      <!-- Step 2: confirm qty + scan destination -->
      <div v-else-if="replStep === 2" class="app-card rounded-xl p-5 flex flex-col gap-4">
        <div>
          <p class="app-subtitle text-sm mb-1">Confirm quantity picked:</p>
          <p class="app-warm font-bold">Required: {{ activeProcess.quantity }} units</p>
        </div>
        <div class="flex flex-col gap-2">
          <label class="text-xs app-muted">Picked quantity</label>
          <InputNumber
            v-model="pickedQty"
            :min="1"
            :max="activeProcess.quantity"
            showButtons
            class="w-full"
          />
        </div>
        <Divider />
        <div>
          <p class="app-subtitle text-sm mb-1">Scan destination location:</p>
          <p class="text-2xl font-bold font-mono app-success">{{ replDestCode }}</p>
        </div>
        <ScanInput
          :key="'r2'"
          @submit="onReplDestination"
          :error="stepError"
          :loading="stepLoading"
        />
      </div>

      <!-- Step 3: complete -->
      <div
        v-else-if="replStep === 3"
        class="app-card rounded-xl p-5 flex flex-col items-center gap-4"
      >
        <div
          class="w-16 h-16 rounded-full flex items-center justify-center"
          style="
            background: color-mix(in srgb, var(--status-success) 15%, transparent);
            border: 2px solid var(--status-success);
          "
        >
          <i class="pi pi-check text-3xl app-success" />
        </div>
        <div class="text-center">
          <p class="font-bold text-lg app-success">All verified!</p>
          <p class="app-subtitle text-sm mt-1">
            Picked {{ pickedQty }} of {{ activeProcess.quantity }} units
          </p>
        </div>
          <div class="w-full app-muted-panel rounded-lg p-3 text-sm flex flex-col gap-1.5">
          <div class="flex justify-between">
            <span class="app-muted">Product</span>
            <span class="font-semibold">{{ activeProcess.productName || activeProcess.product?.name }}</span>
          </div>
          <div class="flex justify-between">
            <span class="app-muted">From</span>
            <span class="font-mono app-brand">{{ activeProcess.locationCode || activeProcess.location?.barcode || activeProcess.location?.locationCode || activeProcess.location?.code }}</span>
          </div>
          <div class="flex justify-between">
            <span class="app-muted">To</span>
            <span class="font-mono app-success">{{ replDestCode }}</span>
          </div>
          <div class="flex justify-between">
            <span class="app-muted">Quantity</span>
            <span class="font-bold app-warm">{{ pickedQty }} units</span>
          </div>
        </div>
        <Button
          label="Complete Process"
          icon="pi pi-flag-fill"
          severity="success"
          raised
          class="w-full"
          :loading="stepLoading"
          @click="onReplComplete"
        />
      </div>
    </main>

    <!-- ══════════════════════════════════════
         EXECUTE — PICKING ORDER
    ══════════════════════════════════════ -->
    <main v-else-if="screen === 'order'" class="p-4 max-w-md mx-auto mt-4">
      <!-- Order info bar -->
      <div class="app-muted-panel rounded-xl px-4 py-3 mb-4 flex items-center justify-between">
        <div>
          <span class="app-muted text-xs font-mono">{{ orderLogicalId || `Process #${activeProcess.id}` }}</span>
          <p class="font-semibold text-sm app-title">Picking Order</p>
          <p class="text-xs app-muted">{{ orderTotalProcesses }} items total</p>
        </div>
        <Tag value="Order" severity="warn" />
      </div>

      <!-- All lines done → scan dispatch -->
      <template v-if="orderAllLinesDone">
        <StepBar :steps="['Items', 'Dispatch']" :current="1" class="mb-5" />
        <div class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div class="flex flex-col items-center gap-2 text-center mb-2">
            <i class="pi pi-send text-4xl app-success" />
            <p class="font-bold app-title">All items picked!</p>
            <p class="app-subtitle text-sm">Scan the dispatch destination</p>
          </div>
          <div>
            <p class="app-subtitle text-sm mb-1">Expected destination:</p>
            <p class="text-2xl font-bold font-mono app-success">{{ orderDestCode }}</p>
          </div>
          <ScanInput
            :key="'od'"
            @submit="onOrderDestination"
            :error="stepError"
            :loading="stepLoading"
          />
        </div>
      </template>

      <!-- Final complete screen -->
      <template v-else-if="orderDone">
        <StepBar :steps="['Items', 'Dispatch', 'Done']" :current="2" class="mb-5" />
        <div class="app-card rounded-xl p-5 flex flex-col items-center gap-4">
          <div
            class="w-16 h-16 rounded-full flex items-center justify-center"
            style="
              background: color-mix(in srgb, var(--status-success) 15%, transparent);
              border: 2px solid var(--status-success);
            "
          >
            <i class="pi pi-check text-3xl app-success" />
          </div>
          <p class="font-bold text-lg app-success">Order ready for dispatch!</p>
          <div class="w-full app-muted-panel rounded-lg p-3 text-sm flex flex-col gap-1">
            <div
              v-for="p in orderProcesses"
              :key="p.id"
              class="flex justify-between py-1.5 border-b border-[var(--border-subtle)] last:border-0"
            >
              <span class="app-title text-xs">{{ p.productName }}</span>
              <span class="app-warm font-bold text-xs">{{ p.quantity }} u</span>
            </div>
            <div class="flex justify-between pt-2">
              <span class="app-muted">Destination</span>
              <span class="font-mono app-success">{{ orderDestCode }}</span>
            </div>
          </div>
          <Button
            label="Complete Order"
            icon="pi pi-flag-fill"
            severity="success"
            raised
            class="w-full"
            :loading="stepLoading"
            @click="onOrderComplete"
          />
        </div>
      </template>

      <!-- Per-line loop -->
      <template v-else>
        <!-- Line progress dots -->
        <div
          class="app-muted-panel rounded-xl px-4 py-2 mb-3 flex items-center justify-between text-sm"
        >
          <span class="app-subtitle"
            >Item {{ orderCurrentIndex }} of {{ orderTotalProcesses }}</span
          >
          <div class="flex gap-1.5">
            <span
              v-for="i in orderTotalProcesses"
              :key="i"
              class="w-2.5 h-2.5 rounded-full transition-all"
              :class="
                i < orderCurrentIndex
                  ? 'bg-[var(--status-success)]'
                  : i === orderCurrentIndex
                    ? 'bg-[var(--brand-warm)]'
                    : 'bg-[var(--border-strong)]'
              "
            />
          </div>
        </div>

        <!-- Current line info -->
        <div class="app-card rounded-xl px-4 py-3 mb-4">
          <p class="app-muted text-xs mb-0.5">Current item</p>
          <p class="font-semibold app-title">{{ currentOrderProc?.productName }}</p>
          <p class="text-xs app-muted font-mono">{{ currentOrderProc?.productSku || currentOrderProc?.sku || currentOrderProc?.product?.barcode || currentOrderProc?.product?.sku || currentOrderProc?.product?.code }}</p>
          <div class="flex gap-4 mt-1">
            <span class="text-sm app-warm font-bold">{{ currentOrderProc?.quantity }} units</span>
            <span class="text-xs app-muted font-mono">{{ currentOrderProc?.locationCode || currentOrderProc?.location?.barcode || currentOrderProc?.location?.locationCode || currentOrderProc?.location?.code }}</span>
          </div>
        </div>

        <!-- Per-line steps -->
        <StepBar :steps="['Source', 'Product', 'Qty']" :current="orderLineStep" class="mb-5" />

        <!-- Line step 0: source -->
        <div v-if="orderLineStep === 0" class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div>
            <p class="app-subtitle text-sm mb-1">Scan source location:</p>
            <p class="text-2xl font-bold font-mono app-brand">
              {{ currentOrderProc?.locationCode || currentOrderProc?.location?.barcode || currentOrderProc?.location?.locationCode || currentOrderProc?.location?.code }}
            </p>
          </div>
          <ScanInput
            :key="'ol0-' + orderLoopIdx"
            @submit="onOrderSource"
            :error="stepError"
            :loading="stepLoading"
          />
        </div>

        <!-- Line step 1: product -->
        <div v-else-if="orderLineStep === 1" class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div>
            <p class="app-subtitle text-sm mb-1">Scan product barcode (SKU):</p>
            <p class="text-xl font-bold font-mono app-warm">{{ currentOrderProc?.productSku || currentOrderProc?.sku || currentOrderProc?.product?.barcode || currentOrderProc?.product?.sku || currentOrderProc?.product?.code }}</p>
          </div>
          <ScanInput
            :key="'ol1-' + orderLoopIdx"
            @submit="onOrderProduct"
            :error="stepError"
            :loading="stepLoading"
          />
        </div>

        <!-- Line step 2: qty -->
        <div v-else-if="orderLineStep === 2" class="app-card rounded-xl p-5 flex flex-col gap-4">
          <div>
            <p class="app-subtitle text-sm mb-1">Confirm picked quantity:</p>
            <p class="app-warm font-bold">Required: {{ currentOrderProc?.quantity }} units</p>
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-xs app-muted">Picked quantity</label>
            <InputNumber
              v-model="pickedQty"
              :min="1"
              :max="currentOrderProc?.quantity"
              showButtons
              class="w-full"
            />
            <Message v-if="stepError" severity="error" :closable="false">{{ stepError }}</Message>
            <Button
              label="Confirm & Next"
              icon="pi pi-arrow-right"
              iconPos="right"
              class="w-full"
              :loading="stepLoading"
              @click="onOrderConfirmQty"
            />
          </div>
        </div>
      </template>
    </main>

    <Toast />
  </div>
</template>

<script setup>
import { ref, computed, defineComponent, h } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import apiClient from '@/api/index.js'
import BarcodeScanner from '@/components/BarcodeScanner.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Divider from 'primevue/divider'
import Toast from 'primevue/toast'
import 'primeicons/primeicons.css'

// ─── StepBar component ────────────────────────────────────────────────────────
const StepBar = defineComponent({
  props: { steps: Array, current: Number },
  setup(props) {
    return () =>
      h(
        'div',
        { class: 'flex items-center gap-1' },
        props.steps.flatMap((s, idx) => {
          const done = idx < props.current
          const active = idx === props.current
          const circleClass = done
            ? 'border-[var(--status-success)] bg-[color-mix(in_srgb,var(--status-success)_15%,transparent)] text-[var(--status-success)]'
            : active
              ? 'border-[var(--brand-warm)] bg-[color-mix(in_srgb,var(--brand-warm)_15%,transparent)] text-[var(--brand-warm)]'
              : 'border-[var(--border-strong)] bg-[var(--surface-card-muted)] text-[var(--text-muted)]'
          const labelClass = done ? 'app-success' : active ? 'app-warm font-medium' : 'app-muted'
          const nodes = [
            h('div', { class: 'flex flex-col items-center flex-1 gap-1' }, [
              h(
                'div',
                {
                  class: `w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all ${circleClass}`,
                },
                done ? [h('i', { class: 'pi pi-check text-xs' })] : [`${idx + 1}`],
              ),
              h('span', { class: `text-xs text-center leading-tight ${labelClass}` }, s),
            ]),
          ]
          if (idx < props.steps.length - 1) {
            nodes.push(
              h('div', {
                class: `h-0.5 flex-1 mb-4 rounded transition-all ${done ? 'bg-[var(--status-success)]' : 'bg-[var(--border-subtle)]'}`,
              }),
            )
          }
          return nodes
        }),
      )
  },
})

// ─── ScanInput component ──────────────────────────────────────────────────────
const ScanInput = defineComponent({
  props: { error: String, loading: Boolean },
  emits: ['submit'],
  setup(props, { emit }) {
    const manual = ref('')
    const scannerOpen = ref(false)
    const submit = (val) => {
      if (val?.trim()) {
        scannerOpen.value = false
        emit('submit', val.trim())
      }
    }
    return () =>
      h('div', { class: 'flex flex-col gap-3' }, [
        h('div', { class: 'flex gap-2' }, [
          h(InputText, {
            modelValue: manual.value,
            'onUpdate:modelValue': (v) => (manual.value = v),
            placeholder: 'Enter code manually...',
            class: 'flex-1',
            disabled: props.loading,
            onKeyup: (e) => {
              if (e.key === 'Enter') submit(manual.value)
            },
          }),
          h(Button, {
            icon: 'pi pi-arrow-right',
            loading: props.loading,
            onClick: () => submit(manual.value),
          }),
        ]),
        h(Button, {
          label: scannerOpen.value ? 'Close Camera' : 'Scan with Camera',
          icon: scannerOpen.value ? 'pi pi-times' : 'pi pi-camera',
          severity: scannerOpen.value ? 'secondary' : 'info',
          outlined: true,
          class: 'w-full',
          disabled: props.loading,
          onClick: () => (scannerOpen.value = !scannerOpen.value),
        }),
        scannerOpen.value ? h(BarcodeScanner, { onDetected: (val) => submit(val) }) : null,
        props.error ? h(Message, { severity: 'error', closable: false }, () => props.error) : null,
      ])
  },
})

// ─── State ────────────────────────────────────────────────────────────────────
const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const screen = ref('idle') // idle | replenishment | order
const starting = ref(false)
const startError = ref('')

const activeProcess = ref(null) // ProcessResponse (primul process din FIFO)
const replStep = ref(0) // 0=source 1=product 2=dest+qty 3=done
const replDestCode = ref('')

const orderProcesses = ref([]) // toate procesele din același task
const orderLoopIdx = ref(0)
const orderTotalProcesses = ref(1)
const orderCurrentIndex = ref(1)
const orderLogicalId = ref('')
const orderLineStep = ref(0) // 0=source 1=product 2=qty
const orderAllLinesDone = ref(false)
const orderDone = ref(false)
const orderDestCode = ref('DISP-01')

const stepError = ref('')
const stepLoading = ref(false)
const pickedQty = ref(1)

const currentOrderProc = computed(() => orderProcesses.value[orderLoopIdx.value] ?? null)

const normalizeLocationCode = (location) => {
  return location?.locationCode || location?.barcode || location?.code || location?.location || ''
}

const normalizeProductSku = (process) => {
  return process?.productSku || process?.sku || process?.productBarcode || process?.product?.barcode || process?.product?.sku || process?.product?.code || process?.productCode || ''
}

const normalizeProcess = (process) => ({
  ...process,
  productName: process?.productName || process?.product?.name || '',
  productSku: normalizeProductSku(process),
  locationCode: process?.locationCode || process?.locationBarcode || normalizeLocationCode(process?.location)
})

// ─── API ──────────────────────────────────────────────────────────────────────
const api = {
  getOperatorProcess: () => apiClient.get('/v1/processes/operators'),
  start: () => apiClient.post('/v1/processes/start'),
  scanLocation: (id, barcode) => apiClient.post(`/v1/processes/${id}/location`, { barcode }),
  scanProduct: (id, barcode) => apiClient.post(`/v1/processes/${id}/product`, { barcode }),
  confirmQty: (id, pickedQuantity) =>
    apiClient.post(`/v1/processes/${id}/confirm-quantity`, { pickedQuantity }),
  complete: (id) => apiClient.post(`/v1/processes/${id}/complete`),
}

const loadStartedProcess = async () => {
  await api.start()
  const operatorRes = await api.getOperatorProcess()
  const response = operatorRes.data || {}
  const process = normalizeProcess(response.processes || response)
  if (!process?.id) {
    throw new Error('No assigned process returned by backend.')
  }
  return { response, process }
}

// ─── START — FIFO logic ───────────────────────────────────────────────────────
const startNext = async () => {
  starting.value = true
  startError.value = ''
  stepError.value = ''

  try {
    const { response, process } = await loadStartedProcess()

    activeProcess.value = process
    pickedQty.value = process.quantity || 1
    if (response.taskType === 'PICKING_ORDER') {
      orderProcesses.value = [process]
      orderLoopIdx.value = 0
      orderTotalProcesses.value = response.totalOfProcess || 1
      orderCurrentIndex.value = response.currentIndexOfProcess || 1
      orderLogicalId.value = response.orderLogicalId || ''
      orderLineStep.value = 0
      orderAllLinesDone.value = false
      orderDone.value = false
      orderDestCode.value = response.destinationLocationBarcode || 'DISP-01'
      screen.value = 'order'
    } else {
      replStep.value = 0
      replDestCode.value = response.destinationLocationBarcode || ''
      screen.value = 'replenishment'
    }
  } catch (e) {
    startError.value = e.response?.data?.message || e.message || 'Failed to start process'
  } finally {
    starting.value = false
  }
}

// ─── REPLENISHMENT steps ──────────────────────────────────────────────────────
const onReplSource = async (barcode) => {
  stepError.value = ''
  stepLoading.value = true
  try {
    await api.scanLocation(activeProcess.value.id, barcode)
    replStep.value = 1
    toast.add({ severity: 'success', summary: 'Source location ✓', life: 1500 })
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Wrong location barcode'
  } finally {
    stepLoading.value = false
  }
}

const onReplProduct = async (barcode) => {
  stepError.value = ''
  stepLoading.value = true
  try {
    await api.scanProduct(activeProcess.value.id, barcode)
    replStep.value = 2
    toast.add({ severity: 'success', summary: 'Product ✓', life: 1500 })
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Wrong product barcode'
  } finally {
    stepLoading.value = false
  }
}

const onReplDestination = async (barcode) => {
  stepError.value = ''
  if (!pickedQty.value || pickedQty.value <= 0) {
    stepError.value = 'Set picked quantity first'
    return
  }
  // Validăm destination pe frontend — backend nu are endpoint pentru asta
  if (barcode.trim().toUpperCase() !== replDestCode.value.toUpperCase()) {
    stepError.value = `Wrong destination. Expected: ${replDestCode.value}`
    return
  }
  stepLoading.value = true
  try {
    await api.confirmQty(activeProcess.value.id, pickedQty.value)
    replStep.value = 3
    toast.add({ severity: 'success', summary: 'Destination ✓', life: 1500 })
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Failed to confirm quantity'
  } finally {
    stepLoading.value = false
  }
}

const onReplComplete = async () => {
  stepLoading.value = true
  try {
    await api.complete(activeProcess.value.id)
    toast.add({ severity: 'success', summary: 'Process completed! 🎉', life: 3000 })
    resetToIdle()
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: e.response?.data?.message || 'Failed to complete',
      life: 4000,
    })
  } finally {
    stepLoading.value = false
  }
}

// ─── ORDER steps — per-line loop ──────────────────────────────────────────────
const onOrderSource = async (barcode) => {
  stepError.value = ''
  stepLoading.value = true
  try {
    await api.scanLocation(currentOrderProc.value.id, barcode)
    orderLineStep.value = 1
    toast.add({ severity: 'success', summary: 'Source ✓', life: 1500 })
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Wrong location barcode'
  } finally {
    stepLoading.value = false
  }
}

const onOrderProduct = async (barcode) => {
  stepError.value = ''
  stepLoading.value = true
  try {
    await api.scanProduct(currentOrderProc.value.id, barcode)
    orderLineStep.value = 2
    pickedQty.value = currentOrderProc.value.quantity
    toast.add({ severity: 'success', summary: 'Product ✓', life: 1500 })
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Wrong product barcode'
  } finally {
    stepLoading.value = false
  }
}

const onOrderConfirmQty = async () => {
  stepError.value = ''
  if (!pickedQty.value || pickedQty.value <= 0) {
    stepError.value = 'Quantity must be greater than 0'
    return
  }
  stepLoading.value = true
  try {
    await api.confirmQty(currentOrderProc.value.id, pickedQty.value)
    await api.complete(currentOrderProc.value.id)

    const isLast = orderCurrentIndex.value >= orderTotalProcesses.value
    if (isLast) {
      orderAllLinesDone.value = true
      toast.add({ severity: 'info', summary: 'All items picked! Scan destination.', life: 2000 })
    } else {
      const { response, process } = await loadStartedProcess()
      activeProcess.value = process
      orderProcesses.value = [process]
      orderLoopIdx.value = 0
      orderTotalProcesses.value = response.totalOfProcess || orderTotalProcesses.value
      orderCurrentIndex.value = response.currentIndexOfProcess || orderCurrentIndex.value + 1
      orderLogicalId.value = response.orderLogicalId || orderLogicalId.value
      orderDestCode.value = response.destinationLocationBarcode || orderDestCode.value
      orderLineStep.value = 0
      pickedQty.value = process.quantity || 1
      stepError.value = ''
      toast.add({ severity: 'info', summary: `Item ${orderCurrentIndex.value - 1} done. Next!`, life: 1500 })
    }
  } catch (e) {
    stepError.value = e.response?.data?.message || 'Failed to confirm quantity'
  } finally {
    stepLoading.value = false
  }
}

const onOrderDestination = (barcode) => {
  if (barcode.trim().toUpperCase() !== orderDestCode.value.toUpperCase()) {
    stepError.value = `Wrong destination. Expected: ${orderDestCode.value}`
    return
  }
  stepError.value = ''
  orderAllLinesDone.value = false
  orderDone.value = true
  toast.add({ severity: 'success', summary: 'Destination ✓', life: 1500 })
}

const onOrderComplete = async () => {
  stepLoading.value = true
  try {
    toast.add({ severity: 'success', summary: 'Order completed! 🎉', life: 3000 })
    resetToIdle()
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: e.response?.data?.message || 'Failed to complete',
      life: 4000,
    })
  } finally {
    stepLoading.value = false
  }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
const resetToIdle = () => {
  screen.value = 'idle'
  activeProcess.value = null
  replStep.value = 0
  replDestCode.value = ''
  orderProcesses.value = []
  orderLoopIdx.value = 0
  orderTotalProcesses.value = 1
  orderCurrentIndex.value = 1
  orderLogicalId.value = ''
  orderLineStep.value = 0
  orderAllLinesDone.value = false
  orderDone.value = false
  stepError.value = ''
  startError.value = ''
  pickedQty.value = 1
}

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}
</script>
