import apiClient from './index'

export const processApi = {
  getAvailable() {
    return apiClient.get('/v1/processes/operators')
  },
  getMy() {
    return apiClient.get('/v1/processes/operators')
  },
  getAssignedExecutions() {
    return apiClient.get('/v1/processes/operators')
  },
  assign(processId) {
    return apiClient.post('/v1/processes/start')
  },
  start() {
    return apiClient.post('/v1/processes/start')
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
  complete(processId) {
    return apiClient.post(`/v1/processes/${processId}/complete`)
  }
}
