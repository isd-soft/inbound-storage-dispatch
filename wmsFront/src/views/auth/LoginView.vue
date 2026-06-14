<template>
  <div class="app-shell relative flex flex-col items-center justify-center px-6 py-12 sm:px-6 lg:px-8">
    <Toast />
    <div class="absolute right-6 top-6">
      <ThemeToggle />
    </div>

    <div class="w-full max-w-md mb-8">
      <img src="/Logo.png" alt="Inbound Storage Dispatch logo" class="mx-auto mb-5 h-20 w-auto" />
      <h2 class="app-brand text-center text-3xl font-extrabold">
        Inbound Storage Dispatch
      </h2>
      <p class="app-subtitle mt-2 text-center text-sm">
        Welcome to WMS
      </p>
    </div>

    <Card class="app-card w-full max-w-md">
      <template #content>
        <form @submit.prevent="handleLogin" class="flex flex-col gap-4">

          <div class="flex flex-col gap-2">
            <label for="username" class="app-subtitle font-medium">Username or Email</label>
            <InputText
              id="username"
              v-model.trim="username"
              type="text"
              placeholder="You shall not pass!"
              class="w-full"
            />
            <small v-if="submitted && !username" class="app-danger">Username or email is required.</small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="password" class="app-subtitle font-medium">Password</label>
            <Password
              id="password"
              v-model="password"
              :feedback="false"
              toggleMask
              inputClass="w-full"
              class="w-full [&>input]:w-full"
            />
            <small v-if="submitted && !password" class="app-danger">Password is required.</small>
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
import ThemeToggle from '@/components/ThemeToggle.vue'

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
