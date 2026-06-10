import apiClient from './index'

export const replenishmentApi = {
  getAll() {
    return apiClient.get('/orders/extended')
  },
  getById(id) {
    return apiClient.get(`/orders/extended/${id}`)
  },
  create(payload) {
    return apiClient.post('/orders', payload)
  },
  update(id, payload) {
    return apiClient.put(`/orders/${id}`, payload)
  },
  delete(id) {
    return apiClient.delete(`/orders/${id}`)
  }
}
