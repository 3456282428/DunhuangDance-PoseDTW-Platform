<template>
  <div class="pagination-wrapper">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:currentPage', 'update:pageSize', 'pagination'])

function handleCurrentChange(val) {
  emit('update:currentPage', val)
  emit('pagination', { currentPage: val, pageSize: props.pageSize })
}

function handleSizeChange(val) {
  emit('update:pageSize', val)
  emit('pagination', { currentPage: 1, pageSize: val })
}
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
