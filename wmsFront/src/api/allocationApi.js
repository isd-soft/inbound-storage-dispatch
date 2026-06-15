import apiClient from './index'

export const allocationApi = {
  getCurrentTaskSummary() {
    return apiClient.get('/v1/processes/operator/current/summary')
  },
  getSupervisorProcesses() {
    return apiClient.get('/v1/processes')
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
  },
}
