import apiClient from './index'

export const processApi = {
  getAvailable: () => apiClient.get('/processes/available'),
  getMyProcesses: () => apiClient.get('/processes/my'),
  getAssigned: () => apiClient.get('/processes/assigned'),
  assign: (id) => apiClient.patch(`/processes/${id}/assign`),
  start: (id) => apiClient.post(`/processes/${id}/start`),
  scanLocation: (id, barcode) => apiClient.post(`/processes/${id}/scan-location`, { barcode }),
  scanProduct: (id, barcode) => apiClient.post(`/processes/${id}/scan-product`, { barcode }),
  confirmQty: (id, pickedQuantity) =>
    apiClient.post(`/processes/${id}/confirm-quantity`, { pickedQuantity }),
  complete: (id) => apiClient.post(`/processes/${id}/complete`),
  completeProcess: (id) => apiClient.patch(`/processes/${id}/complete`),
}
