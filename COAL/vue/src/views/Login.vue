<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">集团管理系统</div>
      <div class="login-welcome">欢 迎 登 录</div>
      <el-form ref="formRef" :model="data.form" :rules="data.rules">
        <el-form-item prop="username">
          <el-input :prefix-icon="User" size="large" v-model="data.form.username" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input show-password :prefix-icon="Lock" size="large" v-model="data.form.password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="large" type="primary" class="login-btn" @click="login">登 录</el-button>
        </el-form-item>

      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const data = reactive({
  form: {},
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ]
  }
})
// 用于表单校验
const formRef = ref()

const login = () => {
  formRef.value.validate(valid => {
    if (valid) { // 表示表单校验通过
      request.post('/login', data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('登录成功')
          // 存储用户信息到浏览器的缓存 (将接受的res.data JSON数据转换为字符串,数据里面包括token)
          localStorage.setItem('xm-user', JSON.stringify(res.data))
          router.push('/manager/home')
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
  background-image: url("@/assets/imgs/login-bg.png");
  background-size: cover;
  background-position: center;
  position: relative;
}

.login-container::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  z-index: 1;
}

.login-box {
  width: 400px;
  padding: 40px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  z-index: 2;
  transition: all 0.3s ease;
}

.login-box:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px 0 rgba(0, 0, 0, 0.45);
}

.login-title {
  font-weight: 700;
  font-size: 30px;
  text-align: center;
  margin-bottom: 10px;
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.login-welcome {
  font-weight: 500;
  font-size: 18px;
  text-align: center;
  margin-bottom: 30px;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 5px;
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #0066bc 0%, #004a8f 100%);
  border: none;
  font-weight: 600;
  letter-spacing: 2px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 15px rgba(0, 102, 188, 0.4);
}

.register-link {
  text-align: right;
  color: #fff;
  font-size: 14px;
}

.register-link a,
.register-link :deep(a) {
  color: #ffd04b;
  text-decoration: none;
  font-weight: 600;
}

.register-link a:hover,
.register-link :deep(a):hover {
  text-decoration: underline;
}

:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.9) !important;
  border-radius: 8px !important;
}
</style>