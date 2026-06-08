<template>
  <div class="min-h-screen bg-gray-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
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
            <label for="email" class="font-medium text-gray-300">Username or Email</label>
            <InputText
              id="email"
              v-model="email"
              type="text"
              required
              placeholder="You shall not pass!"
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-2">
            <label for="password" class="font-medium text-gray-300">Password</label>
            <Password
              id="password"
              v-model="password"
              :feedback="false"
              toggleMask
              required
              inputClass="w-full"
              class="w-full [&>input]:w-full"
            />
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(false)

const handleLogin = async () => {
  errorMessage.value = ''
  isLoading.value = true

  try {
    const result = await authStore.login(email.value, password.value)

    if (result.role === 'ROLE_SUPERVISOR') {
      router.push('/supervisor')
    } else if (result.role === 'ROLE_OPERATOR') {
      router.push('/operator')
    } else if (result.role === 'ROLE_DEV') {
      router.push('/dev')
    }
  } catch (error) {
    errorMessage.value = error.message || 'Wrong Username or Password'
  } finally {
    isLoading.value = false
  }
}
</script>
