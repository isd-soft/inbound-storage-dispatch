import apiClient from './index'

export const allocationApi = {
  getCurrentTaskSummary() {
    return apiClient.get('/v1/allocations/operator/current/summary')
  },
  getSupervisorAllocations() {
    return apiClient.get('/v1/allocations')
  },
  startCurrentTask() {
    return apiClient.post('/v1/allocations/operator/current/start')
  },
  completeAssignedAllocation(allocationId) {
    return apiClient.post(`/v1/allocations/${allocationId}/complete`)
  },
  scanSourceLocation(allocationId, barcode) {
    return apiClient.post(`/v1/allocations/${allocationId}/location`, { barcode })
  },
  scanProduct(allocationId, barcode) {
    return apiClient.post(`/v1/allocations/${allocationId}/product`, { barcode })
  },
  confirmPickedQuantity(allocationId, pickedQuantity) {
    return apiClient.post(`/v1/allocations/${allocationId}/confirm-quantity`, { pickedQuantity })
  }
}
