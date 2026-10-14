<script setup lang="ts">
import { Gauge, LogOut, MapPinned, TrainFront, UserRound } from 'lucide-vue-next'
import AppLogo from '@/components/AppLogo.vue'; import { useAuthStore } from '@/stores/auth'; import { useRouter } from 'vue-router'
const auth=useAuthStore(); const router=useRouter(); const nav=[{to:'/admin',label:'数据概览',icon:Gauge},{to:'/admin/stations',label:'车站管理',icon:MapPinned},{to:'/admin/trains',label:'车次管理',icon:TrainFront}]
async function leave(){await auth.logout(); router.push('/')}
</script>
<template><div class="admin-shell"><aside class="sidebar"><AppLogo/><p class="sidebar-caption">运营管理台</p><nav><RouterLink v-for="item in nav" :key="item.to" :to="item.to"><component :is="item.icon"/>{{item.label}}</RouterLink></nav><div class="sidebar-user"><UserRound/><div><strong>{{auth.user?.username}}</strong><small>系统管理员</small></div><button class="icon-btn" @click="leave"><LogOut/></button></div></aside><div class="admin-main"><header class="admin-mobile-head"><AppLogo/><button class="icon-btn" @click="leave"><LogOut/></button></header><main><RouterView/></main><nav class="admin-mobile-nav"><RouterLink v-for="item in nav" :key="item.to" :to="item.to"><component :is="item.icon"/><span>{{item.label}}</span></RouterLink></nav></div></div></template>
