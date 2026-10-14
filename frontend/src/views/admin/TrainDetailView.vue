<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, CalendarDays, MapPin, Plus, Tags, Warehouse } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import ModalDialog from '@/components/ModalDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { api } from '@/services/api'
import { errorMessage } from '@/services/http'
import { formatSeatType, SEAT_TYPE_OPTIONS } from '@/utils/seatType'
import { formatOffsetTime } from '@/utils/time'
import type { SeatInventory, Station, TrainFare, TrainSchedule, TrainStop } from '@/types'

const route = useRoute()
const trainId = computed(() => String(route.params.id))
const tab = ref<'stops' | 'schedules' | 'fares' | 'inventory'>('stops')
const stops = ref<TrainStop[]>([])
const schedules = ref<TrainSchedule[]>([])
const fares = ref<TrainFare[]>([])
const inventories = ref<SeatInventory[]>([])
const stations = ref<Station[]>([])
const scheduleId = ref('')
const message = ref('')
const modal = ref('')
const stopForm = ref({ stationId: '', stopIndex: 0, arrivalOffsetMinutes: null as number | null, departureOffsetMinutes: null as number | null })
const stopTimeForm = ref({ arrivalDay: 0, arrivalClock: '', departureDay: 0, departureClock: '' })
const scheduleDate = ref(new Date().toISOString().slice(0, 10))
const fareForm = ref({ seatTypeCode: 'SECOND_CLASS', fromStopIndex: 0, toStopIndex: 1, price: 0 })
const inventoryForm = ref({ seatTypeCode: 'SECOND_CLASS', total: 100 })

function stationName(stationId: string) {
  return stations.value.find((station) => station.id === stationId)?.name || stationId
}

function stationNameByStopIndex(stopIndex: number) {
  const stop = stops.value.find((item) => item.stopIndex === stopIndex)
  return stop ? stationName(stop.stationId) : `第 ${stopIndex} 站`
}

function formatSection(fromStopIndex: number, toStopIndex: number) {
  return `${stationNameByStopIndex(fromStopIndex)} → ${stationNameByStopIndex(toStopIndex)}`
}

function toOffsetMinutes(dayOffset: number, clock: string) {
  if (!clock) return null
  const [hour, minute] = clock.split(':').map(Number)
  return dayOffset * 1440 + hour * 60 + minute
}

async function load() {
  try {
    [stops.value, schedules.value, fares.value, stations.value] = await Promise.all([
      api.stops(trainId.value), api.schedules(trainId.value), api.fares(trainId.value), api.adminStations(),
    ])
    if (!scheduleId.value && schedules.value.length) scheduleId.value = schedules.value[0].id
    if (scheduleId.value) inventories.value = await api.inventories(trainId.value, scheduleId.value)
  } catch (error) {
    message.value = errorMessage(error)
  }
}

async function add() {
  try {
    if (modal.value === 'stop') {
      stopForm.value.arrivalOffsetMinutes = toOffsetMinutes(stopTimeForm.value.arrivalDay, stopTimeForm.value.arrivalClock)
      stopForm.value.departureOffsetMinutes = toOffsetMinutes(stopTimeForm.value.departureDay, stopTimeForm.value.departureClock)
      await api.addStop(trainId.value, stopForm.value)
    }
    if (modal.value === 'schedule') await api.addSchedule(trainId.value, scheduleDate.value)
    if (modal.value === 'fare') await api.addFare(trainId.value, fareForm.value)
    if (modal.value === 'inventory' && scheduleId.value) await api.initializeInventory(trainId.value, scheduleId.value, inventoryForm.value)
    modal.value = ''
    await load()
  } catch (error) {
    message.value = errorMessage(error)
  }
}

async function chooseSchedule() {
  inventories.value = scheduleId.value ? await api.inventories(trainId.value, scheduleId.value) : []
}

onMounted(load)
</script>

