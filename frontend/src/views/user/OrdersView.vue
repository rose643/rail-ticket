<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowRight, Clock3 } from 'lucide-vue-next'
import EmptyState from '@/components/EmptyState.vue'
import { api } from '@/services/api'
import { errorMessage } from '@/services/http'
import { formatSeatType } from '@/utils/seatType'
import { formatClock, formatDate, formatDuration } from '@/utils/time'
import type { TicketOrder } from '@/types'

const orders = ref<TicketOrder[]>([])
const message = ref('')
const labels = { PENDING_PAYMENT: '待支付', PAID: '已支付', CLOSED: '已关闭', REFUNDED: '已退款' }

async function load() {
  try { orders.value = await api.orders() } catch (error) { message.value = errorMessage(error) }
}

async function action(type: 'pay' | 'cancel' | 'refund', id: string) {
  try {
    if (type === 'pay') await api.payOrder(id)
    else if (type === 'cancel') await api.cancelOrder(id)
    else await api.refundOrder(id)
    await load()
  } catch (error) { message.value = errorMessage(error) }
}

onMounted(load)
</script>

<template>
  <section class="page container">
    <header class="page-head"><div><p class="eyebrow">MY ORDERS</p><h1>我的订单</h1><p>查看购票人创建的全部订单</p></div></header>
    <p v-if="message" class="notice error">{{ message }}</p>
    <div v-if="orders.length" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card journey-order">
        <header><span>{{ formatDate(order.travelDate) }} · 订单号 {{ order.orderNo }}</span><b :class="`order-${order.status}`">{{ labels[order.status] }}</b></header>
        <div class="journey-heading"><strong>{{ order.trainNo }}</strong><span>{{ order.trainName }}</span></div>
        <div class="journey-route">
          <div><strong>{{ formatClock(order.departureTime) }}</strong><span>{{ order.fromStationName }}</span></div>
          <div class="journey-duration"><small><Clock3 />{{ formatDuration(order.durationMinutes) }}</small><i><ArrowRight /></i></div>
          <div class="arrival"><strong>{{ formatClock(order.arrivalTime) }}</strong><span>{{ order.toStationName }}</span></div>
        </div>
        <div class="order-meta"><p><span>乘车人</span><strong>{{ order.passengerName }}</strong></p><p><span>席别</span><strong>{{ formatSeatType(order.seatTypeCode) }}</strong></p><p><span>订单金额</span><strong class="amount">¥{{ order.amount }}</strong></p></div>
        <footer><span v-if="order.status === 'PENDING_PAYMENT'">请于 {{ order.expireAt.replace('T', ' ').slice(0, 16) }} 前支付</span><span v-else>创建于 {{ order.createdAt.replace('T', ' ').slice(0, 16) }}</span><div><button v-if="order.status === 'PENDING_PAYMENT'" class="btn ghost" @click="action('cancel', order.id)">取消订单</button><button v-if="order.status === 'PENDING_PAYMENT'" class="btn primary" @click="action('pay', order.id)">立即支付</button><button v-if="order.status === 'PAID'" class="btn ghost" @click="action('refund', order.id)">申请退票</button></div></footer>
      </article>
    </div>
    <EmptyState v-else title="暂无订单" hint="查询车次后即可购买车票" />
  </section>
</template>
