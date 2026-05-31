<template>
  <div class="totp-container">
    <!-- 已绑定状态 -->
    <div v-if="isBound" class="totp-bound">
      <el-alert
        title="Google验证器已绑定"
        type="success"
        description="您的账户已开启两步验证，登录时需要输入Google Authenticator中的6位动态码。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />
      <el-form :model="unbindForm" :rules="unbindRules" ref="unbindForm" label-width="120px">
        <el-form-item label="当前动态码" prop="code">
          <el-input
            v-model="unbindForm.code"
            placeholder="输入Google Authenticator中的6位动态码"
            maxlength="6"
            style="width: 240px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="unbinding" @click="handleUnbind">解除绑定</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 未绑定状态 -->
    <div v-else>
      <el-alert
        title="提升账户安全性"
        type="warning"
        description="绑定 Google Authenticator 后，登录时需要额外输入6位动态码，有效防止账号被盗。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <div v-if="!qrData">
        <el-button type="primary" @click="startBind" :loading="loading">开始绑定</el-button>
      </div>

      <div v-else class="bind-steps">
        <el-steps :active="bindStep" align-center style="margin-bottom: 24px;">
          <el-step title="扫描二维码" />
          <el-step title="验证动态码" />
          <el-step title="绑定完成" />
        </el-steps>

        <div v-if="bindStep === 0" class="step-content">
          <p style="margin-bottom:12px;color:#606266;">请使用 <strong>Google Authenticator</strong> 或其他验证器应用扫描以下二维码：</p>
          <div class="qr-wrapper">
            <canvas ref="qrCanvas" width="180" height="180" />
          </div>
          <p style="margin-top:12px;font-size:12px;color:#909399;">
            无法扫码？手动输入密钥：<strong style="letter-spacing:2px;">{{ qrData.secret }}</strong>
          </p>
          <el-button type="primary" style="margin-top:16px;" @click="bindStep = 1">已扫描，下一步</el-button>
        </div>

        <div v-if="bindStep === 1" class="step-content">
          <p style="margin-bottom:16px;color:#606266;">请输入 Google Authenticator 中显示的6位动态码：</p>
          <el-form :model="bindForm" :rules="bindRules" ref="bindForm" label-width="0">
            <el-form-item prop="code">
              <el-input
                v-model="bindForm.code"
                placeholder="6位动态码"
                maxlength="6"
                style="width: 200px;"
                @keyup.enter.native="handleBind"
              />
            </el-form-item>
            <el-form-item>
              <el-button @click="bindStep = 0">上一步</el-button>
              <el-button type="primary" :loading="binding" @click="handleBind">确认绑定</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getTotpQrCode, bindTotp, unbindTotp } from "@/api/login"

export default {
  name: "TotpBind",
  props: {
    user: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      loading: false,
      binding: false,
      unbinding: false,
      bindStep: 0,
      qrData: null,
      bindForm: { code: "" },
      unbindForm: { code: "" },
      bindRules: {
        code: [
          { required: true, message: "请输入动态码", trigger: "blur" },
          { len: 6, message: "动态码为6位数字", trigger: "blur" }
        ]
      },
      unbindRules: {
        code: [
          { required: true, message: "请输入动态码", trigger: "blur" },
          { len: 6, message: "动态码为6位数字", trigger: "blur" }
        ]
      }
    }
  },
  computed: {
    isBound() {
      return this.user && this.user.totpEnabled === 1
    }
  },
  methods: {
    startBind() {
      this.loading = true
      getTotpQrCode().then(res => {
        if (res.code === 200) {
          this.qrData = res.data
          this.bindStep = 0
          this.$nextTick(() => {
            this.renderQrCode(res.data.otpAuthUrl)
          })
        } else {
          this.$message.error(res.msg || "获取二维码失败")
        }
      }).catch(() => {
        this.$message.error("获取二维码失败，请重试")
      }).finally(() => {
        this.loading = false
      })
    },
    renderQrCode(text) {
      const canvas = this.$refs.qrCanvas
      if (!canvas) return
      const ctx = canvas.getContext("2d")
      const size = 180
      // 使用简单的 otpauth URL 绘制文字提示（若有 qrcode 库则生成真实二维码）
      ctx.fillStyle = "#ffffff"
      ctx.fillRect(0, 0, size, size)
      ctx.fillStyle = "#333"
      ctx.font = "12px sans-serif"
      ctx.textAlign = "center"
      // 动态加载 qrcode 库
      if (window.QRCode) {
        this.drawWithQRLib(text, canvas)
      } else {
        const script = document.createElement("script")
        script.src = "https://cdn.jsdelivr.net/npm/qrcode@1.5.3/build/qrcode.min.js"
        script.onload = () => this.drawWithQRLib(text, canvas)
        document.head.appendChild(script)
      }
    },
    drawWithQRLib(text, canvas) {
      if (window.QRCode) {
        window.QRCode.toCanvas(canvas, text, { width: 180, margin: 2 }, (err) => {
          if (err) {
            console.error("QR生成失败", err)
          }
        })
      }
    },
    handleBind() {
      this.$refs.bindForm.validate(valid => {
        if (!valid) return
        this.binding = true
        bindTotp({ secret: this.qrData.secret, code: this.bindForm.code }).then(res => {
          if (res.code === 200) {
            this.$message.success("Google验证器绑定成功！")
            this.bindStep = 2
            this.$emit("refresh")
          } else {
            this.$message.error(res.msg || "绑定失败")
          }
        }).catch(() => {
          this.$message.error("绑定失败，请重试")
        }).finally(() => {
          this.binding = false
        })
      })
    },
    handleUnbind() {
      this.$refs.unbindForm.validate(valid => {
        if (!valid) return
        this.$confirm("解绑后登录将不再需要动态码，确认解绑？", "提示", {
          type: "warning"
        }).then(() => {
          this.unbinding = true
          unbindTotp({ code: this.unbindForm.code }).then(res => {
            if (res.code === 200) {
              this.$message.success("Google验证器已解绑")
              this.unbindForm.code = ""
              this.$emit("refresh")
            } else {
              this.$message.error(res.msg || "解绑失败")
            }
          }).catch(() => {
            this.$message.error("解绑失败，请重试")
          }).finally(() => {
            this.unbinding = false
          })
        }).catch(() => {})
      })
    }
  }
}
</script>

<style scoped>
.totp-container {
  padding: 10px 0;
  max-width: 560px;
}
.bind-steps {
  margin-top: 8px;
}
.step-content {
  text-align: center;
  padding: 16px 0;
}
.qr-wrapper {
  display: inline-block;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}
</style>
