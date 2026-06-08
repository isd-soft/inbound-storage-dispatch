<template>
  <div class="min-h-screen bg-gray-900 flex items-center justify-center p-6">
    <Card class="w-full max-w-md bg-gray-800 border-none shadow-lg text-center">
      <template #title>
        <div class="flex flex-col items-center gap-3 text-gray-100">
          <i class="pi pi-lock text-4xl text-red-400"></i>
          <span>Access Denied</span>
        </div>
      </template>
      <template #content>
        <p class="text-gray-400 mb-6">Your current role does not allow access to this page.</p>
        <Button label="Return to Dashboard" icon="pi pi-arrow-left" @click="goToDashboard" />
      </template>
    </Card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

import Button from 'primevue/button'
import Card from 'primevue/card'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const goToDashboard = () => {
  router.push(authStore.dashboardPath)
}
</script>
