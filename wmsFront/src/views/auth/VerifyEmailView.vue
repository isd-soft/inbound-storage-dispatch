<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-900 p-4">
    <Card class="w-full max-w-md bg-gray-800 border-none shadow-2xl text-center p-4">
      <template #content>
        <div v-if="loading" class="flex flex-col items-center gap-4 py-6">
          <i class="pi pi-spin pi-spinner text-4xl text-blue-500"></i>
          <h2 class="text-xl font-semibold text-gray-200">Verifying your email...</h2>
        </div>

        <div v-else-if="success" class="flex flex-col items-center gap-4 py-6">
          <div class="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mb-2">
            <i class="pi pi-check text-4xl text-green-500"></i>
          </div>
          <h2 class="text-2xl font-bold text-gray-100">Email Confirmed!</h2>
          <p class="text-gray-400">Your account has been successfully activated.</p>
          <Button label="Go to Login" icon="pi pi-sign-in" class="mt-4 w-full" @click="goToLogin" />
        </div>

        <div v-else class="flex flex-col items-center gap-4 py-6">
          <div class="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mb-2">
            <i class="pi pi-times text-4xl text-red-500"></i>
          </div>
          <h2 class="text-2xl font-bold text-gray-100">Verification Failed</h2>
          <p class="text-red-400">{{ errorMessage }}</p>
          <p class="text-gray-400 text-sm mt-2">The link might be expired or invalid. Please ask to resend the invitation.</p>
          <Button label="Back to Login" severity="secondary" outlined class="mt-4 w-full" @click="goToLogin" />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/authApi'
import Card from 'primevue/card'
import Button from 'primevue/button'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const success = ref(false)
const errorMessage = ref('')

const goToLogin = () => {
  router.push('/login')
}

onMounted(async () => {
  const token = route.query.token

  if (!token) {
    loading.value = false
    success.value = false
    errorMessage.value = 'No verification token provided in the URL.'
    return
  }

  try {
    await authApi.verify(token)
    success.value = true
  } catch (error) {
    success.value = false
    errorMessage.value = error.response?.data?.error || 'Failed to verify email.'
  } finally {
    loading.value = false
  }
})
</script>
