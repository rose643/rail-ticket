import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', component: () => import('@/views/RoleSelectView.vue') },
    { path: '/login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', component: () => import('@/views/RegisterView.vue') },
    { path: '/user', component: () => import('@/layouts/UserLayout.vue'), meta: { role: 'USER' }, children: [
      { path: '', redirect: '/user/search' },
      { path: 'search', component: () => import('@/views/user/UserHomeView.vue') },
      { path: 'passengers', component: () => import('@/views/user/PassengersView.vue') },
      { path: 'orders', component: () => import('@/views/user/OrdersView.vue') },
      { path: 'tickets', component: () => import('@/views/user/TicketsView.vue') },
      { path: 'notifications', component: () => import('@/views/user/NotificationsView.vue') },
      { path: 'profile', component: () => import('@/views/user/ProfileView.vue') },
    ] },
    { path: '/admin', component: () => import('@/layouts/AdminLayout.vue'), meta: { role: 'ADMIN' }, children: [
      { path: '', component: () => import('@/views/admin/AdminOverviewView.vue') },
      { path: 'stations', component: () => import('@/views/admin/StationsView.vue') },
      { path: 'trains', component: () => import('@/views/admin/TrainsView.vue') },
      { path: 'trains/:id', component: () => import('@/views/admin/TrainDetailView.vue') },
    ] },
    { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFoundView.vue') },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const requiredRole = to.matched.find((record) => record.meta.role)?.meta.role
  if (!requiredRole) return true
  if (!auth.isLoggedIn) return { path: '/login', query: { role: String(requiredRole), redirect: to.fullPath } }
  if (auth.user?.role !== requiredRole) return auth.user?.role === 'ADMIN' ? '/admin' : '/user'
  return true
})

export default router
