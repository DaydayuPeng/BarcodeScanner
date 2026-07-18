<template>
  <div>
    <h2>CMS 内容管理</h2>
    <el-tabs v-model="type" @tab-change="load">
      <el-tab-pane label="公司概况" name="company" />
      <el-tab-pane label="服务介绍" name="service" />
      <el-tab-pane label="产品介绍" name="product" />
    </el-tabs>
    <el-card v-loading="loading">
      <el-form label-width="100px">
        <el-form-item label="英文标题">
          <el-input v-model="form.titleEn" />
        </el-form-item>
        <el-form-item label="英文正文">
          <el-input v-model="form.contentEn" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="中文正文">
          <el-input v-model="form.contentZh" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="imagesText" type="textarea" :rows="3" placeholder="每行一个图片URL" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const type = ref('company')
const loading = ref(false)
const form = reactive({ titleEn: '', contentEn: '', contentZh: '' })
const imagesText = ref('')

async function load() {
  loading.value = true
  try {
    const res = await http.get(`/api/cms/${type.value}`)
    form.titleEn = res.data.titleEn || ''
    form.contentEn = res.data.contentEn || ''
    form.contentZh = res.data.contentZh || ''
    imagesText.value = (res.data.images || []).join('\n')
  } finally {
    loading.value = false
  }
}

async function save() {
  await http.put(`/api/cms/${type.value}`, {
    titleEn: form.titleEn,
    contentEn: form.contentEn,
    contentZh: form.contentZh,
    images: imagesText.value.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
  })
  ElMessage.success('保存成功')
}

onMounted(load)
</script>
