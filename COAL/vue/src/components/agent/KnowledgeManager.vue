<template>
  <div class="knowledge-manager">
    <h2 style="margin-bottom: 20px;">知识库管理</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-statistic title="文档数量" :value="stats.documentCount" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="切片总数" :value="stats.chunkCount" />
      </el-col>
    </el-row>

    <!-- 上传和检索 -->
    <el-card style="margin-bottom: 20px;">
      <template #header>文档检索</template>
      <div class="search-row">
        <el-input v-model="searchQuery" placeholder="输入想要检索的业务问题..." style="width: 400px;" />
        <el-button type="primary" @click="doSearch" style="margin-left: 10px;">检索</el-button>
      </div>
      <div v-if="searchResult" class="search-result">
        <div class="result-text">{{ searchResult }}</div>
      </div>
    </el-card>

    <!-- 上传新文档 -->
    <el-card style="margin-bottom: 20px;">
      <template #header>上传新文档</template>
      <el-form label-width="80px">
        <el-form-item label="文档标题">
          <el-input v-model="newDoc.title" placeholder="如：煤炭统计填报规范" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newDoc.category" placeholder="选择分类">
            <el-option label="制度文件" value="制度文件" />
            <el-option label="操作手册" value="操作手册" />
            <el-option label="培训材料" value="培训材料" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="newDoc.content" type="textarea" :rows="8" placeholder="粘贴文档内容..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="uploadDoc">上传入库</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文档列表 -->
    <el-card>
      <template #header>已入库文档</template>
      <el-table :data="documents" style="width: 100%">
        <el-table-column prop="title" label="文档名称" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="chunkCount" label="切片数" width="80" />
        <el-table-column prop="uploadedAt" label="上传时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button link type="danger" size="small" @click="deleteDoc(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const BASE_URL = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'
const token = JSON.parse(localStorage.getItem('xm-user') || '{}').token || ''

const stats = ref({ documentCount: 0, chunkCount: 0 })
const documents = ref([])
const searchQuery = ref('')
const searchResult = ref('')
const newDoc = reactive({ title: '', category: '制度文件', content: '' })

// 加载数据
async function loadData() {
  try {
    const [statsRes, docsRes] = await Promise.all([
      fetch(`${BASE_URL}/api/knowledge/stats`, { headers: { token } }),
      fetch(`${BASE_URL}/api/knowledge/list`, { headers: { token } })
    ])
    const statsData = await statsRes.json()
    const docsData = await docsRes.json()
    if (statsData.code === '200') stats.value = statsData.data
    if (docsData.code === '200') documents.value = docsData.data
  } catch (e) {
    console.error('加载失败', e)
  }
}

async function doSearch() {
  if (!searchQuery.value.trim()) return
  const res = await fetch(`${BASE_URL}/api/knowledge/search?query=${encodeURIComponent(searchQuery.value)}`, { headers: { token } })
  const data = await res.json()
  searchResult.value = data.code === '200' ? data.data : '检索失败'
}

async function uploadDoc() {
  if (!newDoc.title || !newDoc.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  const res = await fetch(`${BASE_URL}/api/knowledge/upload`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', token },
    body: JSON.stringify(newDoc)
  })
  const data = await res.json()
  if (data.code === '200') {
    ElMessage.success('文档已入库')
    newDoc.title = ''
    newDoc.content = ''
    loadData()
  } else {
    ElMessage.error('上传失败')
  }
}

async function deleteDoc(row) {
  const res = await fetch(`${BASE_URL}/api/knowledge/${row.id}`, {
    method: 'DELETE',
    headers: { token }
  })
  const data = await res.json()
  if (data.code === '200') {
    ElMessage.success('已删除')
    loadData()
  }
}

loadData()
</script>

<style scoped>
.knowledge-manager {
  padding: 20px;
}
.search-row {
  display: flex;
  align-items: center;
}
.search-result {
  margin-top: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border-left: 3px solid #409EFF;
  max-height: 300px;
  overflow-y: auto;
}
.result-text {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
}
</style>
