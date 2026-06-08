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

    <main class="p-4 max-w-md mx-auto mt-8">
      <div class="text-center mb-8">
        <h2 class="app-title text-2xl font-bold">Ready for Work</h2>
        <p class="app-subtitle mt-1">Active Task: <span class="app-warm font-medium">None</span></p>
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
import ThemeToggle from '@/components/ThemeToggle.vue'
import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}
</script>
