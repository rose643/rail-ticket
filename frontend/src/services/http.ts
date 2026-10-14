import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResult, LoginResponse } from '@/types'

const ACCESS_KEY = 'rail_access_token'
const REFRESH_KEY = 'rail_refresh_token'
const USER_KEY = 'rail_user'

export const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshing: Promise<string> | null = null
http.interceptors.response.use((response) => response, async (error: AxiosError<ApiResult<unknown>>) => {
  const request = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (error.response?.status !== 401 || !request || request._retried || !refreshToken || request.url?.includes('/auth/refresh')) return Promise.reject(error)
  request._retried = true
  refreshing ??= axios.post<ApiResult<LoginResponse>>(`${http.defaults.baseURL}/auth/refresh`, { refreshToken })
    .then(({ data }) => {
      localStorage.setItem(ACCESS_KEY, data.data.accessToken)
      localStorage.setItem(REFRESH_KEY, data.data.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(data.data.user))
      return data.data.accessToken
    })
    .catch((refreshError) => {
      clearStoredSession()
      window.location.assign('/')
      throw refreshError
    })
    .finally(() => { refreshing = null })
  request.headers.Authorization = `Bearer ${await refreshing}`
  return http(request)
})

export function clearStoredSession() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export function errorMessage(error: unknown, fallback = '操作失败，请稍后重试') {
  if (axios.isAxiosError<ApiResult<unknown>>(error)) return error.response?.data?.message || fallback
  return error instanceof Error ? error.message : fallback
}
