import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import LoginView from '../views/auth/LoginView.vue'
import SuperDashboard from '../views/supervisor/SuperDashboard.vue'
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
      name: 'supervisor',
      component: SuperDashboard,
      meta: { requiresAuth: true, role: 'ROLE_SUPERVISOR' }
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
