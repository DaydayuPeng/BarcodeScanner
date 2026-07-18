<template>
  <div>
    <h2>扫码入库</h2>
    <el-card>
      <el-form label-width="90px">
        <el-form-item label="快递单号">
          <el-input
            v-model="trackingInput"
            type="textarea"
            :rows="4"
            placeholder="每行一个单号，可用扫码枪回车换行"
          />
        </el-form-item>
        <el-form-item label="货架号">
          <el-input v-model="shelfNo" placeholder="可选，整批共用" style="width:240px" />
        </el-form-item>
        <el-form-item label="入库图片">
          <el-upload :http-request="uploadImage" :show-file-list="false">
            <el-button>上传图片</el-button>
          </el-upload>
          <span v-if="imageUrl" style="margin-left:12px">{{ imageUrl }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">提交入库</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="result" :title="result.msg" type="success" show-icon :closable="false" />
      <el-table v-if="result?.data?.failList?.length" :data="result.data.failList" style="margin-top:12px">
        <el-table-column prop="trackingNo" label="单号" />
        <el-table-column prop="reason" label="原因" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const trackingInput = ref('')
const shelfNo = ref('')
const imageUrl = ref('')
const loading = ref(false)
const result = ref(null)

async function uploadImage(option) {
  const form = new FormData()
  form.append('file', option.file)
  const res = await http.post('/api/common/upload', form)
  imageUrl.value = res.data.url
  ElMessage.success('上传成功')
}

async function submit() {
  const nos = trackingInput.value.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
  if (!nos.length) {
    ElMessage.warning('请输入至少一单号')
    return
  }
  loading.value = true
  try {
    const res = await http.post('/api/inbound/scan', {
      list: nos.map(trackingNo => ({ trackingNo, shelfNo: shelfNo.value || undefined })),
      imageUrl: imageUrl.value || undefined
    })
    result.value = res
    ElMessage.success(res.msg)
    trackingInput.value = ''
  } finally {
    loading.value = false
  }
}
</script>
