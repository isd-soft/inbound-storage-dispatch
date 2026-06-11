import apiClient from '@/api'
import { inventoryApi } from '@/api/inventoryApi'
import { replenishmentApi } from '@/api/replenishmentApi'

const LOW_STOCK_THRESHOLD = 10
const OPEN_TASK_STATUSES = ['CREATED', 'ASSIGNED', 'IN_PROGRESS']
const COMPLETED_TASK_STATUSES = ['COMPLETED', 'DONE']

const toMetricNumber = (value) => {
  const numberValue = Number(value ?? 0)
  return Number.isFinite(numberValue) ? numberValue : 0
}

const sumInventoryQuantity = (stockItems = []) => {
  return stockItems.reduce((total, stock) => total + Number(stock.availableQuantity ?? stock.quantity ?? 0), 0)
}

const countLowStockAlerts = (stockItems = []) => {
  // TODO: Replace this client-side threshold check when the backend exposes a dedicated low-stock alerts endpoint.
  return stockItems.filter((stock) => Number(stock.availableQuantity ?? stock.quantity ?? 0) < LOW_STOCK_THRESHOLD).length
}

const countTasksByStatus = (tasks = [], statuses = []) => {
  return tasks.filter((task) => statuses.includes(task.status)).length
}

const normalizeSummary = (summary = {}) => ({
  totalInventory: toMetricNumber(summary.totalInventory),
  openTasks: toMetricNumber(summary.openTasks),
  completedTasks: toMetricNumber(summary.completedTasks),
  lowStockAlerts: toMetricNumber(summary.lowStockAlerts),
  lastUpdated: summary.lastUpdated || new Date().toISOString()
})

const aggregateDashboardSummary = async () => {
  const [inventoryResponse, tasksResponse] = await Promise.all([
    inventoryApi.getAllStock(),
    replenishmentApi.getAll()
  ])

  const stockItems = inventoryResponse.data || []
  const tasks = tasksResponse.data || []

  return normalizeSummary({
    totalInventory: sumInventoryQuantity(stockItems),
    openTasks: countTasksByStatus(tasks, OPEN_TASK_STATUSES),
    completedTasks: countTasksByStatus(tasks, COMPLETED_TASK_STATUSES),
    lowStockAlerts: countLowStockAlerts(stockItems)
  })
}

export const dashboardService = {
  async getDashboardSummary() {
    try {
      const response = await apiClient.get('/dashboard/summary')
      return normalizeSummary(response.data)
    } catch (error) {
      if (error.response && ![404, 405].includes(error.response.status)) {
        throw error
      }
      // TODO: Remove this client-side aggregation fallback after GET /api/dashboard/summary is available.
      return aggregateDashboardSummary()
    }
  },

  async getTotalInventory() {
    const summary = await this.getDashboardSummary()
    return summary.totalInventory
  },

  async getOpenTasksCount() {
    const summary = await this.getDashboardSummary()
    return summary.openTasks
  },

  async getCompletedTasksCount() {
    const summary = await this.getDashboardSummary()
    return summary.completedTasks
  },

  async getLowStockAlertsCount() {
    const summary = await this.getDashboardSummary()
    return summary.lowStockAlerts
  }
}
