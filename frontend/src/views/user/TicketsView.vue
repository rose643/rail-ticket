<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowRight, Clock3, TrainFront } from 'lucide-vue-next'
import EmptyState from '@/components/EmptyState.vue'
import { api } from '@/services/api'
import { formatSeatType } from '@/utils/seatType'
import { formatClock, formatDate, formatDuration } from '@/utils/time'
import type { TicketOrder } from '@/types'

const items = ref<TicketOrder[]>([])
const statusLabels: Record<string, string> = { PENDING_PAYMENT: '待支付', PAID: '已出票', CLOSED: '已关闭', REFUNDED: '已退票' }
onMounted(async () => { items.value = await api.tickets() })
</script>

<template>
  <section class="page container">
    <header class="page-head"><div><p class="eyebrow">MY TICKETS</p><h1>我的车票</h1><p>以本人实名信息查询作为乘车人的车票</p></div></header>
    <div v-if="items.length" class="ticket-list">
      <article v-for="ticket in items" :key="ticket.id" class="rail-ticket-card">
        <header><div><TrainFront /><strong>{{ ticket.trainNo }}</strong><span>{{ ticket.trainName }}</span></div><b>{{ statusLabels[ticket.status] || ticket.status }}</b></header>
        <div class="ticket-date">{{ formatDate(ticket.travelDate) }}</div>
        <div class="journey-route ticket-journey">
          <div><strong>{{ formatClock(ticket.departureTime) }}</strong><span>{{ ticket.fromStationName }}</span><small>{{ ticket.fromStationCode }}</small></div>
          <div class="journey-duration"><small><Clock3 />{{ formatDuration(ticket.durationMinutes) }}</small><i><ArrowRight /></i></div>
          <div class="arrival"><strong>{{ formatClock(ticket.arrivalTime) }}</strong><span>{{ ticket.toStationName }}</span><small>{{ ticket.toStationCode }}</small></div>
        </div>
        <div class="ticket-details"><p><span>乘车人</span><strong>{{ ticket.passengerName }}</strong></p><p><span>席别</span><strong>{{ formatSeatType(ticket.seatTypeCode) }}</strong></p><p><span>票价</span><strong>¥{{ ticket.amount }}</strong></p><p><span>订单号</span><strong>{{ ticket.orderNo }}</strong></p></div>
      </article>
    </div>
    <EmptyState v-else title="暂无本人车票" hint="完成实名认证后，这里会展示与你证件匹配的车票" />
  </section>
</template>