<template>
  <section class="admin-page">
    <RouterLink to="/admin/trains" class="back"><ArrowLeft />返回车次列表</RouterLink>
    <header class="page-head compact">
      <div><p class="eyebrow">TRAIN DETAIL</p><h1>车次票务配置</h1><p>车次 ID：{{ trainId }}</p></div>
    </header>
    <p v-if="message" class="notice error">{{ message }}</p>

    <nav class="tabs">
      <button :class="{ active: tab === 'stops' }" @click="tab = 'stops'"><MapPin />经停路线</button>
      <button :class="{ active: tab === 'schedules' }" @click="tab = 'schedules'"><CalendarDays />运行计划</button>
      <button :class="{ active: tab === 'fares' }" @click="tab = 'fares'"><Tags />区间票价</button>
      <button :class="{ active: tab === 'inventory' }" @click="tab = 'inventory'"><Warehouse />席位库存</button>
    </nav>

    <div class="tab-panel">
      <div class="panel-head">
        <h2>{{ tab === 'stops' ? '经停路线' : tab === 'schedules' ? '运行计划' : tab === 'fares' ? '区间票价' : '席位库存' }}</h2>
        <div>
          <select v-if="tab === 'inventory'" v-model="scheduleId" @change="chooseSchedule">
            <option value="">选择运行计划</option>
            <option v-for="schedule in schedules" :key="schedule.id" :value="schedule.id">{{ schedule.travelDate }}</option>
          </select>
          <button class="btn primary small" :disabled="tab === 'inventory' && !scheduleId" @click="modal = tab === 'inventory' ? 'inventory' : tab.slice(0, -1)"><Plus />新增</button>
        </div>
      </div>

      <div v-if="tab === 'stops' && stops.length" class="route-list">
        <article v-for="stop in stops" :key="stop.id">
          <span>{{ stop.stopIndex }}</span>
          <div>
            <strong>{{ stationName(stop.stationId) }}</strong>
            <p>到达 {{ formatOffsetTime(stop.arrivalOffsetMinutes) }} · 发车 {{ formatOffsetTime(stop.departureOffsetMinutes) }}</p>
          </div>
        </article>
      </div>

      <div v-else-if="tab === 'schedules' && schedules.length" class="data-table-wrap">
        <table class="data-table"><thead><tr><th>运行日期</th><th>状态</th><th>计划 ID</th></tr></thead><tbody>
          <tr v-for="schedule in schedules" :key="schedule.id"><td><strong>{{ schedule.travelDate }}</strong></td><td><StatusBadge :active="schedule.status === 1" /></td><td><code>{{ schedule.id }}</code></td></tr>
        </tbody></table>
      </div>

      <div v-else-if="tab === 'fares' && fares.length" class="data-table-wrap">
        <table class="data-table"><thead><tr><th>席别</th><th>乘车区间</th><th>票价</th></tr></thead><tbody>
          <tr v-for="fare in fares" :key="fare.id"><td>{{ formatSeatType(fare.seatTypeCode) }}</td><td>{{ formatSection(fare.fromStopIndex, fare.toStopIndex) }}</td><td><strong>¥{{ fare.price }}</strong></td></tr>
        </tbody></table>
      </div>

      <div v-else-if="tab === 'inventory' && inventories.length" class="data-table-wrap">
        <table class="data-table"><thead><tr><th>席别</th><th>运行区段</th><th>总库存</th><th>可用</th></tr></thead><tbody>
          <tr v-for="inventory in inventories" :key="inventory.id"><td>{{ formatSeatType(inventory.seatTypeCode) }}</td><td>{{ formatSection(inventory.segmentIndex, inventory.segmentIndex + 1) }}</td><td>{{ inventory.total }}</td><td><strong>{{ inventory.available }}</strong></td></tr>
        </tbody></table>
      </div>
      <EmptyState v-else :hint="tab === 'inventory' && !scheduleId ? '请先选择运行计划' : '点击右上角新增数据'" />
    </div>

    <ModalDialog :open="!!modal" :title="`新增${tab === 'stops' ? '经停站' : tab === 'schedules' ? '运行计划' : tab === 'fares' ? '票价' : '库存'}`" @close="modal = ''">
      <form class="stack-form" @submit.prevent="add">
        <template v-if="modal === 'stop'">
          <label>车站<select v-model="stopForm.stationId" required><option value="">请选择</option><option v-for="station in stations.filter((item) => item.status === 1)" :key="station.id" :value="station.id">{{ station.city }} · {{ station.name }}</option></select></label>
          <label>经停顺序<input v-model.number="stopForm.stopIndex" type="number" min="0" required></label>
          <div class="form-grid">
            <label>到达日期<select v-model.number="stopTimeForm.arrivalDay"><option :value="0">当日</option><option :value="1">次日</option><option :value="2">第 3 天</option></select></label>
            <label>到达时刻<input v-model="stopTimeForm.arrivalClock" type="time"></label>
            <label>发车日期<select v-model.number="stopTimeForm.departureDay"><option :value="0">当日</option><option :value="1">次日</option><option :value="2">第 3 天</option></select></label>
            <label>发车时刻<input v-model="stopTimeForm.departureClock" type="time"></label>
          </div>
          <small class="muted">始发站可不填到达时刻，终点站可不填发车时刻。</small>
        </template>
        <label v-else-if="modal === 'schedule'">运行日期<input v-model="scheduleDate" type="date" required></label>
        <template v-else-if="modal === 'fare'">
          <label>席别<select v-model="fareForm.seatTypeCode"><option v-for="option in SEAT_TYPE_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
          <div class="form-grid">
            <label>出发站<select v-model.number="fareForm.fromStopIndex"><option v-for="stop in stops" :key="stop.id" :value="stop.stopIndex">{{ stationName(stop.stationId) }}</option></select></label>
            <label>到达站<select v-model.number="fareForm.toStopIndex"><option v-for="stop in stops" :key="stop.id" :value="stop.stopIndex">{{ stationName(stop.stationId) }}</option></select></label>
          </div>
          <label>票价<input v-model.number="fareForm.price" type="number" min="0" step="0.01"></label>
        </template>
        <template v-else>
          <label>席别<select v-model="inventoryForm.seatTypeCode"><option v-for="option in SEAT_TYPE_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
          <label>初始总库存<input v-model.number="inventoryForm.total" type="number" min="0"></label>
        </template>
        <button class="btn primary wide">保存</button>
      </form>
    </ModalDialog>
  </section>
</template>
