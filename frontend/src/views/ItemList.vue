<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listItems } from '@/api/item'

const DEFAULT_PAGE = 1
const DEFAULT_SIZE = 10
const SORT_OPTIONS = ['price_asc', 'price_desc']

const route = useRoute()
const router = useRouter()

const items = ref([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const keywordInput = ref('')

// 以 URL query 为单一数据源：缺省/非法值在这里归一化，默认值（page=1、size=10）不写入 URL
const filters = computed(() => {
  const query = route.query
  const page = Number.parseInt(query.page, 10)
  const size = Number.parseInt(query.size, 10)
  return {
    page: Number.isInteger(page) && page >= 1 ? page : DEFAULT_PAGE,
    size: Number.isInteger(size) && size >= 1 && size <= 50 ? size : DEFAULT_SIZE,
    keyword: typeof query.keyword === 'string' ? query.keyword.trim() : '',
    sort: SORT_OPTIONS.includes(query.sort) ? query.sort : '',
  }
})

// 输入框草稿与 URL 中的关键词保持同步（清空/前进后退时也能正确回显）
watch(
  () => filters.value.keyword,
  (keyword) => {
    keywordInput.value = keyword
  },
  { immediate: true }
)

// 请求序号：快速切换搜索/排序时，只允许最新一次响应更新页面，避免旧结果覆盖新结果
let requestSeq = 0

async function loadItems() {
  const seq = requestSeq + 1
  requestSeq = seq
  loading.value = true
  errorMessage.value = ''

  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size,
    }
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.sort) params.sort = filters.value.sort

    const data = await listItems(params)
    if (seq !== requestSeq) return

    items.value = Array.isArray(data?.list) ? data.list : []
    total.value = Number.isFinite(data?.total) ? data.total : 0
  } catch (error) {
    if (seq !== requestSeq) return
    items.value = []
    total.value = 0
    errorMessage.value = error?.message || '商品列表加载失败，请稍后重试'
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

watch(() => route.query, loadItems, { immediate: true })

// 更新 URL query（默认值省略），route.query 变化后由上面的 watch 自动重新加载
function updateQuery(patch) {
  const next = { ...filters.value, ...patch }
  const query = {}
  if (next.keyword) query.keyword = next.keyword
  if (next.sort) query.sort = next.sort
  if (next.page > DEFAULT_PAGE) query.page = String(next.page)
  if (next.size !== DEFAULT_SIZE) query.size = String(next.size)
  router.replace({ name: 'item-list', query })
}

function handleSearch() {
  updateQuery({ keyword: keywordInput.value.trim(), page: DEFAULT_PAGE })
}

function handleResetSearch() {
  keywordInput.value = ''
  updateQuery({ keyword: '', page: DEFAULT_PAGE })
}

function handleSortChange(sort) {
  updateQuery({ sort: sort || '', page: DEFAULT_PAGE })
}

function handlePageChange(page) {
  updateQuery({ page })
}

function formatPrice(price) {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '-'
}
</script>

<template>
  <section class="page">
    <div class="page__head">
      <h1 class="page__title">商品列表</h1>
      <p v-if="!errorMessage" class="page__subtitle">共 {{ total }} 件商品</p>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keywordInput"
        class="toolbar__search"
        placeholder="搜索商品名称"
        maxlength="50"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleResetSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>

      <el-select
        class="toolbar__sort"
        :model-value="filters.sort"
        placeholder="排序方式"
        @change="handleSortChange"
      >
        <el-option label="默认排序（最新发布）" value="" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
      </el-select>
    </div>

    <div v-if="errorMessage" class="state-block">
      <el-alert :title="errorMessage" type="error" show-icon :closable="false" />
      <el-button class="state-block__retry" @click="loadItems">重新加载</el-button>
    </div>

    <div v-else v-loading="loading" class="item-grid">
      <!-- 列表卡片只展示名称、价格、发布时间；联系方式仅在详情页展示 -->
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="{ name: 'item-detail', params: { id: item.id } }"
        class="item-card"
      >
        <h2 class="item-card__name">{{ item.item }}</h2>
        <p class="item-card__price">￥{{ formatPrice(item.itemPrice) }}</p>
        <p class="item-card__time">发布于 {{ item.createdAt }}</p>
      </router-link>

      <el-empty
        v-if="!loading && items.length === 0"
        class="item-grid__empty"
        description="暂无商品"
      />
    </div>

    <div v-if="!errorMessage && total > 0" class="pagination">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="filters.size"
        :current-page="filters.page"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>
