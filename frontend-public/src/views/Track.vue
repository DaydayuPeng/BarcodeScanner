<template>
  <section class="track">
    <h1>Track Parcel</h1>
    <p>Enter tracking numbers (one per line) or a keyword prefix (6+ characters).</p>
    <div class="panel">
      <textarea v-model="input" rows="6" placeholder="SF1234567890&#10;YT9876543210"></textarea>
      <div class="row">
        <label>
          <input type="radio" value="list" v-model="mode" /> Batch numbers
        </label>
        <label>
          <input type="radio" value="keyword" v-model="mode" /> Fuzzy keyword
        </label>
        <button @click="query" :disabled="loading">{{ loading ? 'Searching...' : 'Search' }}</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
    <table v-if="rows.length">
      <thead>
        <tr>
          <th>Tracking No</th>
          <th>Status</th>
          <th>Inbound Time</th>
          <th>Shelf</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.trackingNo + (row.inboundTime || '')">
          <td>{{ row.trackingNo }}</td>
          <td>
            <span :class="row.status === 1 ? 'ok' : 'miss'">
              {{ row.status === 1 ? 'In warehouse' : (row.message || 'Not found') }}
            </span>
          </td>
          <td>{{ row.inboundTime || '-' }}</td>
          <td>{{ row.shelfNo || '-' }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api'

const input = ref('')
const mode = ref('list')
const rows = ref([])
const loading = ref(false)
const error = ref('')

async function query() {
  error.value = ''
  loading.value = true
  try {
    const text = input.value.trim()
    if (!text) {
      error.value = 'Please enter tracking info'
      return
    }
    let payload
    if (mode.value === 'keyword') {
      payload = { keyword: text.split(/\r?\n/)[0].trim() }
    } else {
      payload = {
        trackingNos: text.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
      }
    }
    const res = await http.post('/api/inbound/query', payload)
    rows.value = res.data || []
  } catch (e) {
    error.value = e.message || 'Query failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.track { max-width: 920px; padding-top: 20px; animation: rise .5s ease both; }
h1 {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3rem);
  margin-bottom: 8px;
}
p { color: var(--muted); }
.panel {
  margin-top: 20px;
  background: rgba(255,255,255,.7);
  border: 1px solid rgba(20,35,31,.08);
  border-radius: 18px;
  padding: 18px;
}
textarea {
  width: 100%;
  border: 1px solid rgba(20,35,31,.15);
  border-radius: 12px;
  padding: 12px;
  resize: vertical;
  background: #fff;
}
.row {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}
button {
  margin-left: auto;
  background: var(--moss);
  color: #fff;
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 700;
  cursor: pointer;
}
button:disabled { opacity: .6; cursor: wait; }
.error { color: #b42318; margin-top: 10px; }
table {
  width: 100%;
  margin-top: 22px;
  border-collapse: collapse;
  background: rgba(255,255,255,.65);
  border-radius: 14px;
  overflow: hidden;
}
th, td {
  text-align: left;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(20,35,31,.06);
}
.ok { color: var(--moss); font-weight: 700; }
.miss { color: #9a3412; }
@keyframes rise {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
</style>
