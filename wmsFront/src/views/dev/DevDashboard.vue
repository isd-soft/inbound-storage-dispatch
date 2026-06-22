<template>
  <div class="app-shell font-sans">

    <main class="p-4 max-w-7xl mx-auto mt-4">

    </main>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import 'primeicons/primeicons.css'

const router = useRouter()
const authStore = useAuthStore()

</script>
