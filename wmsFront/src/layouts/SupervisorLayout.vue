<template>
  <div class="app-shell font-sans flex">
    <aside class="app-sidebar w-64 shadow-xl flex-col justify-between hidden md:flex">
      <div class="min-h-0">
        <SidebarBrand />
        <SidebarNavigation :items="menuItems" />
      </div>

      <div class="p-4 border-t border-white/10 flex flex-col gap-3">
        <ThemeToggle show-label class="w-full justify-center" />
        <Button
          icon="pi pi-sign-out"
          label="Logout"
          severity="danger"
          text
          class="w-full justify-start"
          @click="handleLogout"
        />
      </div>
    </aside>

    <div
      v-if="mobileMenuOpen"
      class="mobile-sidebar-backdrop md:hidden"
      @click="closeMobileMenu"
    ></div>

    <aside
      class="app-sidebar mobile-sidebar shadow-xl flex flex-col justify-between md:hidden"
      :class="{ 'mobile-sidebar--open': mobileMenuOpen }"
    >
      <div class="min-h-0">
        <div class="flex items-center justify-between border-b border-white/10">
          <SidebarBrand />
          <Button
            icon="pi pi-times"
            text
            rounded
            severity="secondary"
            aria-label="Close menu"
            class="mr-3"
            @click="closeMobileMenu"
          />
        </div>
        <SidebarNavigation :items="menuItems" @navigate="closeMobileMenu" />
      </div>

      <div class="p-4 border-t border-white/10 flex flex-col gap-3">
        <ThemeToggle show-label class="w-full justify-center" />
        <Button
          icon="pi pi-sign-out"
          label="Logout"
          severity="danger"
          text
          class="w-full justify-start"
          @click="handleLogout"
        />
      </div>
    </aside>

    <main class="flex-1 min-w-0 overflow-y-auto">
      <header
        class="app-header sticky top-0 z-30 flex items-center justify-between gap-3 p-4 md:hidden"
      >
        <div class="flex items-center gap-3">
          <i class="pi pi-box text-xl app-brand"></i>
          <span class="app-title text-lg font-bold tracking-wide">ISD WMS</span>
        </div>
        <Button
          icon="pi pi-bars"
          text
          rounded
          severity="secondary"
          aria-label="Open menu"
          @click="openMobileMenu"
        />
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Button from 'primevue/button'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)

const SidebarBrand = defineComponent({
  name: 'SidebarBrand',
  setup() {
    return () =>
      h('div', { class: 'p-6 flex items-center gap-3' }, [
        h('i', { class: 'pi pi-box text-2xl app-brand' }),
        h('h1', { class: 'app-title text-xl font-bold tracking-wide' }, 'ISD WMS'),
      ])
  },
})

const SidebarNavigation = defineComponent({
  name: 'SidebarNavigation',
  props: {
    items: {
      type: Array,
      required: true,
    },
  },
  emits: ['navigate'],
  setup(props, { emit }) {
    return () =>
      h(
        'nav',
        { class: 'p-4 flex flex-col gap-2' },
        props.items.map((item) =>
          h(
            RouterLink,
            {
              key: item.to,
              to: item.to,
              exactActiveClass: 'app-nav-link-active',
              activeClass: 'app-nav-link-active',
              class: 'app-nav-link p-3 rounded-lg transition flex items-center gap-3',
              onClick: () => emit('navigate'),
            },
            () => [h('i', { class: item.icon }), item.label],
          ),
        ),
      )
  },
})

const menuItems = computed(() => {
  const isDev = authStore.role === 'ROLE_DEV'
  const isOperator = authStore.role === 'ROLE_OPERATOR'
  if (isOperator) {
    return [{ to: '/operator', label: 'Operator Console', icon: 'pi pi-box' }]
  }

  const items = [
    { to: '/dashboard', label: 'Dashboard', icon: 'pi pi-chart-bar', exact: true },
    { to: '/supervisor/order-form', label: 'Order', icon: 'pi pi-cart-arrow-down' },
    { to: '/supervisor/replenishments', label: 'Replenishments', icon: 'pi pi-sync' },
    { to: '/supervisor/allocations', label: 'Allocations', icon: 'pi pi-list' },
    { to: '/supervisor/inventory', label: 'Inventory', icon: 'pi pi-table' },
    { to: '/supervisor/products', label: 'Products', icon: 'pi pi-tags' },
    { to: '/supervisor/locations', label: 'Locations', icon: 'pi pi-map-marker' },
    { to: '/supervisor/users', label: 'Users', icon: 'pi pi-users' },
    { to: '/supervisor/history', label: 'History', icon: 'pi pi-history' },
  ]

  if (isDev) {
    items.unshift({ to: '/supervisor/dev', label: 'Dev Overview', icon: 'pi pi-server' })
  }

  return items
})

const openMobileMenu = () => {
  mobileMenuOpen.value = true
}

const closeMobileMenu = () => {
  mobileMenuOpen.value = false
}

const handleLogout = () => {
  closeMobileMenu()
  authStore.logout()
  router.push({ name: 'login', query: { loggedOut: '1' } })
}

watch(
  () => route.fullPath,
  () => closeMobileMenu(),
)
</script>

<style scoped>
.mobile-sidebar {
  bottom: 0;
  left: 0;
  max-width: 20rem;
  position: fixed;
  top: 0;
  transform: translateX(-100%);
  transition: transform 0.2s ease;
  width: min(82vw, 20rem);
  z-index: 50;
}

.mobile-sidebar--open {
  transform: translateX(0);
}

.mobile-sidebar-backdrop {
  background: rgba(0, 0, 0, 0.48);
  inset: 0;
  position: fixed;
  z-index: 40;
}
</style>
