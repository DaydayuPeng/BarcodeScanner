<template>
  <article class="page" v-loading="false">
    <p class="eyebrow">{{ typeLabel }}</p>
    <h1>{{ content.titleEn || 'Loading...' }}</h1>
    <div class="body" v-html="content.contentEn"></div>
    <div class="images" v-if="content.images?.length">
      <img v-for="(img, i) in content.images" :key="i" :src="img" :alt="content.titleEn" />
    </div>
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, watch } from 'vue'
import http from '../api'

const props = defineProps({ type: { type: String, required: true } })
const content = reactive({ titleEn: '', contentEn: '', images: [] })
const typeLabel = computed(() => ({
  company: 'Company',
  service: 'Service',
  product: 'Product'
}[props.type] || props.type))

async function load() {
  const res = await http.get(`/api/cms/${props.type}`)
  content.titleEn = res.data.titleEn
  content.contentEn = res.data.contentEn
  content.images = res.data.images || []
}

onMounted(load)
watch(() => props.type, load)
</script>

<style scoped>
.page {
  max-width: 860px;
  padding: 28px 0 40px;
  animation: rise .5s ease both;
}
.eyebrow {
  text-transform: uppercase;
  letter-spacing: .12em;
  color: var(--gold);
  font-weight: 700;
  font-size: 12px;
}
h1 {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3rem);
  margin: 8px 0 20px;
}
.body :deep(p) { line-height: 1.7; color: var(--muted); }
.images {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 28px;
}
.images img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 14px;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
</style>
