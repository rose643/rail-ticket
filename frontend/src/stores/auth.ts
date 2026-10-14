import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/services/api'
import { clearStoredSession } from '@/services/http'
import type { Role, User } from '@/types'

const USER_KEY = 'rail_user'
const ACCESS_KEY = 'rail_access_token'
const REFRESH_KEY = 'rail_refresh_token'

export const useAuthStore = defineStore('auth', () => {
  const raw = localStorage.getItem(USER_KEY)
  const user = ref<User | null>(raw ? JSON.parse(raw) : null)
  const isLoggedIn = computed(() => Boolean(user.value && localStorage.getItem(ACCESS_KEY)))
  const save = (result: { user: User; accessToken: string; refreshToken: string }) => {
    user.value = result.user
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    localStorage.setItem(ACCESS_KEY, result.accessToken)
    localStorage.setItem(REFRESH_KEY, result.refreshToken)
  }
  async function login(body: { username: string; password: string; captchaId: string; captchaCode: string }, expectedRole: Role) {
    const result = await api.login(body)
    if (result.user.role !== expectedRole) throw new Error(expectedRole === 'ADMIN' ? '该账号不是管理员账号' : '管理员请从管理端入口登录')
    save(result)
  }
  async function register(body: { username: string; password: string; phone: string; captchaId: string; captchaCode: string }) { await api.register(body) }
  async function logout() {
    const token = localStorage.getItem(REFRESH_KEY)
    try { if (token) await api.logout(token) } finally { user.value = null; clearStoredSession() }
  }
  return { user, isLoggedIn, login, register, logout }
})
