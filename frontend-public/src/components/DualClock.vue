<template>
  <div class="clocks" aria-label="Time display">
    <div>
      <small>Beijing</small>
      <strong>{{ beijing }}</strong>
    </div>
    <div>
      <small>Local</small>
      <strong>{{ local }}</strong>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import http from '../api'

const beijing = ref('--')
const local = ref('--')
let timer

async function refresh() {
  try {
    const res = await http.get('/api/common/current-time')
    beijing.value = res.data.beijingTime
    local.value = res.data.localTime
  } catch {
    // ignore transient errors
  }
}

onMounted(() => {
  refresh()
  timer = setInterval(refresh, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.clocks {
  justify-self: end;
  display: flex;
  gap: 14px;
  font-variant-numeric: tabular-nums;
}
.clocks div {
  background: rgba(255,255,255,.55);
  border: 1px solid rgba(20,35,31,.08);
  border-radius: 10px;
  padding: 6px 10px;
  min-width: 140px;
}
small {
  display: block;
  color: var(--muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: .08em;
}
strong { font-size: 13px; }
@media (max-width: 860px) {
  .clocks { justify-self: start; width: 100%; }
}
</style>
