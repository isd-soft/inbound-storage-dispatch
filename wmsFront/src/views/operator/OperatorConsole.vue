<template>
  <div class="min-h-screen bg-gray-900 text-gray-100 font-sans">

    <header class="flex justify-between items-center p-4 bg-gray-800 shadow-md">
      <div class="flex items-center gap-3">
        <i class="pi pi-box text-2xl text-orange-400"></i>
        <h1 class="text-xl font-bold tracking-wide">Operator</h1>
      </div>
      <Button icon="pi pi-sign-out" severity="danger" text rounded @click="handleLogout" />
    </header>

    <main class="p-4 max-w-md mx-auto mt-8">
      <div class="text-center mb-8">
        <h2 class="text-2xl font-bold text-gray-100">Ready for Work</h2>
        <p class="text-gray-400 mt-1">Active Task: <span class="text-orange-400 font-medium">None</span></p>
      </div>

      <div class="flex flex-col gap-5">
        <Button
          label="Scan Location / SKU"
          icon="pi pi-barcode"
          class="p-4 text-lg w-full font-bold"
          severity="info"
          raised
        />
        <Button
          label="My Assigned Tasks"
          icon="pi pi-list"
          badge="2"
          badgeSeverity="danger"
          class="p-4 text-lg w-full font-bold"
          severity="success"
          raised
        />
        <Button
          label="Report Issue"
          icon="pi pi-exclamation-triangle"
          class="p-4 text-lg w-full"
          severity="danger"
          outlined
        />
      </div>
    </main>

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import Button from 'primevue/button'
import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}
</script>
