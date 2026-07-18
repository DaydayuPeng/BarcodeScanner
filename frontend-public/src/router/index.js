import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import CmsPage from '../views/CmsPage.vue'
import Track from '../views/Track.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/company', component: CmsPage, props: { type: 'company' } },
    { path: '/service', component: CmsPage, props: { type: 'service' } },
    { path: '/product', component: CmsPage, props: { type: 'product' } },
    { path: '/track', component: Track }
  ],
  scrollBehavior: () => ({ top: 0 })
})

export default router
