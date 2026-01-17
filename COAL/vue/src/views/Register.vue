<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size: 24px; text-align: center; margin-bottom: 30px; color: #1450aa">欢 迎 注 册</div>
      <el-form ref="formRef" :model="data.form" :rules="data.rules">
        <el-form-item prop="loginname">
          <el-input :prefix-icon="User" size="large" v-model="data.form.loginname" placeholder="请输入登录名"></el-input>
        </el-form-item>
        <el-form-item prop="username">
          <el-input :prefix-icon="UserFilled" size="large" v-model="data.form.username" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input show-password :prefix-icon="Lock" size="large" v-model="data.form.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input show-password :prefix-icon="Lock" size="large" v-model="data.form.confirmPassword" placeholder="请确认密码"></el-input>
        </el-form-item>
        <el-form-item prop="roleid">
          <el-select v-model="data.form.roleid" placeholder="请选择角色" size="large" style="width: 100%">
            <el-option
              v-for="role in data.roles"
              :key="role.roleid"
              :label="role.rolename"
              :value="role.roleid"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="danweibianma">
          <el-select v-model="data.form.danweibianma" placeholder="请选择单位" size="large" style="width: 100%" filterable>
            <el-option
              v-for="danwei in data.danweiList"
              :key="danwei.bianMa"
              :label="danwei.mingCheng"
              :value="danwei.bianMa"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="large" type="primary" style="width: 100%" @click="register">注 册</el-button>
        </el-form-item>
        <div style="text-align: right">
          已有账号？请 <router-link to="/login">登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { User, UserFilled, Lock } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

// 自定义校验规则：确认密码
const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else {
    if (value !== data.form.password) {
      callback(new Error("确认密码跟原密码不一致!"))
    }
    callback()
  }
}

const data = reactive({
  form: {},
  roles: [],      // 角色列表
  danweiList: [], // 单位列表
  rules: {
    loginname: [
      { required: true, message: '请输入登录名', trigger: 'blur' },
      { pattern: /^[a-zA-Z][a-zA-Z0-9]*$/, message: '登录名只能包含英文字母和数字，且必须以字母开头', trigger: 'blur' }
    ],
    username: [
      { required: true, message: '请输入姓名', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ],
    confirmPassword: [
      { validator: validatePass, trigger: 'blur' }
    ],
    roleid: [
      { required: true, message: '请选择角色', trigger: 'change' }
    ],
    danweibianma: [
      { required: true, message: '请选择单位', trigger: 'change' }
    ]
  }
})

const formRef = ref()

// 页面加载时获取角色和单位列表
onMounted(() => {
  loadRoles()
  loadDanweiList()
})

// 加载角色列表
const loadRoles = () => {
  request.get('/roles/selectAll').then(res => {
    if (res.code === '200') {
      data.roles = res.data
    }
  })
}

// 加载单位列表
const loadDanweiList = () => {
  request.get('/danwei/selectAll').then(res => {
    if (res.code === '200') {
      data.danweiList = res.data
    }
  })
}

// 注册
const register = () => {
  formRef.value.validate(valid => {
    if (valid) {
      // 构建注册数据
      const registerData = {
        loginname: data.form.loginname,
        username: data.form.username,
        password: data.form.password,
        roleid: data.form.roleid,
        danweibianma: data.form.danweibianma
      }
      request.post('/register', registerData).then(res => {
        if (res.code === '200') {
          ElMessage.success('注册成功')
          router.push('/login')
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(to top, #00467f, #a5cc82);
}
.login-box {
  width: 400px;
  padding: 30px;
  border-radius: 5px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.5);
}
</style>