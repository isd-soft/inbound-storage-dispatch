import apiClient from './index'

export const processApi = {
  getCurrentTaskSummary() {
    return apiClient.get('/v1/processes/operator/current/summary')
  },
  startCurrentTask() {
    return apiClient.post('/v1/processes/operator/current/start')
  },
  completeAssignedProcess(processId) {
    return apiClient.post(`/v1/processes/${processId}/complete`)
  },
  scanSourceLocation(processId, barcode) {
    return apiClient.post(`/v1/processes/${processId}/location`, { barcode })
  },
  scanProduct(processId, barcode) {
    return apiClient.post(`/v1/processes/${processId}/product`, { barcode })
  },
  confirmPickedQuantity(processId, pickedQuantity) {
    return apiClient.post(`/v1/processes/${processId}/confirm-quantity`, { pickedQuantity })
  }
}
