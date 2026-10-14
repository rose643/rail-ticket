<script setup lang="ts">
import { ref, watch } from 'vue'
import { MapPin } from 'lucide-vue-next'
import { api } from '@/services/api'
import type { StationSuggestion } from '@/types'

const props = defineProps<{ modelValue: StationSuggestion | null; placeholder: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: StationSuggestion | null] }>()
const keyword = ref(props.modelValue?.label || '')
const options = ref<StationSuggestion[]>([])
const open = ref(false)
let timer = 0
watch(() => props.modelValue, (value) => { if (value) keyword.value = value.label })
watch(keyword, (value) => {
  window.clearTimeout(timer)
  if (value.trim().length < 1) { options.value = []; return }
  if (props.modelValue?.label !== value) emit('update:modelValue', null)
  timer = window.setTimeout(async () => { options.value = await api.suggestions(value.trim()); open.value = true }, 250)
})
function select(option: StationSuggestion) { keyword.value = option.label; emit('update:modelValue', option); open.value = false }
function closeLater() { window.setTimeout(() => { open.value = false }, 150) }
</script>
<template><div class="location-picker"><MapPin :size="19"/><input v-model="keyword" :placeholder="placeholder" @focus="open=true" @blur="closeLater"><div v-if="open && options.length" class="suggestions"><button v-for="item in options" :key="item.type+item.value" @mousedown.prevent="select(item)"><span>{{ item.label }}</span><small>{{ item.type === 'CITY' ? '城市 · 全部车站' : item.city }}</small></button></div></div></template>
