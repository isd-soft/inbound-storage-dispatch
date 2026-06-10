<template>
  <div class="card p-4">
    <Toast position="top-right" />

    <Form @submit="onSubmit" class="flex flex-col gap-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="field">
          <label for="logicId" class="block mb-1 font-medium">Logic ID</label>
          <InputText
            id="logicId"
            v-model="formData.logicId"
            fluid
            :class="{ 'p-invalid': submitted && !formData.logicId }"
          />
          <small v-if="submitted && !formData.logicId" class="p-error">Logic ID is required.</small>
        </div>

        <div class="field">
          <label for="location" class="block mb-1 font-medium">Location</label>
          <Dropdown
            id="location"
            v-model="formData.location"
            :options="locations"
            optionLabel="name"
            optionValue="code"
            placeholder="Select a location"
            fluid
            :class="{ 'p-invalid': submitted && !formData.location }"
          />
          <small v-if="submitted && !formData.location" class="p-error"
            >Location is required.</small
          >
        </div>
      </div>

      <!-- Dynamic lines section -->
      <div>
        <h3 class="mb-2">Order Lines</h3>
        <div
          v-for="(line, idx) in formData.lines"
          :key="line.id"
          class="line-item mb-3 p-3 border-round surface-border"
        >
          <div class="flex flex-wrap gap-3 align-items-end">
            <div class="flex-1 min-w-12rem">
              <label :for="`product-${line.id}`" class="block mb-1">Product</label>
              <Dropdown
                :id="`product-${line.id}`"
                v-model="line.product"
                :options="products"
                optionLabel="name"
                optionValue="id"
                placeholder="Choose product"
                fluid
                :class="{ 'p-invalid': submitted && !line.product }"
              />
              <small v-if="submitted && !line.product" class="p-error">Required</small>
            </div>
            <div class="w-10rem">
              <label :for="`qty-${line.id}`" class="block mb-1">Quantity</label>
              <InputNumber
                :id="`qty-${line.id}`"
                v-model="line.quantity"
                :min="1"
                showButtons
                buttonLayout="horizontal"
                fluid
                :class="{ 'p-invalid': submitted && (!line.quantity || line.quantity < 1) }"
              />
              <small v-if="submitted && (!line.quantity || line.quantity < 1)" class="p-error"
                >Min 1</small
              >
            </div>
            <Button icon="pi pi-trash" severity="danger" text rounded @click="removeLine(idx)" />
          </div>
        </div>

        <Button label="+ Add line" icon="pi pi-plus" outlined @click="addLine" class="mt-2" />
      </div>

      <div class="flex justify-end">
        <Button type="submit" label="Submit Order" severity="success" icon="pi pi-check" />
      </div>
    </Form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { Form } from '@primevue/forms'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'

const toast = useToast()

const formData = reactive({
  logicId: '',
  location: null,
  lines: [],
})

const locations = ref([
  { name: 'Warehouse A', code: 'WH_A' },
  { name: 'Warehouse B', code: 'WH_B' },
  { name: 'Store C', code: 'ST_C' },
])

const products = ref([
  { id: 1, name: 'Laptop' },
  { id: 2, name: 'Mouse' },
  { id: 3, name: 'Keyboard' },
  { id: 4, name: 'Monitor' },
])

let nextLineId = 1
const addLine = () => {
  formData.lines.push({
    id: nextLineId++,
    product: null,
    quantity: 1,
  })
}

const removeLine = (index) => {
  formData.lines.splice(index, 1)
}

const submitted = ref(false)

const onSubmit = (event) => {
  submitted.value = true

  const isTopValid = !!formData.logicId && !!formData.location

  const areLinesValid =
    formData.lines.length > 0 &&
    formData.lines.every((line) => line.product && line.quantity && line.quantity >= 1)

  if (!isTopValid || !areLinesValid) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fill all fields and at least one valid line.',
      life: 4000,
    })
    return
  }

  // Prepare payload
  const payload = {
    logicId: formData.logicId,
    location: formData.location,
    lines: formData.lines.map((line) => ({
      productId: line.product,
      productName: products.value.find((p) => p.id === line.product)?.name,
      quantity: line.quantity,
    })),
  }

  console.log('Submitting:', payload)

  toast.add({
    severity: 'success',
    summary: 'Order Submitted',
    detail: `${formData.lines.length} line(s) added.`,
    life: 5000,
  })

}

addLine()
</script>

<style scoped>
.line-item {
  background-color: var(--surface-card);
  border: 1px solid var(--surface-border);
}
.min-w-12rem {
  min-width: 12rem;
}
.w-10rem {
  width: 10rem;
}
</style>
