<script setup lang="ts">
defineProps<{ active?: boolean; text?: string }>()
</script>
<template><span class="status" :class="active ? 'status-success' : 'status-muted'"><i />{{ text || (active ? '启用' : '停用') }}</span></template>
