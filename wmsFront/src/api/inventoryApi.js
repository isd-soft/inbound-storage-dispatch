import apiClient from './index'

export const inventoryApi = {
  getAllStock() {
    return apiClient.get('/inventory')
  },
  getStockById(stockId) {
    return apiClient.get(`/inventory/${stockId}`)
  },
  addStock(payload) {
    return apiClient.post('/inventory/add', payload)
  },
  removeStock(payload) {
    return apiClient.post('/inventory/remove', payload)
  },
  adjustStock(payload) {
    return apiClient.put('/inventory/adjust', payload)
  },
  getAllHistory() {
    return apiClient.get('/inventory/history')
  },
  getStockHistory(stockId) {
    return apiClient.get(`/inventory/${stockId}/history`)
  },
  getProducts() {
    return apiClient.get('/products')
  },
  getLocations() {
    return apiClient.get('/locations')
  }
}
