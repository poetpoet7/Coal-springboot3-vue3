import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/manager/home" },
    {
      path: "/manager",
      component: () => import("@/views/Manager.vue"),
      children: [
        { path: "home", meta: { name: "系统首页" }, component: () => import("@/views/manager/Home.vue") },
        { path: "userinfo", meta: { name: "用户信息" }, component: () => import("@/views/manager/UserInfo.vue") },
        { path: "notice", meta: { name: "系统公告" }, component: () => import("@/views/manager/Notice.vue") },

        { path: "czkb/manage", meta: { name: "产值快报管理" }, component: () => import("@/views/manager/CzkbManage.vue") },
        { path: "czkb/approve", meta: { name: "产值快报审批" }, component: () => import("@/views/manager/CzkbApprove.vue") },
        { path: "touzikuaibao", meta: { name: "产值、主要产品产量及固定资产投资快报" }, component: () => import("@/views/manager/TouZiKuaiBao.vue") },

        { path: "cphlqx/manage", meta: { name: "主要产品货流去向管理" }, component: () => import("@/views/manager/CphlqxManage.vue") },
        { path: "cphlqx/approve", meta: { name: "主要产品货流去向审批" }, component: () => import("@/views/manager/CphlqxApprove.vue") },
        { path: "cphlqx/report", meta: { name: "主要产品货流去向" }, component: () => import("@/views/manager/CphlqxReport.vue") },

        { path: "jingyingzongzhi/manage", meta: { name: "生产经营总值管理", moduleKey: "jingyingzongzhi" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "jingyingzongzhi/approve", meta: { name: "生产经营总值审批", moduleKey: "jingyingzongzhi" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "jingyingzongzhi/report", meta: { name: "生产经营总值", moduleKey: "jingyingzongzhi" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "chanpinchanxiaocun/manage", meta: { name: "主要工业产品产、销、存管理", moduleKey: "chanpinchanxiaocun" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "chanpinchanxiaocun/approve", meta: { name: "主要工业产品产、销、存审批", moduleKey: "chanpinchanxiaocun" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "chanpinchanxiaocun/report", meta: { name: "主要工业产品产、销、存", moduleKey: "chanpinchanxiaocun" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "chukouchanpin/manage", meta: { name: "主要出口产品情况管理", moduleKey: "chukouchanpin" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "chukouchanpin/approve", meta: { name: "主要出口产品情况审批", moduleKey: "chukouchanpin" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "chukouchanpin/report", meta: { name: "主要出口产品情况", moduleKey: "chukouchanpin" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "zhuyaojishujingji/manage", meta: { name: "主要技术经济指标管理", moduleKey: "zhuyaojishujingji" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "zhuyaojishujingji/approve", meta: { name: "主要技术经济指标审批", moduleKey: "zhuyaojishujingji" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "zhuyaojishujingji/report", meta: { name: "主要技术经济指标", moduleKey: "zhuyaojishujingji" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "dianlijishu/manage", meta: { name: "电力企业主营业务技术指标管理", moduleKey: "dianlijishu" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "dianlijishu/approve", meta: { name: "电力企业主营业务技术指标审批", moduleKey: "dianlijishu" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "dianlijishu/report", meta: { name: "电力企业主营业务技术指标", moduleKey: "dianlijishu" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "huagongyewu/manage", meta: { name: "化工企业主营业务技术指标管理", moduleKey: "huagongyewu" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "huagongyewu/approve", meta: { name: "化工企业主营业务技术指标审批", moduleKey: "huagongyewu" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "huagongyewu/report", meta: { name: "化工企业主营业务技术指标", moduleKey: "huagongyewu" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "feimeilaodonggongzi/manage", meta: { name: "非煤产业劳动工资管理", moduleKey: "feimeilaodonggongzi" }, component: () => import("@/views/manager/TongjiGenericManage.vue") },
        { path: "feimeilaodonggongzi/approve", meta: { name: "非煤产业劳动工资审批", moduleKey: "feimeilaodonggongzi" }, component: () => import("@/views/manager/TongjiGenericApprove.vue") },
        { path: "feimeilaodonggongzi/report", meta: { name: "非煤产业劳动工资", moduleKey: "feimeilaodonggongzi" }, component: () => import("@/views/manager/TongjiGenericReport.vue") },

        { path: "person", meta: { name: "个人资料" }, component: () => import("@/views/manager/Person.vue") },
        { path: "password", meta: { name: "修改密码" }, component: () => import("@/views/manager/Password.vue") }
      ]
    },
    {
      path: "/front",
      component: () => import("@/views/Front.vue"),
      children: [
        { path: "home", component: () => import("@/views/front/Home.vue") },
        { path: "person", component: () => import("@/views/front/Person.vue") }
      ]
    },
    { path: "/login", component: () => import("@/views/Login.vue") },
    { path: "/404", component: () => import("@/views/404.vue") },
    { path: "/:pathMatch(.*)", redirect: "/404" }
  ]
});

export default router;
