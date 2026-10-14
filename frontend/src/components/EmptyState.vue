<script setup lang="ts">import { SearchX } from 'lucide-vue-next'; defineProps<{ title?: string; hint?: string }>()</script>
<template><div class="empty"><SearchX :size="34"/><strong>{{ title || '暂无数据' }}</strong><span>{{ hint || '换个条件再试试吧' }}</span></div></template>
