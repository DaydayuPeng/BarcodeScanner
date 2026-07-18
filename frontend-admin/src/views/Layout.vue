<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">TuGuLu</div>
      <el-menu :default-active="$route.path" router background-color="#0f2a24" text-color="#d9e8e2" active-text-color="#c4a35a">
        <el-menu-item index="/inbound">扫码入库</el-menu-item>
        <el-menu-item index="/work-orders">工单管理</el-menu-item>
        <el-menu-item index="/cms">CMS 内容</el-menu-item>
        <el-menu-item index="/stats">统计分析</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ userStore.userInfo?.realName }}（{{ userStore.userInfo?.role }}）</span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #0f2a24; }
.brand {
  color: #c4a35a;
  font-size: 22px;
  font-weight: 700;
  padding: 20px 16px;
  letter-spacing: .04em;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eee;
  background: #fff;
}
</style>
