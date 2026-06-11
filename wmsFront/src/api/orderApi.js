import apiClient from './index'

export const orderApi = {
  getAll() {
    return apiClient.get('/v1/orders')
  },
  getExtended() {
    return apiClient.get('/v1/orders/extended')
  },
  getById(orderId) {
    return apiClient.get(`/v1/orders/${orderId}`)
  },
  create(payload) {
    return apiClient.post('/v1/orders', payload)
  },
  update(orderId, payload) {
    return apiClient.put(`/v1/orders/${orderId}`, payload)
  },
  delete(orderId) {
    return apiClient.delete(`/v1/orders/${orderId}`)
  },
  getOrderLines() {
    return apiClient.get('/v1/order-lines')
  }
}
