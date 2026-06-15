import apiClient from './index'

export const replenishmentApi = {
  getAll() {
    return apiClient.get('/replenishments')
  },
  getById(id) {
    return apiClient.get(`/replenishments/${id}`)
  },
  create(payload) {
    return apiClient.post('/replenishments', payload)
  },
  update(id, payload) {
    return apiClient.put(`/replenishments/${id}`, payload)
  },
  assign(taskId, operatorId) {
    return apiClient.post(`/tasks/${taskId}/operators/${operatorId}`)
  },
  delete(id) {
    return apiClient.delete(`/replenishments/${id}`)
  },
  filter(filters) {
    return apiClient.post('/replenishments/filter', null, { params: filters })
  }
}
