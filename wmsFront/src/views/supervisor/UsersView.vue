<template>
  <div class="p-6">
    <Toast />

    <div class="flex justify-between items-center mb-6">
      <h2 class="app-title text-2xl font-bold">System Users</h2>
      <Button label="Add User" icon="pi pi-user-plus" severity="success" @click="openCreateDialog" />
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable
          :value="users"
          :loading="loading"
          stripedRows
          class="p-datatable-sm"
          emptyMessage="No users found."
        >
          <Column field="id" header="ID"></Column>
          <Column field="username" header="Username" sortable></Column>
          <Column field="email" header="Email"></Column>
          <Column field="userRole" header="Role" sortable>
            <template #body="{ data }">
              <Tag :severity="getRoleSeverity(data.userRole)" :value="data.userRole" />
            </template>
          </Column>
          <Column header="Actions">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" outlined rounded size="small" class="mr-2" severity="warning" />
              <Button icon="pi pi-ban" outlined rounded severity="danger" size="small" @click="banUser(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog v-model:visible="dialogVisible" header="Register New User" :modal="true" class="p-fluid w-full max-w-md">

      <div class="field mb-4">
        <label for="username" class="block text-sm font-medium mb-1">Username *</label>
        <InputText id="username" v-model="formData.username" required autofocus />
      </div>

      <div class="field mb-4">
        <label for="email" class="block text-sm font-medium mb-1">Email *</label>
        <InputText id="email" type="email" v-model="formData.email" required />
      </div>

      <div class="field mb-4">
        <label for="password" class="block text-sm font-medium mb-1">Password *</label>
        <Password id="password" v-model="formData.password" :feedback="false" toggleMask required />
      </div>

      <div class="field mb-4">
        <label for="role" class="block text-sm font-medium mb-1">Role *</label>
        <Dropdown id="role" v-model="formData.userRole" :options="roles" placeholder="Select a Role" />
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text @click="dialogVisible = false" />
        <Button label="Register" icon="pi pi-check" severity="success" :loading="actionLoading" @click="registerUser" :disabled="!isFormValid" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'

import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Dropdown from 'primevue/dropdown'
import Toast from 'primevue/toast'

import { userApi } from '@/api/userApi'
import { useAuthStore } from '@/stores/auth'

const toast = useToast()
const authStore = useAuthStore()

const users = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)

const roles = computed(() => {
  return authStore.role === 'ROLE_DEV'
    ? ['ROLE_SUPERVISOR', 'ROLE_OPERATOR', 'ROLE_DEV']
    : ['ROLE_SUPERVISOR', 'ROLE_OPERATOR']
})

const formData = ref({
  username: '',
  email: '',
  password: '',
  userRole: null
})

const isFormValid = computed(() => {
  return formData.value.username.trim() &&
    formData.value.email.trim() &&
    formData.value.password.trim() &&
    formData.value.userRole
})

const getRoleSeverity = (role) => {
  if (role === 'ROLE_SUPERVISOR') return 'warning'
  if (role === 'ROLE_DEV') return 'danger'
  return 'success'
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getAll()
    users.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Load Failed', detail: error.message, life: 4000 })
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  formData.value = { username: '', email: '', password: '', userRole: null }
  dialogVisible.value = true
}

const registerUser = async () => {
  actionLoading.value = true
  try {
    await userApi.register(formData.value)
    toast.add({ severity: 'success', summary: 'Success', detail: 'User registered successfully', life: 3000 })
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    const errorMsg = error.response?.data?.error || 'Registration failed'
    toast.add({ severity: 'error', summary: 'Error', detail: errorMsg, life: 5000 })
  } finally {
    actionLoading.value = false
  }
}

const banUser = (user) => {
  toast.add({ severity: 'info', summary: 'WIP', detail: `Ban functionality for ${user.username} is not implemented yet.`, life: 3000 })
}

onMounted(() => {
  loadUsers()
})
</script>
