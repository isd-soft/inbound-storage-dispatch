import apiClient from './index'

export const productApi = {
  getAllProducts() {
    return apiClient.get('/products')
  },
  getProductById(productId) {
    return apiClient.get(`/products/${productId}`)
  },
  createProduct(payload) {
    return apiClient.post('/products', payload)
  },
  updateProduct(productId, payload) {
    return apiClient.put(`/products/${productId}`, payload)
  },
  deleteProduct(productId) {
    return apiClient.delete(`/products/${productId}`)
  },
  searchProducts(params) {
    return apiClient.get('/products/search', { params })
  },
  getCategories() {
    return apiClient.get('/categories')
  },
  createCategory(payload) {
    return apiClient.post('/categories', payload)
  },
  getAllProductsWithQuantityInZone(payload) {
    return apiClient.post('/quantities', payload)
  }
}
