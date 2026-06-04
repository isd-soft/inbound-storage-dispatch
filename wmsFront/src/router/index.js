import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue')
    },
    {
      path: '/supervisor',
      name: 'supervisor',
      component: () => import('@/views/supervisor/SuperDashboard.vue'),
      meta: { requiresAuth: true, role: 'SUPERVISOR' }
    },
    {
      path: '/operator',
      name: 'operator',
      component: () => import('@/views/operator/OperatorConsole.vue'),
      meta: { requiresAuth: true, role: 'OPERATOR' }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = !!authStore.token
  const userRole = authStore.role

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      return next('/login')
    }
    if (to.meta.role && to.meta.role !== userRole) {
      return next(userRole === 'SUPERVISOR' ? '/supervisor' : '/operator')
    }
  }
  else if (to.path === '/login' && isAuthenticated) {
    return next(userRole === 'SUPERVISOR' ? '/supervisor' : '/operator')
  }

  next()
})

export default router
