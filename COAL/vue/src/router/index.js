import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/manager/home' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      children: [
        { path: 'home', meta: { name: '系统首页' }, component: () => import('@/views/manager/Home.vue'), },
        { path: 'userinfo', meta: { name: '用户信息' }, component: () => import('@/views/manager/UserInfo.vue'), },
        { path: 'notice', meta: { name: '系统公告' }, component: () => import('@/views/manager/Notice.vue'), },
        { path: 'touzikuaibao', meta: { name: '产值、主要产品产量及固定资产投资快报' }, component: () => import('@/views/manager/TouZiKuaiBao.vue'), },
        { path: 'czkb/manage', meta: { name: '产值快报管理' }, component: () => import('@/views/manager/CzkbManage.vue'), },
        { path: 'czkb/approve', meta: { name: '产值快报审批' }, component: () => import('@/views/manager/CzkbApprove.vue'), },
        { path: 'person', meta: { name: '个人资料' }, component: () => import('@/views/manager/Person.vue'), },
        { path: 'password', meta: { name: '修改密码' }, component: () => import('@/views/manager/Password.vue'), },
      ]
    },
    {
      path: '/front',
      component: () => import('@/views/Front.vue'),
      children: [
        { path: 'home', component: () => import('@/views/front/Home.vue'), },
        { path: 'person', component: () => import('@/views/front/Person.vue'), }
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/404', component: () => import('@/views/404.vue') },
    //   通配符，对于所有找不到路径的路由都返回404
    { path: '/:pathMatch(.*)', redirect: '/404' }
  ]
})

export default router
