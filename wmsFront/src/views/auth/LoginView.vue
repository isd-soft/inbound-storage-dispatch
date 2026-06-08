<template>
  <div class="min-h-screen bg-gray-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <Toast />

    <div class="sm:mx-auto sm:w-full sm:max-w-md mb-8">
      <h2 class="text-center text-3xl font-extrabold text-blue-400">
        Inbound Storage Dispatch
      </h2>
      <p class="mt-2 text-center text-sm text-blue-200">
        Welcome to WMS
      </p>
    </div>

    <Card class="sm:mx-auto sm:w-full sm:max-w-md shadow-lg bg-gray-800 border-none">
      <template #content>
        <form @submit.prevent="handleLogin" class="flex flex-col gap-4">

          <div class="flex flex-col gap-2">
            <label for="username" class="font-medium text-gray-300">Username or Email</label>
            <InputText
              id="username"
              v-model.trim="username"
              type="text"
              placeholder="You shall not pass!"
              class="w-full"
            />
            <small v-if="submitted && !username" class="text-red-400">Username or email is required.</small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="password" class="font-medium text-gray-300">Password</label>
            <Password
              id="password"
              v-model="password"
              :feedback="false"
              toggleMask
              inputClass="w-full"
              class="w-full [&>input]:w-full"
            />
            <small v-if="submitted && !password" class="text-red-400">Password is required.</small>
          </div>

          <Message v-if="errorMessage" severity="error" :closable="false">
            {{ errorMessage }}
          </Message>

          <Button
            type="submit"
            :label="isLoading ? 'Logging In...' : 'Log In'"
            :loading="isLoading"
            class="w-full mt-2"
          />

        </form>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'

import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Toast from 'primevue/toast'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)
const submitted = ref(false)

const getErrorMessage = (error) => {
  return error.response?.data?.error || error.response?.data?.message || error.message || 'Wrong Username or Password'
}

const handleLogin = async () => {
  submitted.value = true
  errorMessage.value = ''

  if (!username.value || !password.value) {
    errorMessage.value = 'Username and password are required.'
    return
  }

  isLoading.value = true

  try {
    await authStore.login(username.value, password.value)

    router.push(route.query.redirect || authStore.dashboardPath)
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
    toast.add({ severity: 'error', summary: 'Login failed', detail: errorMessage.value, life: 4000 })
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  if (route.query.loggedOut) {
    toast.add({ severity: 'success', summary: 'Logged out', detail: 'You have been logged out successfully.', life: 3000 })
  }
})
</script>
