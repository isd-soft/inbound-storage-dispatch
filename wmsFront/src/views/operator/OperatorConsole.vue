<template>
  <div class="min-h-screen bg-gray-50 p-4">
    <div class="flex justify-between items-center bg-white p-4 rounded-lg shadow-sm mb-4">
      <h1 class="text-xl font-bold text-gray-800">Operator_Console</h1>

      <button
        @click="handleLogout"
        class="px-3 py-1.5 border border-red-600 text-red-600 text-sm font-medium rounded-md hover:bg-red-50 transition-colors"
      >
        Exit Light
      </button>
    </div>

    <div class="bg-white p-4 rounded-lg shadow-sm border border-gray-100 flex flex-col items-center justify-center h-64">
      <p class="text-gray-500 text-center">No tasks :(.</p>
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
