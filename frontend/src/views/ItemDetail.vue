<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getItem } from '@/api/item'
import { useUserStore } from '@/stores/user'

// TODO(M3/M4): 详情页完整业务在 M3/M4 完成——删除二次确认（ElMessageBox + deleteItem）、
// 404 空态交互、编辑/删除入口的完整权限联动（后端权限仍是唯一安全边界）
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const item = ref(null)
const loading = ref(false)
const errorMessage = ref('')

// 前端仅控制入口显隐，真正的权限判定在后端（ADMIN / 本人）
const canManage = computed(() => {
  if (!userStore.isLoggedIn || !item.value) return false
  return userStore.isAdmin || userStore.user.id === item.value.userId
})

async function loadItem() {
  loading.value = true
  errorMessage.value = ''
  item.value = null

  try {
    item.value = await getItem(route.params.id)
  } catch (error) {
    errorMessage.value =
      error?.code === 10005 ? '商品不存在或已被删除' : error?.message || '商品详情加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, loadItem, { immediate: true })

function handleEdit() {
  if (!item.value) return
  router.push({ name: 'item-edit', params: { id: item.value.id } })
}

function handleDelete() {
  // TODO(M3/M4): 改为 ElMessageBox.confirm 二次确认后调用 deleteItem，成功后刷新并返回列表
  ElMessage.info('删除功能将在 M4 阶段完成')
}

function formatPrice(price) {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '-'
}
</script>

<template>
  <section class="page">
    <div class="page__head">
      <el-button text @click="router.push({ name: 'item-list' })">← 返回商品列表</el-button>
    </div>

    <div v-loading="loading" class="detail">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
      />

      <el-card v-else-if="item" class="detail-card">
        <h1 class="detail-card__title">{{ item.item }}</h1>
        <p class="detail-card__price">￥{{ formatPrice(item.itemPrice) }}</p>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="联系方式">{{ item.contactInfo }}</el-descriptions-item>
          <el-descriptions-item label="发布者 ID">{{ item.userId }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ item.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ item.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="商品 ID">{{ item.id }}</el-descriptions-item>
        </el-descriptions>

        <!-- TODO(M3/M4): 编辑/删除入口在 M3/M4 与列表、详情完整联调 -->
        <div v-if="canManage" class="detail-card__actions">
          <el-button type="primary" @click="handleEdit">编辑商品</el-button>
          <el-button type="danger" @click="handleDelete">删除商品</el-button>
        </div>
      </el-card>
    </div>
  </section>
</template>
