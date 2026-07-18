<template>
  <div>
    <h2>统计分析</h2>
    <el-card>
      <template #header>每日入库数量</template>
      <el-form inline>
        <el-form-item label="开始">
          <el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="endDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-button type="primary" @click="loadInbound">查询</el-button>
      </el-form>
      <el-table :data="inboundRows">
        <el-table-column prop="statDate" label="日期" />
        <el-table-column prop="totalCount" label="入库件数" />
      </el-table>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>每日打包重量（员工×客户）</template>
      <el-form inline>
        <el-form-item label="日期">
          <el-date-picker v-model="statDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-button type="primary" @click="loadPacking">查询</el-button>
      </el-form>
      <el-table :data="packingRows">
        <el-table-column prop="packerName" label="打包人" />
        <el-table-column prop="customerId" label="客户编号" />
        <el-table-column prop="totalWeight" label="总重量(KG)" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const today = new Date().toISOString().slice(0, 10)
const startDate = ref(today.slice(0, 8) + '01')
const endDate = ref(today)
const statDate = ref(today)
const inboundRows = ref([])
const packingRows = ref([])

async function loadInbound() {
  const res = await http.get('/api/stat/inbound', { params: { startDate: startDate.value, endDate: endDate.value } })
  inboundRows.value = res.data
}

async function loadPacking() {
  const res = await http.get('/api/stat/packing', { params: { statDate: statDate.value } })
  packingRows.value = res.data
}

onMounted(() => {
  loadInbound()
  loadPacking()
})
</script>
