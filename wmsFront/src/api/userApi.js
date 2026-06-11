import apiClient from './index'

export const userApi = {
  getAll() {
    return apiClient.get('/supervisor/users')
  },
  register(payload) {
    return apiClient.post('/supervisor/users/register', payload)
  }
}
