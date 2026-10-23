<script setup>
// B4 列表卡片：只展示商品名称、价格、发布时间；
// 联系方式只在详情页展示（docs/requirements.md 界面要求）
defineProps({
  item: {
    type: Object,
    required: true,
  },
})

function formatPrice(price) {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '-'
}
</script>

<template>
  <router-link :to="{ name: 'item-detail', params: { id: item.id } }" class="item-card">
    <h2 class="item-card__name" :title="item.item">{{ item.item }}</h2>
    <p class="item-card__price">￥{{ formatPrice(item.itemPrice) }}</p>
    <p class="item-card__time">发布于 {{ item.createdAt }}</p>
  </router-link>
</template>
