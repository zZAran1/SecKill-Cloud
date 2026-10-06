<script setup lang="ts">
</script>

<template>
  <div class="nf-page">
    <p class="nf-code">404</p>
    <p class="nf-title">页面不存在</p>
    <p class="nf-desc">你要找的页面可能已被移除，或地址有误</p>
    <router-link :to="{ name: 'products' }" class="btn btn-primary">回到首页</router-link>
  </div>
</template>
