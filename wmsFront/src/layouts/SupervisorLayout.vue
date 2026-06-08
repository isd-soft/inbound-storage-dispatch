<template>
  <div class="min-h-screen bg-gray-900 text-gray-100 font-sans flex">

    <aside class="w-64 bg-gray-800 shadow-xl flex flex-col justify-between hidden md:flex">
      <div>
        <div class="p-6 flex items-center gap-3 border-b border-gray-700">
          <i class="pi pi-box text-2xl text-blue-400"></i>
          <h1 class="text-xl font-bold tracking-wide">ISD WMS</h1>
        </div>
        <nav class="p-4 flex flex-col gap-2">
          <router-link v-for="item in menuItems" :key="item.to" :to="item.to" :exact-active-class="item.exact ? 'bg-blue-600 text-white' : undefined" active-class="bg-blue-600 text-white" class="p-3 rounded-lg hover:bg-gray-700 transition flex items-center gap-3">
            <i :class="item.icon"></i> {{ item.label }}
          </router-link>
        </nav>
      </div>

      <div class="p-4 border-t border-gray-700">
        <Button icon="pi pi-sign-out" label="Logout" severity="danger" text class="w-full justify-start" @click="handleLogout" />
      </div>
    </aside>

    <main class="flex-1 overflow-y-auto">
      <router-view />
    </main>

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'
import Button from 'primevue/button'

const router = useRouter()
const authStore = useAuthStore()

const menuItems = computed(() => {
  const isDev = authStore.role === 'ROLE_DEV'
  const items = [
    { to: '/supervisor', label: 'Dashboard', icon: 'pi pi-chart-bar', exact: true },
    { to: '/supervisor/inventory', label: 'Inventory', icon: 'pi pi-table' },
    { to: '/supervisor/tasks', label: 'Tasks', icon: 'pi pi-check-square' },
    { to: '/supervisor/products', label: 'Products', icon: 'pi pi-tags' },
    { to: '/supervisor/locations', label: 'Locations', icon: 'pi pi-map-marker' },
    { to: '/supervisor/history', label: 'History', icon: 'pi pi-history' },
    { to: '/supervisor/users', label: 'Users', icon: 'pi pi-users' }
  ]

  if (isDev) {
    items.unshift({ to: '/dev', label: 'Dev Overview', icon: 'pi pi-server' })
    items.push({ to: '/operator', label: 'Operator Console', icon: 'pi pi-box' })
  }

  return items
})

const handleLogout = () => {
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}
</script>
