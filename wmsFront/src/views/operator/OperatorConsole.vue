<template>
  <div class="app-shell font-sans">

    <header class="app-header flex justify-between items-center p-4 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-box text-2xl app-warm"></i>
        <h1 class="text-xl font-bold tracking-wide">Operator</h1>
      </div>
      <div class="flex items-center gap-2">
        <ThemeToggle />
        <Button icon="pi pi-sign-out" severity="danger" text rounded @click="handleLogout" />
      </div>
    </header>

    <main class="p-4 max-w-md mx-auto mt-6">
      <!-- Status -->
      <div class="text-center mb-8">
        <h2 class="app-title text-2xl font-bold">Ready for Work</h2>
        <p class="app-subtitle mt-1">Active Task: <span class="app-warm font-medium">None</span></p>
      </div>

      <!-- Main actions -->
      <div class="flex flex-col gap-4 mb-8">
        <Button
          :label="scannerOpen ? 'Close Scanner' : 'Scan Location / SKU'"
          :icon="scannerOpen ? 'pi pi-times' : 'pi pi-barcode'"
          class="p-4 text-lg w-full font-bold"
          :severity="scannerOpen ? 'secondary' : 'info'"
          raised
          @click="scannerOpen = !scannerOpen"
        />

        <!-- Scanner panel -->
        <div v-if="scannerOpen" class="bg-gray-800 rounded-xl p-4 flex flex-col gap-3">
          <BarcodeScanner @detected="onBarcodeDetected" />
          <Message v-if="lastScanned" severity="info" :closable="false">
            Scanned: <span class="font-mono font-semibold">{{ lastScanned }}</span>
          </Message>
        </div>

        <Button
          label="My Assigned Tasks"
          icon="pi pi-list"
          :badge="myTasks.length ? String(myTasks.length) : undefined"
          badgeSeverity="danger"
          class="p-4 text-lg w-full font-bold"
          severity="success"
          raised
          @click="tasksOpen = !tasksOpen"
        />

        <Button
          label="Report Issue"
          icon="pi pi-exclamation-triangle"
          class="p-4 text-lg w-full"
          severity="danger"
          outlined
        />
      </div>

      <!-- Tasks panel -->
      <div v-if="tasksOpen" class="flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <span class="text-sm font-semibold text-gray-400 uppercase tracking-wider"
            >Available Tasks</span
          >
          <Button icon="pi pi-refresh" text size="small" :loading="loading" @click="loadTasks" />
        </div>

        <Message v-if="error" severity="error" :closable="false">{{ error }}</Message>

        <!-- Skeletons -->
        <div v-if="loading && tasks.length === 0" class="flex flex-col gap-3">
          <Skeleton v-for="i in 3" :key="i" height="130px" class="rounded-xl" />
        </div>

        <!-- Empty -->
        <div
          v-if="!loading && tasks.length === 0 && !error"
          class="bg-gray-800 rounded-xl p-8 flex flex-col items-center gap-3"
        >
          <i class="pi pi-inbox text-5xl text-gray-600" />
          <p class="text-gray-500 text-sm">No tasks available</p>
        </div>

        <!-- Task cards -->
        <div
          v-for="task in tasks"
          :key="task.id"
          class="bg-gray-800 rounded-xl p-4 flex flex-col gap-3 border"
          :class="statusBorder(task.status)"
        >
          <div class="flex items-start justify-between gap-2">
            <div>
              <span class="text-xs text-gray-500 font-mono">#{{ task.id }}</span>
              <p class="text-sm font-semibold mt-0.5">Product #{{ task.productId }}</p>
            </div>
            <Tag :value="formatStatus(task.status)" :severity="statusSeverity(task.status)" />
          </div>

          <div class="grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm">
            <div>
              <p class="text-xs text-gray-500">Quantity</p>
              <p class="font-semibold">{{ task.requestedQuantity }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-500">From → To</p>
              <p>
                <span class="font-mono">#{{ task.sourceLocationId }}</span>
                <span class="text-gray-500 mx-1">→</span>
                <span class="font-mono">#{{ task.destinationLocationId }}</span>
              </p>
            </div>
            <div>
              <p class="text-xs text-gray-500">Created</p>
              <p class="text-gray-400">{{ formatDate(task.createdAt) }}</p>
            </div>
            <div v-if="task.operatorId">
              <p class="text-xs text-gray-500">Operator</p>
              <p class="text-gray-400">#{{ task.operatorId }}</p>
            </div>
          </div>

          <Button
            v-if="task.status === 'CREATED'"
            label="Take Task"
            icon="pi pi-check"
            class="w-full"
            severity="info"
            :loading="actionLoading === task.id"
            @click="assignTask(task.id)"
          />
          <Button
            v-else-if="task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'"
            label="Mark In Progress"
            icon="pi pi-spin pi-spinner"
            severity="warning"
            class="w-full"
            :loading="actionLoading === task.id"
            @click="assignTask(task.id)"
          />
        </div>
      </div>
    </main>

    <Toast />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import apiClient from '@/api/index.js'
import BarcodeScanner from '@/components/BarcodeScanner.vue'

import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Skeleton from 'primevue/skeleton'
import Toast from 'primevue/toast'
import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const tasks = ref([])
const loading = ref(false)
const error = ref('')
const actionLoading = ref(null)
const lastScanned = ref('')
const completedCount = ref(0)
const scannerOpen = ref(false)
const tasksOpen = ref(false)

const availableTasks = computed(() => tasks.value.filter((t) => t.status === 'CREATED'))
const myTasks = computed(() =>
  tasks.value.filter((t) => ['ASSIGNED', 'IN_PROGRESS'].includes(t.status)),
)

const loadTasks = async () => {
  loading.value = true
  error.value = ''
  try {
    const response = await apiClient.get('/replenishment-tasks')
    tasks.value = response.data.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED')
    completedCount.value = response.data.filter((t) => t.status === 'COMPLETED').length
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load tasks'
  } finally {
    loading.value = false
  }
}

const assignTask = async (id) => {
  actionLoading.value = id
  try {
    const response = await apiClient.patch(`/replenishment-tasks/${id}`)
    const idx = tasks.value.findIndex((t) => t.id === id)
    if (idx !== -1) tasks.value[idx] = response.data
    toast.add({ severity: 'success', summary: 'Task updated', life: 3000 })
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: e.response?.data?.error || 'Failed to update task',
      life: 4000,
    })
  } finally {
    actionLoading.value = null
  }
}

