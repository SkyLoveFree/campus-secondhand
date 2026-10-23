<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

// TODO(M3/M4): M2 只做发布/编辑表单骨架，以下逻辑待 M3/M4 接入：
// 1. 编辑模式先调用 getItem(id) 回填表单（并处理 404/403）；
// 2. 发布调用 createItem、编辑调用 updateItem，成功后跳转详情或列表；
// 3. 400/fieldErrors 回填到对应表单项。
const route = useRoute()
const router = useRouter()

const formRef = ref()
const submitting = ref(false)
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

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  // TODO(M3/M4): 在这里调用 createItem / updateItem，本阶段不提交、不 mock 数据
  submitting.value = false
  ElMessage.info(isEdit.value ? '保存功能将在 M4 阶段接入' : '发布功能将在 M3 阶段接入')
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <section class="page page--narrow">
    <el-alert
      class="form-tip"
      type="info"
      :closable="false"
      title="该页面为 M2 骨架：编辑回填与提交保存将在 M3/M4 阶段完成"
    />

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
