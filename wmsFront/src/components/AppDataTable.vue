<template>
  <DataTable
    :value="value"
    :loading="loading"
    :dataKey="dataKey"
    :emptyMessage="emptyMessage"
    :rows="rows"
    :rowsPerPageOptions="rowsPerPageOptions"
    paginator
    stripedRows
    showGridlines
    responsiveLayout="scroll"
    class="app-data-table p-datatable-sm"
    paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
    currentPageReportTemplate="{first}-{last} of {totalRecords}"
    v-bind="$attrs"
  >
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps || {}" />
    </template>
  </DataTable>
</template>

<script setup>
import DataTable from 'primevue/datatable'

defineOptions({ inheritAttrs: false })

defineProps({
  value: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  dataKey: { type: String, default: 'id' },
  emptyMessage: { type: String, default: 'No records found.' },
  rows: { type: Number, default: 10 },
  rowsPerPageOptions: { type: Array, default: () => [10, 20, 50] }
})
</script>
