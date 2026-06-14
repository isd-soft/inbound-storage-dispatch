<template>
  <div class="p-6">
    <Toast />
    <ConfirmDialog /> <div class="flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
    <div>
      <h2 class="app-title text-2xl font-bold">System Users</h2>
      <p class="app-subtitle text-sm mt-1">Manage personnel access and system roles.</p>
    </div>
    <Button label="Add User" icon="pi pi-user-plus" severity="success" @click="openCreateDialog" />
  </div>

    <Card class="app-card border-none shadow-lg">
      <template #content>
        <DataTable
          :value="users"
          :loading="loading"
          stripedRows
          class="p-datatable-sm"
          emptyMessage="No users found."
        >
          <Column field="id" header="ID"></Column>
          <Column field="username" header="Username" sortable>
            <template #body="{ data }">
              <span class="app-title font-semibold">{{ data.username }}</span>
            </template>
          </Column>
          <Column field="email" header="Email"></Column>
          <Column field="userRole" header="Role" sortable>
            <template #body="{ data }">
              <Tag :severity="getRoleSeverity(data.userRole)" :value="data.userRole" />
            </template>
          </Column>
          <Column header="Actions">
            <template #body="{ data }">
              <div class="flex gap-2">
                <Button
                  v-if="isDev"
                  icon="pi pi-pencil"
                  outlined
                  rounded
                  size="small"
                  severity="warning"
                  @click="openEditDialog(data)"
                />
                <Button
                  v-if="canDelete(data)"
                  icon="pi pi-trash"
                  outlined
                  rounded
                  severity="danger"
                  size="small"
                  @click="deleteUser(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'add' ? 'Register New User' : 'Edit User'"
      :modal="true"
      class="w-full max-w-md"
    >
      <div class="flex flex-col gap-4 mt-2">
        <div class="flex flex-col gap-2">
          <label for="username" class="app-subtitle font-medium">Username <span class="text-red-500">*</span></label>
          <InputText id="username" v-model="formData.username" required autofocus class="w-full" />
        </div>

        <template v-if="dialogMode === 'add'">
          <div class="flex flex-col gap-2">
            <label for="email" class="app-subtitle font-medium">Email <span class="text-red-500">*</span></label>
            <InputText id="email" type="email" v-model="formData.email" required class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="password" class="app-subtitle font-medium">Password <span class="text-red-500">*</span></label>
            <Password id="password" v-model="formData.password" :feedback="false" toggleMask required inputClass="w-full" class="w-full" />
          </div>
        </template>

        <div class="flex flex-col gap-2">
          <label for="role" class="app-subtitle font-medium">Role <span class="text-red-500">*</span></label>
          <Dropdown id="role" v-model="formData.userRole" :options="roles" placeholder="Select a Role" class="w-full" />
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" icon="pi pi-times" text severity="secondary" @click="dialogVisible = false" />
        <Button
          :label="dialogMode === 'add' ? 'Register' : 'Save Changes'"
          icon="pi pi-check"
          :severity="dialogMode === 'add' ? 'success' : 'warning'"
          :loading="actionLoading"
          @click="submitAction"
          :disabled="!isFormValid"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

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
import ConfirmDialog from 'primevue/confirmdialog'

import { userApi } from '@/api/userApi'
import { useAuthStore } from '@/stores/auth'

const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()

const users = ref([])
const loading = ref(false)
const actionLoading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('add')

const isDev = computed(() => authStore.role === 'ROLE_DEV')

const formData = ref({
  id: null,
  username: '',
  email: '',
  password: '',
  userRole: null,
  originalRole: null
})

const roles = computed(() => {
  if (dialogMode.value === 'edit' && formData.value.originalRole === 'ROLE_DEV') {
    return ['ROLE_SUPERVISOR', 'ROLE_OPERATOR', 'ROLE_DEV']
  }
  return ['ROLE_SUPERVISOR', 'ROLE_OPERATOR']
})

const isFormValid = computed(() => {
  if (dialogMode.value === 'add') {
    return formData.value.username.trim() &&
      formData.value.email.trim() &&
      formData.value.password.trim() &&
      formData.value.userRole
  } else {
    return formData.value.username.trim() && formData.value.userRole
  }
})

const getRoleSeverity = (role) => {
  if (role === 'ROLE_SUPERVISOR') return 'warning'
  if (role === 'ROLE_DEV') return 'danger'
  return 'success'
}

const canDelete = (user) => {
  if (user.username === authStore.username) return false
  if (authStore.role === 'ROLE_DEV') return true
  if (authStore.role === 'ROLE_SUPERVISOR' && user.userRole === 'ROLE_OPERATOR') return true
  return false
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
  dialogMode.value = 'add'
  formData.value = { id: null, username: '', email: '', password: '', userRole: null, originalRole: null }
  dialogVisible.value = true
}

const openEditDialog = (user) => {
  dialogMode.value = 'edit'
  formData.value = {
    id: user.id,
    username: user.username,
    email: '',
    password: '',
    userRole: user.userRole,
    originalRole: user.userRole
  }
  dialogVisible.value = true
}

const submitAction = async () => {
  if (dialogMode.value === 'add') {
    await registerUser()
  } else {
    await updateUser()
  }
}

const handleBackendError = (error) => {
  if (error.response?.status === 400 && typeof error.response.data === 'object' && !error.response.data.error) {
    for (const [field, msg] of Object.entries(error.response.data)) {
      toast.add({ severity: 'error', summary: `Invalid ${field}`, detail: msg, life: 6000 })
    }
  } else {
    const errorMsg = error.response?.data?.error || 'Operation failed'
    toast.add({ severity: 'error', summary: 'Error', detail: errorMsg, life: 5000 })
  }
}

const registerUser = async () => {
  actionLoading.value = true
  try {
    const payload = {
      username: formData.value.username,
      email: formData.value.email,
      password: formData.value.password,
      userRole: formData.value.userRole
    }
    await userApi.register(payload)
    toast.add({ severity: 'success', summary: 'Success', detail: 'User registered successfully', life: 3000 })
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    handleBackendError(error)
  } finally {
    actionLoading.value = false
  }
}

const updateUser = async () => {
  actionLoading.value = true
  try {
    const payload = {
      username: formData.value.username,
      userRole: formData.value.userRole
    }
    await userApi.update(formData.value.id, payload)
    toast.add({ severity: 'success', summary: 'Success', detail: 'User updated successfully', life: 3000 })
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    handleBackendError(error)
  } finally {
    actionLoading.value = false
  }
}

const deleteUser = (user) => {
  confirm.require({
    message: `Are you sure you want to permanently delete user '${user.username}'?`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      loading.value = true
      try {
        await userApi.delete(user.id)
        toast.add({ severity: 'success', summary: 'Deleted', detail: 'User deleted successfully', life: 3000 })
        await loadUsers()
      } catch (error) {
        handleBackendError(error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  loadUsers()
})
</script>
