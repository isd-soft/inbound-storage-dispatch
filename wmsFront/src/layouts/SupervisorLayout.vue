<template>
  <div class="min-h-screen bg-gray-900 text-gray-100 font-sans flex">

    <aside class="w-64 bg-gray-800 shadow-xl flex flex-col justify-between hidden md:flex">
      <div>
        <div class="p-6 flex items-center gap-3 border-b border-gray-700">
          <i class="pi pi-box text-2xl text-blue-400"></i>
          <h1 class="text-xl font-bold tracking-wide">ISD WMS</h1>
        </div>
        <nav class="p-4 flex flex-col gap-2">
          <router-link to="/supervisor" exact-active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-chart-bar"></i> Dashboard
          </router-link>
          <router-link to="/supervisor/inventory" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-table"></i> Inventory
          </router-link>
          <router-link to="/supervisor/tasks" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-check-square"></i> Tasks
          </router-link>
          <router-link to="/supervisor/products" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-tags"></i> Products
          </router-link>
          <router-link to="/supervisor/locations" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-map-marker"></i> Locations
          </router-link>
          <router-link to="/supervisor/history" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-history"></i> History
          </router-link>
          <router-link to="/supervisor/users" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i class="pi pi-users"></i> Users
          </router-link>
        </nav>
      </div>

      <div class="p-4 border-t border-gray-700">
        <Button icon="pi pi-sign-out" label="Logout" severity="danger" text class="w-full justify-start" @click="handleLogout" />
      </div>
    </aside>

    <main class="flex-1 overflow-y-auto">
      <router-view />
    </main>

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Button from 'primevue/button'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}
</script>
