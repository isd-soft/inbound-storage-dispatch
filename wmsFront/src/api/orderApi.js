import apiClient from './index'

export const orderApi = {
  getAll() {
    return apiClient.get('/v1/orders/extended')
  },
  getById(id) {
    return apiClient.get(`/v1/orders/extended/${id}`)
  },
  create(payload) {
    return apiClient.post('/v1/orders', payload)
  },
  update(id, payload) {
    return apiClient.put(`/v1/orders/${id}`, payload)
  },
  delete(id) {
    return apiClient.delete(`/v1/orders/${id}`)
  },
  getProducts() {
    return apiClient.get('/products/quantities')
  },
  getLocationsForDispatch() {
    return apiClient.get('/locations/dispatches')
  },
}
