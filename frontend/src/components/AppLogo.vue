<script setup lang="ts">
import { TrainFront } from 'lucide-vue-next'
defineProps<{ compact?: boolean }>()
</script>
<template><RouterLink to="/" class="brand"><span class="brand-icon"><TrainFront :size="22" /></span><span v-if="!compact">铁路通</span></RouterLink></template>
