<script setup lang="ts">
import { Bell, Contact, LogOut, Search, TicketCheck, UserRound, WalletCards } from 'lucide-vue-next'
import AppLogo from '@/components/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
const auth = useAuthStore(); const router = useRouter()
const nav = [{to:'/user/search',label:'查票',icon:Search},{to:'/user/orders',label:'订单',icon:WalletCards},{to:'/user/tickets',label:'车票',icon:TicketCheck},{to:'/user/passengers',label:'乘车人',icon:Contact},{to:'/user/notifications',label:'消息',icon:Bell}]
async function leave(){await auth.logout(); router.push('/')}
</script>
<template><div class="app-shell"><header class="topbar"><AppLogo/><nav class="desktop-nav"><RouterLink v-for="item in nav" :key="item.to" :to="item.to"><component :is="item.icon" :size="17"/>{{item.label}}</RouterLink></nav><div class="user-menu"><RouterLink to="/user/profile"><UserRound :size="18"/>{{auth.user?.username}}</RouterLink><button class="icon-btn" title="退出登录" @click="leave"><LogOut :size="19"/></button></div></header><main class="main-content"><RouterView/></main><nav class="mobile-nav"><RouterLink v-for="item in nav.slice(0,5)" :key="item.to" :to="item.to"><component :is="item.icon"/><span>{{item.label}}</span></RouterLink></nav></div></template>
