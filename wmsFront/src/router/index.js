import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import LoginView from '../views/auth/LoginView.vue'
import SupervisorLayout from '../layouts/SupervisorLayout.vue'
import SuperDashboard from '../views/supervisor/SuperDashboard.vue'
import ProductsView from '../views/supervisor/ProductsView.vue'
import LocationsView from '../views/supervisor/LocationsView.vue'
import InventoryView from '../views/supervisor/InventoryView.vue'
import TasksView from '../views/supervisor/TasksView.vue'
import HistoryView from '../views/supervisor/HistoryView.vue'
import UsersView from '../views/supervisor/UsersView.vue'
import OperatorConsole from '../views/operator/OperatorConsole.vue'
import DevDashboard from '../views/dev/DevDashboard.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true }
    },
    {
      path: '/supervisor',
      component: SupervisorLayout,
      meta: { requiresAuth: true, role: 'ROLE_SUPERVISOR' },
      children: [
        { path: '', name: 'supervisor-dashboard', component: SuperDashboard },
        { path: 'inventory', name: 'inventory', component: InventoryView },
        { path: 'tasks', name: 'tasks', component: TasksView },
        { path: 'products', name: 'products', component: ProductsView },
        { path: 'locations', name: 'locations', component: LocationsView },
        { path: 'history', name: 'history', component: HistoryView },
        { path: 'users', name: 'users', component: UsersView }
      ]
    },
    {
      path: '/operator',
      name: 'operator',
      component: OperatorConsole,
      meta: { requiresAuth: true, role: 'ROLE_OPERATOR' }
    },
    {
      path: '/dev',
      name: 'dev',
      component: DevDashboard,
      meta: { requiresAuth: true, role: 'ROLE_DEV' }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = !!authStore.token
  const userRole = authStore.role

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      return next('/')
    }

    if (to.meta.role && to.meta.role !== userRole) {
      if (userRole === 'ROLE_SUPERVISOR') return next('/supervisor')
      if (userRole === 'ROLE_OPERATOR') return next('/operator')
      if (userRole === 'ROLE_DEV') return next('/dev')
      return next('/')
    }
  }

  if (to.meta.guestOnly && isAuthenticated) {
    if (userRole === 'ROLE_SUPERVISOR') return next('/supervisor')
    if (userRole === 'ROLE_OPERATOR') return next('/operator')
    if (userRole === 'ROLE_DEV') return next('/dev')
  }

  next()
})

export default router
