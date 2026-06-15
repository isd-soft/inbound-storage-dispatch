<template>
  <div class="fixed bottom-6 right-6 z-50">

    <transition name="fade-slide">
      <div
        v-if="isOpen"
        class="absolute bottom-0 right-0 origin-bottom-right app-card shadow-2xl rounded-2xl w-96 h-[500px] flex flex-col border border-white/10 overflow-hidden"
      >
        <div class="p-4 border-b border-gray-700 flex justify-between items-center bg-gray-800">
          <h2 class="text-lg font-bold text-white flex items-center gap-2 m-0">
            <i class="pi pi-sparkles"></i> AI Assistant
          </h2>
          <Button icon="pi pi-times" text rounded severity="secondary" @click="isOpen = false" />
        </div>

        <div class="flex-1 overflow-y-auto p-4 flex flex-col gap-3 scroll-smooth" ref="chatBox">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            :class="['p-3 rounded-xl max-w-[85%]', msg.role === 'user' ? 'bg-blue-600 text-white self-end' : 'bg-gray-700 text-gray-200 self-start']"
          >
            {{ msg.content }}
          </div>
          <div v-if="loading" class="text-gray-400 self-start p-3 text-sm italic">
            <i class="pi pi-spin pi-spinner"></i> Thinking...
          </div>
        </div>

        <div class="p-3 border-t border-gray-700 flex gap-2">
          <InputText v-model="inputMsg" class="flex-1" @keyup.enter="sendMessage" placeholder="Ask about stock..." />
          <Button icon="pi pi-send" severity="primary" @click="sendMessage" :loading="loading" />
        </div>
      </div>
    </transition>

    <transition name="fade-btn">
      <Button
        v-if="!isOpen"
        icon="pi pi-sparkles"
        class="absolute bottom-0 right-0 w-14 h-14 rounded-full shadow-xl hover:scale-105 transition-all duration-300"
        size="large"
        severity="primary"
        @click="isOpen = true"
      />
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import { chatApi } from '@/api/chatApi';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';

const isOpen = ref(false);
const messages = ref([{ role: 'ai', content: 'Hello! I am your WMS AI Assistant.' }]);
const inputMsg = ref('');
const loading = ref(false);
const chatBox = ref(null);

const scrollToBottom = async () => {
  await nextTick();
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight;
  }
};

const sendMessage = async () => {
  if (!inputMsg.value.trim() || loading.value) return;

  const userText = inputMsg.value;
  messages.value.push({ role: 'user', content: userText });
  inputMsg.value = '';
  loading.value = true;
  await scrollToBottom();

  try {
    const res = await chatApi.sendMessage(userText);
    messages.value.push({ role: 'ai', content: res.data.reply });
  } catch (e) {
    messages.value.push({ role: 'ai', content: 'Error: Connection failed.' });
  } finally {
    loading.value = false;
    await scrollToBottom();
  }
};
</script>

<style scoped>
/* Анимация для окна чата */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 0.4s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform, opacity; /* Защита от фризов при рендере */
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: scale(0.85);
}

.fade-slide-enter-to,
.fade-slide-leave-from {
  opacity: 1;
  transform: scale(1);
}

.fade-btn-enter-active,
.fade-btn-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
  transition-delay: 0.1s;
}

.fade-btn-enter-from,
.fade-btn-leave-to {
  opacity: 0;
  transform: scale(0.5);
}
</style>
