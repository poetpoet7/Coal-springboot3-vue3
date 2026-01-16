<template>
  <div class="manager-container">
    <div class="manager-header">
      <div class="manager-header-left" :style="{ width: sidebarWidth + 'px' }">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title" v-show="sidebarWidth > 150" @click="goHome" style="cursor: pointer;">管理系统</div>
      </div>
      <div class="manager-header-center">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/manager/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ router.currentRoute.value.meta.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="manager-header-right">
        <el-dropdown style="cursor: pointer">
          <div style="padding-right: 20px; display: flex; align-items: center">
            <img style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
            <span style="margin-left: 5px; color: white">{{ data.user.name }}</span><el-icon color="#fff"><arrow-down /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/manager/person')">个人资料</el-dropdown-item>
              <el-dropdown-item @click="router.push('/manager/password')">修改密码</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <!-- 下面部分开始 -->
    <div style="display: flex">
      <div class="manager-main-left" :style="{ width: sidebarWidth + 'px' }">
        <div class="sidebar-scroll-container">
          <el-menu :default-active="router.currentRoute.value.path"
                   router
          >
            <el-menu-item index="/manager/home">
              <el-icon><HomeFilled /></el-icon>
              <span>系统首页</span>
            </el-menu-item>
            
            <!-- 统计 -->
            <el-sub-menu index="tongji">
              <template #title>
                <el-icon><DataAnalysis /></el-icon>
                <span>统计</span>
              </template>
              
              <!-- 生产经营 -->
              <el-sub-menu index="scjy">
                <template #title>
                  <el-icon><TrendCharts /></el-icon>
                  <span>生产经营</span>
                </template>
                
                <!-- 综合生产 -->
                <el-sub-menu index="zhsc">
                  <template #title>
                    <el-icon><Operation /></el-icon>
                    <span>综合生产</span>
                  </template>
                  
                  <!-- 产值、主要产品产量及固定资产投资快报 -->
                  <el-sub-menu index="czkb">
                    <template #title>
                      <el-icon><Document /></el-icon>
                      <span>产值、主要产品产量及固定资产投资快报</span>
                    </template>
                    <el-menu-item index="/manager/czkb/manage">
                      <el-icon><Edit /></el-icon>
                      <span>管理</span>
                    </el-menu-item>
                    <el-menu-item index="/manager/czkb/approve">
                      <el-icon><Stamp /></el-icon>
                      <span>审批</span>
                    </el-menu-item>
                  </el-sub-menu>
                  
                  <!-- 统计报表 -->
                  <el-sub-menu index="tjbb">
                    <template #title>
                      <el-icon><DataLine /></el-icon>
                      <span>统计报表</span>
                    </template>
                    <el-menu-item index="/manager/touzikuaibao">
                      <el-icon><PieChart /></el-icon>
                      <span>产值、主要产品产量及固定资产投资快报</span>
                    </el-menu-item>
                  </el-sub-menu>
                </el-sub-menu>
              </el-sub-menu>
            </el-sub-menu>
            
            <!-- 信息管理 -->
            <el-sub-menu index="info">
              <template #title>
                <el-icon><Files /></el-icon>
                <span>信息管理</span>
              </template>
              <el-menu-item index="/manager/notice">
                <el-icon><Bell /></el-icon>
                <span>系统公告</span>
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 用户管理 -->
            <el-sub-menu index="user">
              <template #title>
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </template>
              <el-menu-item index="/manager/userinfo">
                <el-icon><UserFilled /></el-icon>
                <span>用户信息</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>
        <!-- 拖动调整宽度的手柄 -->
        <div class="resizer" 
             :class="{ 'is-dragging': isDragging }"
             @mousedown="startDragging"
        ></div>
      </div>
      <div class="manager-main-right">
        <!-- 标签栏 -->
        <div class="tags-view-container" v-show="tagsList.length > 0">
          <div
              v-for="(tag, index) in tagsList"
              :key="tag.path"
              class="tags-view-item"
              :class="{ 'active': tag.path === router.currentRoute.value.path }"
              @click="handleTabClick(tag)"
          >
            <span>{{ tag.title }}</span>
            <el-icon class="el-icon-close" @click.stop="closeTag(index, tag)">
              <Close />
            </el-icon>
          </div>
        </div>

        <!-- 内容渲染区，带状态保留 -->
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" :key="router.currentRoute.value.path" @updateUser="updateUser" />
          </keep-alive>
        </router-view>
      </div>
    </div>
    <!-- 下面部分结束 -->


  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted, watch } from "vue";
import router from "@/router/index.js";
import { ElMessage } from "element-plus";
import { Close } from "@element-plus/icons-vue";

const data = reactive({
  //取出来的字符串信息转换为JSON数据
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

// 标签页列表
const tagsList = ref([]);

// 监听路由变化，添加标签
watch(() => router.currentRoute.value.path, (newPath) => {
  setTags(router.currentRoute.value);
}, { immediate: true });

function setTags(route) {
  // 不记录首页、登录页、404等特殊页面
  if (['/manager/home', '/login', '/404'].includes(route.path) || !route.meta.name) return;
  
  const isExist = tagsList.value.some(item => item.path === route.path);
  if (!isExist) {
    if (tagsList.value.length >= 10) { // 限制最多打开10个标签，防止性能问题
      tagsList.value.shift();
    }
    tagsList.value.push({
      title: route.meta.name,
      path: route.path,
      name: route.name
    });
  }
}

// 点击标签跳转
const handleTabClick = (tag) => {
  router.push(tag.path);
}

// 关闭标签
const closeTag = (index, tag) => {
  tagsList.value.splice(index, 1);
  const nextTag = tagsList.value[index] || tagsList.value[index - 1];
  if (nextTag) {
    tag.path === router.currentRoute.value.path && router.push(nextTag.path);
  } else {
    router.push('/manager/home');
  }
}

// 侧边栏宽度相关逻辑
const sidebarWidth = ref(220) // 默认宽度
const isDragging = ref(false)

const startDragging = (e) => {
  isDragging.value = true
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', stopDragging)
  // 防止拖拽时选中文字
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
}

const onMouseMove = (e) => {
  if (isDragging.value) {
    // 限制最小和最大宽度
    let newWidth = e.clientX
    if (newWidth < 60) newWidth = 60
    if (newWidth > 600) newWidth = 600
    sidebarWidth.value = newWidth
  }
}

const stopDragging = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', stopDragging)
  document.body.style.userSelect = 'auto'
  document.body.style.cursor = 'default'
}

const logout = () => {
  localStorage.removeItem('xm-user')
  router.push('/login')
}

// 点击"管理系统"回到首页并刷新
const goHome = () => {
  router.push('/manager/home').then(() => {
    // location.reload() // 移除刷新，使用标签页跳转更平滑
  })
}

const updateUser = () => {
  data.user =  JSON.parse(localStorage.getItem('xm-user') || '{}')
}

if (!data.user.id) {
  logout()
  ElMessage.error('请登录！')
}
</script>

<style scoped>
@import "@/assets/css/manager.css";
</style>