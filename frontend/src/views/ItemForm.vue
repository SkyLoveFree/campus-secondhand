<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createItem, getItem, updateItem } from '@/api/item'

// 发布/编辑共用表单：契约见 docs/api-spec.md §3.7/§3.8；归属由后端 Session 决定，前端不传 userId
const route = useRoute()
const router = useRouter()

const formRef = ref()
const submitting = ref(false)
const loadingItem = ref(false)
const isEdit = computed(() => route.name === 'item-edit')
const pageTitle = computed(() => (isEdit.value ? '编辑商品' : '发布商品'))

const form = reactive({
  item: '',
  itemPrice: undefined,
  contactInfo: '',
})

// 字符串字段：trim 后 1–maxLength 个字符（空串/纯空白视为未填写）
function validateText(maxLength, emptyMessage) {
  return (rule, value, callback) => {
    const text = typeof value === 'string' ? value.trim() : ''
    if (!text) {
      callback(new Error(emptyMessage))
      return
    }
    if (text.length > maxLength) {
      callback(new Error(`长度不能超过 ${maxLength} 个字符`))
      return
    }
    callback()
  }
}

// 校验规则与 docs/api-spec.md §3.7 一致：名称/联系方式 trim 后 1–100，价格 >0 且 ≤999999.99
const rules = {
  item: [{ validator: validateText(100, '请输入商品名称'), trigger: 'blur' }],
  itemPrice: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (typeof value !== 'number' || Number.isNaN(value)) {
          callback(new Error('请输入正确的价格'))
          return
        }
        if (value <= 0) {
          callback(new Error('价格必须大于 0'))
          return
        }
        if (value > 999999.99) {
          callback(new Error('价格不能超过 999999.99'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  contactInfo: [{ validator: validateText(100, '请输入联系方式'), trigger: 'blur' }],
}

// 编辑模式：进入时回填表单；商品不存在则提示并返回列表
async function loadForEdit() {
  if (!isEdit.value) return
  loadingItem.value = true
  try {
    const data = await getItem(route.params.id)
    form.item = data.item
    form.itemPrice = Number(data.itemPrice)
    form.contactInfo = data.contactInfo
  } catch (error) {
    ElMessage.error(error?.code === 10005 ? '商品不存在或已被删除' : error?.message || '商品信息加载失败')
    router.push({ name: 'item-list' })
  } finally {
    loadingItem.value = false
  }
}
loadForEdit()

// 400 参数校验失败：把后端 fieldErrors 逐字段回填到表单项（request.js 已弹第一条提示）
function applyFieldErrors(fieldErrors) {
  if (!fieldErrors || typeof fieldErrors !== 'object') return
  formRef.value?.fields?.forEach((field) => {
    const message = fieldErrors[field.prop]
    if (message) {
      field.validateMessage = message
      field.validateState = 'error'
    }
  })
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  // 字符串 trim 后提交（与后端校验口径一致）
  const payload = {
    item: form.item.trim(),
    itemPrice: form.itemPrice,
    contactInfo: form.contactInfo.trim(),
  }
  try {
    const saved = isEdit.value
      ? await updateItem(route.params.id, payload)
      : await createItem(payload)
    ElMessage.success(isEdit.value ? '商品已保存' : '商品发布成功')
    router.push({ name: 'item-detail', params: { id: saved.id } })
  } catch (error) {
    if (error?.code === 10001) {
      applyFieldErrors(error.data?.fieldErrors)
    } else if (error?.code === 10005) {
      ElMessage.error('商品不存在或已被删除')
      router.push({ name: 'item-list' })
    } else if (error?.code === 10004) {
      // 403 提示已由 request.js 统一弹出，退出编辑回列表
      router.push({ name: 'item-list' })
    }
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <section v-loading="loadingItem" class="page page--narrow">
    <div class="page__head">
      <h1 class="page__title">{{ pageTitle }}</h1>
      <p v-if="isEdit" class="page__subtitle">商品 ID：{{ route.params.id }}</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="商品名称" prop="item">
        <el-input
          v-model="form.item"
          maxlength="100"
          show-word-limit
          placeholder="例如：二手自行车"
        />
      </el-form-item>

      <el-form-item label="价格（元）" prop="itemPrice">
        <el-input-number
          v-model="form.itemPrice"
          :min="0.01"
          :max="999999.99"
          :precision="2"
          :step="1"
          controls-position="right"
        />
      </el-form-item>

      <el-form-item label="联系方式" prop="contactInfo">
        <el-input
          v-model="form.contactInfo"
          maxlength="100"
          show-word-limit
          placeholder="例如：微信 bike_2024"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '发布商品' }}
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>
