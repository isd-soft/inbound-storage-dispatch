<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-2xl font-bold text-gray-100">System Users</h2>
      <Button label="Add User" icon="pi pi-user-plus" severity="success" />
    </div>

    <Card class="bg-gray-800 border-none shadow-lg">
      <template #content>
        <DataTable :value="mockUsers" stripedRows class="p-datatable-sm">
          <Column field="id" header="ID"></Column>
          <Column field="username" header="Username" sortable></Column>
          <Column field="email" header="Email"></Column>
          <Column field="role" header="Role" sortable>
            <template #body="slotProps">
              <Tag :severity="getRoleSeverity(slotProps.data.role)" :value="slotProps.data.role" />
            </template>
          </Column>
          <Column header="Actions">
            <template #body>
              <Button icon="pi pi-pencil" outlined rounded size="small" class="mr-2" />
              <Button icon="pi pi-ban" outlined rounded severity="danger" size="small" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const mockUsers = ref([
  { id: 1, username: 'supervisor_admin', email: 'supervisor@gmail.com', role: 'ROLE_SUPERVISOR' },
  { id: 2, username: 'smoothOperator', email: 'operator@gmail.com', role: 'ROLE_OPERATOR' },
  { id: 3, username: 'dev_master', email: 'dev@gmail.com', role: 'ROLE_DEV' }
])

const getRoleSeverity = (role) => {
  if (role === 'ROLE_SUPERVISOR') return 'warning'
  if (role === 'ROLE_DEV') return 'danger'
  return 'success'
}
</script>
