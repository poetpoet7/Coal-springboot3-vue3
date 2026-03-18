<template>
  <div class="manager-container">
    <div class="manager-header">
      <div class="manager-header-left" :style="{ width: `${sidebarWidth}px` }">
        <img src="@/assets/imgs/logo.png" alt="logo" />
        <div v-show="sidebarWidth > 150" class="title" style="cursor: pointer" @click="goHome">管理系统</div>
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
            <span style="color: white">{{ data.user.name }}</span>
            <el-icon color="#fff"><arrow-down /></el-icon>
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

    <div style="display: flex">
      <div class="manager-main-left" :style="{ width: `${sidebarWidth}px` }">
        <div class="sidebar-scroll-container">
          <el-menu :default-active="router.currentRoute.value.path" router>
            <el-menu-item index="/manager/home">
              <el-icon><HomeFilled /></el-icon>
              <span>系统首页</span>
            </el-menu-item>

            <el-sub-menu index="tongji">
              <template #title>
                <el-icon><DataAnalysis /></el-icon>
                <span>统计</span>
              </template>

              <el-sub-menu index="scjy">
                <template #title>
                  <el-icon><TrendCharts /></el-icon>
                  <span>生产经营</span>
                </template>

                <el-sub-menu index="zhsc">
                  <template #title>
                    <el-icon><Operation /></el-icon>
                    <span>综合生产</span>
                  </template>

                  <el-sub-menu index="czkb">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>产值、主要产品产量及固定资产投资快报</span>
                    </template>
                    <el-menu-item index="/manager/czkb/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/czkb/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="jingyingzongzhi">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>生产经营总值</span>
                    </template>
                    <el-menu-item index="/manager/jingyingzongzhi/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/jingyingzongzhi/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="chanpinchanxiaocun">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>主要工业产品产、销、存</span>
                    </template>
                    <el-menu-item index="/manager/chanpinchanxiaocun/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/chanpinchanxiaocun/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="chukouchanpin">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>主要出口产品情况</span>
                    </template>
                    <el-menu-item index="/manager/chukouchanpin/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/chukouchanpin/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="zhuyaojishujingji">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>主要技术经济指标</span>
                    </template>
                    <el-menu-item index="/manager/zhuyaojishujingji/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/zhuyaojishujingji/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="cphlqx">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>主要产品货流去向</span>
                    </template>
                    <el-menu-item index="/manager/cphlqx/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/cphlqx/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="dianlijishu">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>电力企业主营业务技术指标</span>
                    </template>
                    <el-menu-item index="/manager/dianlijishu/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/dianlijishu/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="huagongyewu">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>化工企业主营业务技术指标</span>
                    </template>
                    <el-menu-item index="/manager/huagongyewu/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/huagongyewu/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="feimeilaodonggongzi">
                    <template #title>
                      <el-icon><Folder /></el-icon>
                      <span>非煤产业劳动工资</span>
                    </template>
                    <el-menu-item index="/manager/feimeilaodonggongzi/manage"><el-icon><Edit /></el-icon><span>管理</span></el-menu-item>
                    <el-menu-item index="/manager/feimeilaodonggongzi/approve"><el-icon><Stamp /></el-icon><span>审批</span></el-menu-item>
                  </el-sub-menu>

                  <el-sub-menu index="tongjibaobiao">
                    <template #title>
                      <el-icon><DataLine /></el-icon>
                      <span>统计报表</span>
                    </template>
                    <el-menu-item index="/manager/touzikuaibao"><el-icon><Document /></el-icon><span>产值、主要产品产量及固定资产投资快报</span></el-menu-item>
                    <el-menu-item index="/manager/jingyingzongzhi/report"><el-icon><Document /></el-icon><span>生产经营总值</span></el-menu-item>
                    <el-menu-item index="/manager/chanpinchanxiaocun/report"><el-icon><Document /></el-icon><span>主要工业产品产、销、存</span></el-menu-item>
                    <el-menu-item index="/manager/chukouchanpin/report"><el-icon><Document /></el-icon><span>主要出口产品情况</span></el-menu-item>
                    <el-menu-item index="/manager/zhuyaojishujingji/report"><el-icon><Document /></el-icon><span>主要技术经济指标</span></el-menu-item>
                    <el-menu-item index="/manager/cphlqx/report"><el-icon><Document /></el-icon><span>主要产品货流去向</span></el-menu-item>
                    <el-menu-item index="/manager/dianlijishu/report"><el-icon><Document /></el-icon><span>电力企业主营业务技术指标</span></el-menu-item>
                    <el-menu-item index="/manager/huagongyewu/report"><el-icon><Document /></el-icon><span>化工企业主营业务技术指标</span></el-menu-item>
                    <el-menu-item index="/manager/feimeilaodonggongzi/report"><el-icon><Document /></el-icon><span>非煤产业劳动工资</span></el-menu-item>
                  </el-sub-menu>
                </el-sub-menu>
              </el-sub-menu>
            </el-sub-menu>

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

            <el-sub-menu index="user" v-if="data.user.roleid === 1">
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

        <div class="resizer" :class="{ 'is-dragging': isDragging }" @mousedown="startDragging" />
      </div>

      <div class="manager-main-right">
        <div v-show="tagsList.length > 0" class="tags-view-container">
          <div
            v-for="(tag, index) in tagsList"
            :key="tag.path"
            class="tags-view-item"
            :class="{ active: tag.path === router.currentRoute.value.path }"
            @click="handleTabClick(tag)"
          >
            <span>{{ tag.title }}</span>
            <el-icon class="el-icon-close" @click.stop="closeTag(index, tag)">
              <Close />
            </el-icon>
          </div>
        </div>

        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" :key="router.currentRoute.value.path" @updateUser="updateUser" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Close } from "@element-plus/icons-vue";
