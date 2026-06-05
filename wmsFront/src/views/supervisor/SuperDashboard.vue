<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="flex justify-between items-center bg-white p-4 rounded-lg shadow-sm mb-6">
      <h1 class="text-2xl font-bold text-gray-800">Super_Dashboard</h1>

      <button
        @click="handleLogout"
        class="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-md hover:bg-red-700 transition-colors"
      >
        Exit light
      </button>
    </div>

    <div class="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      <p class="text-gray-500">Soon......</p>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()

  router.push('/login')
}
</script>
