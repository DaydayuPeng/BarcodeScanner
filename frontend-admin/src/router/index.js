import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Inbound from '../views/Inbound.vue'
import WorkOrders from '../views/WorkOrders.vue'
import Cms from '../views/Cms.vue'
import Stats from '../views/Stats.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      redirect: '/inbound',
      children: [
        { path: 'inbound', component: Inbound },
        { path: 'work-orders', component: WorkOrders },
        { path: 'cms', component: Cms },
        { path: 'stats', component: Stats }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) next('/login')
  else if (to.path === '/login' && token) next('/')
  else next()
})

export default router