import router from "@/router/index.js";

const data = reactive({
  user: JSON.parse(localStorage.getItem("xm-user") || "{}")
});

const tagsList = ref([]);
const sidebarWidth = ref(220);
const isDragging = ref(false);

watch(
  () => router.currentRoute.value.path,
  () => setTags(router.currentRoute.value),
  { immediate: true }
);

function setTags(route) {
  if (["/manager/home", "/login", "/404"].includes(route.path) || !route.meta.name) return;
  if (tagsList.value.some((item) => item.path === route.path)) return;
  if (tagsList.value.length >= 10) tagsList.value.shift();
  tagsList.value.push({
    title: route.meta.name,
    path: route.path,
    name: route.name
  });
}

function handleTabClick(tag) {
  router.push(tag.path);
}

function closeTag(index, tag) {
  tagsList.value.splice(index, 1);
  const nextTag = tagsList.value[index] || tagsList.value[index - 1];
  if (nextTag && tag.path === router.currentRoute.value.path) {
    router.push(nextTag.path);
  } else if (!nextTag) {
    router.push("/manager/home");
  }
}

function startDragging() {
  isDragging.value = true;
  document.addEventListener("mousemove", onMouseMove);
  document.addEventListener("mouseup", stopDragging);
  document.body.style.userSelect = "none";
  document.body.style.cursor = "col-resize";
}

function onMouseMove(event) {
  if (!isDragging.value) return;
  let newWidth = event.clientX;
  if (newWidth < 60) newWidth = 60;
  if (newWidth > 600) newWidth = 600;
  sidebarWidth.value = newWidth;
}

function stopDragging() {
  isDragging.value = false;
  document.removeEventListener("mousemove", onMouseMove);
  document.removeEventListener("mouseup", stopDragging);
  document.body.style.userSelect = "auto";
  document.body.style.cursor = "default";
}

function logout() {
  localStorage.removeItem("xm-user");
  router.push("/login");
}

function goHome() {
  router.push("/manager/home");
}

function updateUser() {
  data.user = JSON.parse(localStorage.getItem("xm-user") || "{}");
}

if (!data.user.id) {
  logout();
  ElMessage.error("请登录！");
}
</script>

<style scoped>
@import "@/assets/css/manager.css";
</style>
