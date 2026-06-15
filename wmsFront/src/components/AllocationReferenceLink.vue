<template>
  <RouterLink v-if="target" :to="target" class="app-product-link" :title="title">
    {{ label }}
  </RouterLink>

  <span v-else class="app-product-link app-muted" :title="title">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  type: { type: String, default: '' },
  orderId: { type: [Number, String], default: null },
  replenishmentId: { type: [Number, String], default: null },
})

const label = computed(() => {
  if (props.type === 'PICKING_ORDER' && props.orderId) {
    return `Order #${props.orderId}`
  }

  if (props.type === 'REPLENISHMENT' && props.replenishmentId) {
    return `Replenishment #${props.replenishmentId}`
  }

  return 'Reference'
})

const title = computed(() => `Open ${label.value}`)

const target = computed(() => {
  if (props.type === 'PICKING_ORDER' && props.orderId) {
    return {
      name: 'orders',
      query: { orderId: props.orderId },
    }
  }

  if (props.type === 'REPLENISHMENT' && props.replenishmentId) {
    return {
      name: 'replenishments',
      query: { replenishmentId: props.replenishmentId },
    }
  }

  return null
})
</script>
