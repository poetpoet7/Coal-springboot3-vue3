<template>
  <div style="width: 50%" class="card">
    <el-form ref="user" :model="data.user" label-width="80px" style="padding: 20px">
      <el-form-item prop="loginname" label="登录名">
        <el-input disabled v-model="data.user.loginname" placeholder="登录名"></el-input>
      </el-form-item>
      <el-form-item prop="username" label="姓名">
        <el-input v-model="data.user.username" placeholder="请输入姓名"></el-input>
      </el-form-item>
      <el-form-item prop="usersex" label="性别">
        <el-select v-model="data.user.usersex" placeholder="请选择性别" style="width: 100%">
          <el-option label="男" value="男"></el-option>
          <el-option label="女" value="女"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item prop="telephone" label="电话">
        <el-input v-model="data.user.telephone" placeholder="请输入电话"></el-input>
      </el-form-item>
      <el-form-item prop="email" label="邮箱">
        <el-input v-model="data.user.email" placeholder="请输入邮箱"></el-input>
      </el-form-item>
      <div style="text-align: center">
        <el-button type="primary" @click="update">保 存</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const data = reactive({
  user: JSON.parse(localStorage.getItem('xm-user') || '{}')
})

// 自定义内置触发事件 updateUser
const emit = defineEmits(['updateUser'])

const update = () => {
  // 构建更新数据，使用 userid 作为主键
  const updateData = {
    userid: data.user.userid,
    username: data.user.username,
    usersex: data.user.usersex,
    telephone: data.user.telephone,
    email: data.user.email
  }
  
  request.put('/userinfo/update', updateData).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      // 更新 localStorage 中的用户信息
      const storedUser = JSON.parse(localStorage.getItem('xm-user') || '{}')
      storedUser.username = data.user.username
      storedUser.usersex = data.user.usersex
      storedUser.telephone = data.user.telephone
      storedUser.email = data.user.email
      // 同时更新 name 字段以兼容显示
      storedUser.name = data.user.username
      localStorage.setItem('xm-user', JSON.stringify(storedUser))
      // 触发事件通知父组件更新用户信息
      emit('updateUser')
    } else {
      ElMessage.error(res.msg)
    }
  })
}
</script>

<style scoped>

</style>