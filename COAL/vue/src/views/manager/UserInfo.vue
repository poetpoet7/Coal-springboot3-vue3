<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.username" prefix-icon="Search" style="width: 200px; margin-right: 10px" placeholder="用户姓名"></el-input>
      <el-input v-model="data.loginname" prefix-icon="Search" style="width: 200px; margin-right: 10px" placeholder="登录名"></el-input>
      <el-button type="info" plain @click="load">查询</el-button>
      <el-button type="warning" plain style="margin: 0 10px" @click="reset">重置</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="loginname" label="登录名" width="120" />
        <el-table-column prop="username" label="用户姓名" width="120" />
        <el-table-column prop="rolename" label="角色" width="120" />
        <el-table-column prop="danweibianma" label="单位编码" width="120" />
        <el-table-column prop="usersex" label="性别" width="80" />
        <el-table-column prop="telephone" label="电话" width="150" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.userid)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="用户信息" v-model="data.formVisible" width="40%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="100px" style="padding: 20px">
        <el-form-item prop="loginname" label="登录名">
          <el-input v-model="data.form.loginname" placeholder="请输入登录名"></el-input>
        </el-form-item>
        <el-form-item prop="username" label="用户姓名">
          <el-input v-model="data.form.username" placeholder="请输入用户姓名"></el-input>
        </el-form-item>
        <el-form-item prop="password" label="密码" v-if="!data.form.userid">
          <el-input v-model="data.form.password" placeholder="不填则使用默认密码123456" show-password></el-input>
        </el-form-item>
        <el-form-item prop="roleid" label="角色">
          <el-select v-model="data.form.roleid" placeholder="请选择角色">
            <el-option v-for="role in data.roles" :key="role.roleid" :label="role.rolename" :value="role.roleid"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="danweibianma" label="单位编码">
          <el-input v-model="data.form.danweibianma" placeholder="请输入单位编码"></el-input>
        </el-form-item>
        <el-form-item prop="usersex" label="性别">
          <el-select v-model="data.form.usersex" placeholder="请选择性别">
            <el-option value="男" label="男"></el-option>
            <el-option value="女" label="女"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="telephone" label="电话">
          <el-input v-model="data.form.telephone" placeholder="请输入电话"></el-input>
        </el-form-item>
        <el-form-item prop="email" label="邮箱">
          <el-input v-model="data.form.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取 消</el-button>
          <el-button type="primary" @click="save">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>

import {reactive, onMounted} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit} from "@element-plus/icons-vue";

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  username: null,
  loginname: null,
  ids: [],
  roles: []
})

// 加载用户列表
const load = () => {
  request.get('/userinfo/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      username: data.username,
      loginname: data.loginname
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.records || res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

// 加载角色列表
const loadRoles = () => {
  request.get('/roles/selectAll').then(res => {
    if (res.code === '200') {
      data.roles = res.data || []
    }
  })
}

const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}
const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}
const add = () => {
  request.post('/userinfo/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/userinfo/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const save = () => {
  data.form.userid ? update() : add()
}

const del = (userid) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete('/userinfo/delete/' + userid).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {
    console.error(err)
  })
}
const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据")
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(res => {
    request.delete("/userinfo/delete/batch", {data: data.ids}).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(err => {
    console.error(err)
  })
}
const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.userid)
}

const reset = () => {
  data.username = null
  data.loginname = null
  load()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>
