import request from '@/utils/request'

// 接口契约见 docs/api-spec.md §3.5–3.9；参数名严格使用契约字段（item/itemPrice/contactInfo）
export function listItems(params = {}) {
  return request.get('/items', { params })
}

export function getItem(id) {
  return request.get(`/items/${id}`)
}

export function createItem(data) {
  return request.post('/items', data)
}

export function updateItem(id, data) {
  return request.put(`/items/${id}`, data)
}

export function deleteItem(id) {
  return request.delete(`/items/${id}`)
}
