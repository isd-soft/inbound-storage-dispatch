<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-900 p-4">
    <Toast />
    <Card class="w-full max-w-md bg-gray-800 border-none shadow-2xl text-center p-4">
      <template #content>
        <div v-if="!success && !fatalError" class="flex flex-col gap-4 py-2">
          <div class="w-16 h-16 bg-blue-500/20 rounded-full flex items-center justify-center mx-auto mb-2">
            <i class="pi pi-user-edit text-4xl text-blue-500"></i>
          </div>
          <h2 class="text-2xl font-bold text-gray-100">Activate Your Account</h2>
          <p class="text-gray-400 text-sm mb-4">Please set a password for your new account.</p>

          <form @submit.prevent="submitVerification" class="flex flex-col gap-4 text-left">
            <div class="flex flex-col gap-2">
              <label for="password" class="text-gray-300 font-medium">New Password <span class="text-red-500">*</span></label>
              <small class="text-gray-500 text-xs mb-1">
                Must be 8-64 chars, min. 1 uppercase, 1 lowercase, 1 digit and 1 special char (@$!%*?&_#).
              </small>
              <Password
                id="password"
                v-model="password"
                toggleMask
                required
                promptLabel="Choose a password"
                weakLabel="Weak password"
                mediumLabel="Medium strength"
                strongLabel="Strong password"
                :mediumRegex="passwordMediumRegex"
                :strongRegex="passwordStrongRegex"
                inputClass="w-full"
                class="w-full [&>input]:w-full"
              />
            </div>

            <div class="flex flex-col gap-2 mt-2">
              <label for="confirmPassword" class="text-gray-300 font-medium">Confirm Password <span class="text-red-500">*</span></label>
              <Password
                id="confirmPassword"
                v-model="confirmPassword"
                :feedback="false"
                toggleMask
                required
                inputClass="w-full"
                class="w-full [&>input]:w-full"
              />
              <small v-if="confirmPassword && !isPasswordMatching" class="text-red-500 text-xs mt-1">
                Passwords do not match.
              </small>
            </div>

            <Button
              type="submit"
              label="Set Password & Activate"
              icon="pi pi-check"
              class="mt-4 w-full"
              :loading="loading"
              :disabled="!isFormValid"
            />
          </form>
        </div>

        <div v-else-if="success" class="flex flex-col items-center gap-4 py-6">
          <div class="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mb-2">
            <i class="pi pi-check text-4xl text-green-500"></i>
          </div>
          <h2 class="text-2xl font-bold text-gray-100">Account Activated!</h2>
          <p class="text-gray-400">Your password has been set successfully.</p>
          <Button label="Go to Login" icon="pi pi-sign-in" class="mt-4 w-full" @click="goToLogin" />
        </div>

        <div v-else class="flex flex-col items-center gap-4 py-6">
          <div class="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mb-2">
            <i class="pi pi-times text-4xl text-red-500"></i>
          </div>
          <h2 class="text-2xl font-bold text-gray-100">Activation Failed</h2>
          <p class="text-red-400">{{ errorMessage }}</p>
          <p class="text-gray-400 text-sm mt-2">The link might be expired or invalid. Please ask to resend the invitation.</p>
          <Button label="Back to Login" severity="secondary" outlined class="mt-4 w-full" @click="goToLogin" />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { authApi } from '@/api/authApi'

import Card from 'primevue/card'
import Button from 'primevue/button'
import Password from 'primevue/password'
import Toast from 'primevue/toast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const token = ref('')
const password = ref('')
const confirmPassword = ref('')

const loading = ref(false)
const success = ref(false)
const fatalError = ref(false)
const errorMessage = ref('')

const passwordMediumRegex = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,64}$'
const passwordStrongRegex = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#])[A-Za-z\\d@$!%*?&_#]{8,64}$'

const isPasswordMatching = computed(() => {
  return password.value === confirmPassword.value
})

const isPasswordStrongEnough = computed(() => {
  const regex = new RegExp(passwordStrongRegex)
  return regex.test(password.value)
})

const isFormValid = computed(() => {
  return password.value.trim() && isPasswordStrongEnough.value && isPasswordMatching.value
})

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  token.value = route.query.token

  if (!token.value) {
    fatalError.value = true
    errorMessage.value = 'No verification token provided in the URL.'
  }
})

const submitVerification = async () => {
  if (!isFormValid.value) return

  loading.value = true
  try {
    await authApi.verify({
      token: token.value,
      password: password.value
    })
    success.value = true
  } catch (error) {
    fatalError.value = true
    errorMessage.value = error.response?.data?.error || 'Failed to verify email.'
    toast.add({ severity: 'error', summary: 'Error', detail: errorMessage.value, life: 5000 })
  } finally {
    loading.value = false
  }
}
</script>
