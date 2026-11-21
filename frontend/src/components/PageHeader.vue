<template>
  <div class="page-header">
    <div class="page-header__title">
      <h3>{{ title }}</h3>
      <span v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</span>
    </div>
    <div class="page-header__actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dh-border, #d4c5a9);
  position: relative;
}
/* 标题左侧朱砂金珠竖线装饰 */
.page-header::before {
  content: '';
  position: absolute;
  left: 0; bottom: -1px;
  width: 80px; height: 3px;
  background: linear-gradient(90deg, var(--dh-cinnabar, #c13a2a), var(--dh-earth-yellow, #c89a2e));
  border-radius: 2px;
}
.page-header__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.page-header__title h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  font-family: var(--dh-font-display);
  color: var(--dh-ink, #2c1810);
  letter-spacing: 2px;
  position: relative;
  padding-left: 14px;
}
/* 标题前朱砂小方块（仿印章） */
.page-header__title h3::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 8px; height: 8px;
  background: var(--dh-cinnabar, #c13a2a);
  border-radius: 2px;
  box-shadow: 0 0 0 1px rgba(201,168,76,0.4);
}
.page-header__subtitle {
  color: var(--dh-text-light, #6b5a4a);
  font-size: 14px;
}
.page-header__actions {
  display: flex;
  gap: 10px;
}
</style>