const onBarcodeDetected = (barcode) => {
  lastScanned.value = barcode
  const found = tasks.value.find((t) => String(t.productId) === barcode || String(t.id) === barcode)
  if (found) {
    tasksOpen.value = true
    toast.add({ severity: 'info', summary: `Task #${found.id} found`, life: 3000 })
  } else {
    toast.add({ severity: 'warn', summary: `No task found for: ${barcode}`, life: 3000 })
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}

const formatStatus = (status) =>
  ({
    CREATED: 'Available',
    ASSIGNED: 'Assigned',
    IN_PROGRESS: 'In Progress',
    COMPLETED: 'Done',
    CANCELLED: 'Cancelled',
  })[status] || status

const statusSeverity = (status) =>
  ({
    CREATED: 'info',
    ASSIGNED: 'warn',
    IN_PROGRESS: 'warn',
    COMPLETED: 'success',
    CANCELLED: 'secondary',
  })[status] || 'secondary'

const statusBorder = (status) =>
  ({
    CREATED: 'border-gray-700',
    ASSIGNED: 'border-yellow-700/50',
    IN_PROGRESS: 'border-orange-700/50',
    COMPLETED: 'border-green-700/50',
    CANCELLED: 'border-gray-700',
  })[status] || 'border-gray-700'

const formatDate = (timestamp) => {
  if (!timestamp) return '—'
  return new Date(timestamp).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => loadTasks())
</script>
