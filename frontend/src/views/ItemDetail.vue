<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getItem } from '@/api/item'
import { useUserStore } from '@/stores/user'

// B5 详情页：联系方式只在详情页展示；「编辑/删除」入口仅对本人或 ADMIN 显示。
// TODO(M4): 编辑页提交与删除二次确认（ElMessageBox.confirm + deleteItem）在 M4 实现。
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const item = ref(null)
const loading = ref(false)
const errorMessage = ref('')
// 'notFound'（404 空态）/ 'badRequest'（400 参数错误）/ 'error'（其他错误）
const errorType = ref('')

// 前端仅控制入口显隐，真正的权限判定在后端（ADMIN / 本人）
const canManage = computed(() => {
  if (!userStore.isLoggedIn || !item.value) return false
  return userStore.isAdmin || userStore.user.id === item.value.userId
})

const manageHint = computed(() => {
  if (!canManage.value) return ''
  if (userStore.isAdmin && item.value?.userId !== userStore.user.id) {
    return '管理员可管理该商品'
  }
  return '这是你发布的商品'
})

function firstFieldMessage(error) {
  const fieldErrors = error?.data?.fieldErrors
  if (!fieldErrors || typeof fieldErrors !== 'object') return ''
  return Object.values(fieldErrors).find((message) => typeof message === 'string' && message) || ''
}

async function loadItem() {
  loading.value = true
  errorMessage.value = ''
  errorType.value = ''
  item.value = null

  try {
    item.value = await getItem(route.params.id)
  } catch (error) {
    if (error?.code === 10005) {
      errorType.value = 'notFound'
      errorMessage.value = '商品不存在或已被删除'
    } else if (error?.code === 10001) {
      errorType.value = 'badRequest'
      errorMessage.value = firstFieldMessage(error) || '商品编号不合法'
    } else {
      errorType.value = 'error'
      errorMessage.value = error?.message || '商品详情加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, loadItem, { immediate: true })

function goBackToList() {
  // 从列表进入时优先回退，保留列表的搜索/排序/分页 URL query；直接打开详情则跳到列表
  if (window.history.state?.back) {
    router.back()
    return
  }
  router.push({ name: 'item-list' })
}

function handleEdit() {
  // TODO(M4): 编辑页数据回填与保存（ItemForm.vue）在 M4 完成
  if (!item.value) return
  router.push({ name: 'item-edit', params: { id: item.value.id } })
}

function formatPrice(price) {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '-'
}
</script>

<template>
  <section class="page page--detail">
    <div class="page__head">
      <el-button text @click="goBackToList">← 返回商品列表</el-button>
    </div>

    <div v-loading="loading" class="detail">
      <!-- 404：商品不存在或已删除 -->
      <el-empty v-if="errorType === 'notFound'" description="商品不存在或已被删除">
        <el-button type="primary" @click="goBackToList">返回商品列表</el-button>
      </el-empty>

      <!-- 400：商品编号不合法 / 其他加载错误 -->
      <div v-else-if="errorMessage" class="state-block">
        <el-alert
          :title="errorMessage"
          :type="errorType === 'badRequest' ? 'warning' : 'error'"
          show-icon
          :closable="false"
        />
        <div class="state-block__actions">
          <el-button type="primary" @click="loadItem">重新加载</el-button>
          <el-button @click="goBackToList">返回商品列表</el-button>
        </div>
      </div>

      <el-card v-else-if="item" shadow="never" class="detail-card">
        <h1 class="detail-card__title">{{ item.item }}</h1>
        <p class="detail-card__price">￥{{ formatPrice(item.itemPrice) }}</p>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品编号">{{ item.id }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ item.item }}</el-descriptions-item>
          <el-descriptions-item label="价格">￥{{ formatPrice(item.itemPrice) }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">
            <span class="detail-card__contact">{{ item.contactInfo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发布者 ID">{{ item.userId }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ item.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ item.updatedAt }}</el-descriptions-item>
        </el-descriptions>

        <!-- 本人或 ADMIN 可见；删除按钮 M4 接入真实操作 -->
        <div v-if="canManage" class="detail-card__actions">
          <el-tag type="success" effect="plain">{{ manageHint }}</el-tag>
          <div class="detail-card__buttons">
            <el-button type="primary" @click="handleEdit">编辑商品</el-button>
            <el-tooltip content="删除功能将在 M4 阶段实现" placement="top">
              <span>
                <el-button type="danger" disabled>删除商品</el-button>
              </span>
            </el-tooltip>
          </div>
        </div>
      </el-card>
    </div>
  </section>
</template>
