<template>
  <div>
    <div class="toolbar">
      <h2>工单管理</h2>
      <el-button type="primary" @click="showCreate = true">创建工单</el-button>
    </div>
    <el-tabs v-model="status" @tab-change="load">
      <el-tab-pane v-for="t in tabs" :key="t.value" :label="t.label" :name="t.value" />
    </el-tabs>
    <el-table :data="records" v-loading="loading">
      <el-table-column prop="id" label="工单号" width="90" />
      <el-table-column prop="customerId" label="客户编号" />
      <el-table-column prop="batchNo" label="批次号" />
      <el-table-column prop="totalQuantity" label="件数" width="80" />
      <el-table-column prop="packerName" label="打包人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-button link type="primary" @click="openLogs(row)">流转</el-button>
          <el-button v-if="row.status==='PENDING_PREP'" link type="success" @click="act(row,'start-pack')">确认备货</el-button>
          <el-button v-if="row.status==='PENDING_PACK'" link type="success" @click="openQty(row,'confirm-pack')">确认打包</el-button>
          <el-button v-if="row.status==='PACKED'" link @click="openQty(row,'replenish')">补货</el-button>
          <el-button v-if="row.status==='PACKED'" link type="warning" @click="act(row,'seal')">封箱</el-button>
          <el-button v-if="row.status==='PENDING_SEAL'" link type="warning" @click="openShip(row)">确认发货</el-button>
          <el-button v-if="row.status==='PENDING_SHIP'" link type="danger" @click="act(row,'complete')">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px"
      background
      layout="prev, pager, next, total"
      :total="total"
      v-model:current-page="page"
      :page-size="size"
      @current-change="load"
    />

    <el-dialog v-model="showCreate" title="创建工单" width="420px">
      <el-form label-width="90px">
        <el-form-item label="客户编号"><el-input v-model="createForm.customerId" /></el-form-item>
        <el-form-item label="批次号"><el-input v-model="createForm.batchNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate=false">取消</el-button>
        <el-button type="primary" @click="createOrder">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qtyVisible" :title="qtyMode==='replenish'?'补货':'确认打包'" width="360px">
      <el-input-number v-model="qty" :min="1" />
      <template #footer>
        <el-button type="primary" @click="submitQty">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="确认发货" width="420px">
      <el-form label-width="80px">
        <el-form-item label="件数"><el-input-number v-model="shipForm.quantity" :min="1" /></el-form-item>
        <el-form-item label="重量KG"><el-input-number v-model="shipForm.weight" :min="0.01" :step="0.1" /></el-form-item>
        <el-form-item label="体积m³"><el-input-number v-model="shipForm.volume" :min="0.01" :step="0.01" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitShip">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logsVisible" title="流转记录" size="420px">
      <el-timeline>
        <el-timeline-item v-for="(log,i) in logs" :key="i" :timestamp="log.operateTime">
          {{ log.operatorName }}：{{ log.fromStatus || '新建' }} → {{ log.toStatus }}
          <div v-if="log.remark" style="color:#888">{{ log.remark }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const tabs = [
  { label: '待备货', value: 'PENDING_PREP' },
  { label: '待打包', value: 'PENDING_PACK' },
  { label: '已打包', value: 'PACKED' },
  { label: '待封箱', value: 'PENDING_SEAL' },
  { label: '待发货', value: 'PENDING_SHIP' },
  { label: '已完成', value: 'COMPLETED' }
]

const status = ref('PENDING_PREP')
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const showCreate = ref(false)
const createForm = reactive({ customerId: '', batchNo: '' })
const qtyVisible = ref(false)
const qtyMode = ref('confirm-pack')
const qty = ref(1)
const current = ref(null)
const shipVisible = ref(false)
const shipForm = reactive({ quantity: 1, weight: 1, volume: 0.1 })
const logsVisible = ref(false)
const logs = ref([])

async function load() {
  loading.value = true
  try {
    const res = await http.get('/api/work-order/list', { params: { status: status.value, page: page.value, size } })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function createOrder() {
  await http.post('/api/work-order/create', createForm)
  ElMessage.success('创建成功')
  showCreate.value = false
  status.value = 'PENDING_PREP'
  await load()
}

async function act(row, action) {
  const res = await http.put(`/api/work-order/${row.id}/${action}`)
  ElMessage.success(res.msg)
  await load()
}

function openQty(row, mode) {
  current.value = row
  qtyMode.value = mode
  qty.value = mode === 'replenish' ? 1 : (row.totalQuantity || 1)
  qtyVisible.value = true
}

async function submitQty() {
  if (qtyMode.value === 'replenish') {
    const res = await http.put(`/api/work-order/${current.value.id}/replenish`, { additionalQuantity: qty.value })
    ElMessage.success(res.msg)
  } else {
    const res = await http.put(`/api/work-order/${current.value.id}/confirm-pack`, { quantity: qty.value })
    ElMessage.success(res.msg)
  }
  qtyVisible.value = false
  await load()
}

function openShip(row) {
  current.value = row
  shipForm.quantity = row.totalQuantity || 1
  shipForm.weight = 1
  shipForm.volume = 0.1
  shipVisible.value = true
}

async function submitShip() {
  const res = await http.put(`/api/work-order/${current.value.id}/confirm-ship`, shipForm)
  ElMessage.success(res.msg)
  shipVisible.value = false
  await load()
}

async function openLogs(row) {
  const res = await http.get(`/api/work-order/${row.id}/logs`)
  logs.value = res.data
  logsVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; align-items:center; }
</style>
