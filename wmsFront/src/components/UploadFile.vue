<template>
  <Dialog
    :visible="visible"
    header="Import File"
    modal
    class="w-full max-w-md"
    @update:visible="emit('update:visible', $event)"
    @hide="reset"
  >
    <div class="flex flex-col gap-4">
      <FileUpload
        mode="basic"
        :auto="false"
        :multiple="false"
        :customUpload="true"
        accept=".xlsx,.xls,.csv"
        chooseLabel="Select file"
        @select="onSelect"
      />

      <div v-if="file" class="text-sm text-gray-500">
        Selected: {{ file.name }} ({{ formatSize(file.size) }})
      </div>

      <Message v-if="error" severity="error">{{ error }}</Message>
    </div>

    <template #footer>
      <Button label="Cancel" severity="secondary" text @click="close" />
      <Button
        label="Import"
        icon="pi pi-upload"
        severity="success"
        :loading="loading"
        :disabled="!file"
        @click="upload"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'

import Dialog from 'primevue/dialog'
import FileUpload from 'primevue/fileupload'
import Button from 'primevue/button'
import Message from 'primevue/message'

const props = defineProps({
  visible: Boolean,
  apiCall: {
    type: Function,
    required: true,
  },
})

const emit = defineEmits(['update:visible', 'success'])

const file = ref(null)
const loading = ref(false)
const error = ref('')

const onSelect = (event) => {
  file.value = event.files?.[0] ?? null
  error.value = ''
}

const upload = async () => {
  if (!file.value) return

  loading.value = true
  error.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file.value)

    await props.apiCall(formData)

    emit('success')
    close()
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || 'Import failed'
  } finally {
    loading.value = false
  }
}

const reset = () => {
  file.value = null
  error.value = ''
  loading.value = false
}

const close = () => {
  emit('update:visible', false)
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}
</script>
