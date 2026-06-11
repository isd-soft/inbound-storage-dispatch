import apiClient from './index'

export const processApi = {
  getAvailable() {
    return apiClient.get('/processes/available')
  },
  getMy() {
    return apiClient.get('/processes/my')
  },
  getAssignedExecutions() {
    return apiClient.get('/processes/assigned')
  },
  assign(processId) {
    return apiClient.patch(`/processes/${processId}/assign`)
  },
  start(processId) {
    return apiClient.post(`/processes/${processId}/start`)
  },
  scanSourceLocation(processId, barcode) {
    return apiClient.post(`/processes/${processId}/location`, { barcode })
  },
  scanProduct(processId, barcode) {
    return apiClient.post(`/processes/${processId}/product`, { barcode })
  },
  confirmPickedQuantity(processId, pickedQuantity) {
    return apiClient.post(`/processes/${processId}/confirm-quantity`, { pickedQuantity })
  },
  complete(processId) {
    return apiClient.post(`/processes/${processId}/complete`)
  }
}
