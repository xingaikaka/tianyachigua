# 天涯吃瓜项目部署包

## 文件说明
- ruoyi-admin.jar: Java后端应用
- vue-admin-prod.tar.gz: Vue后台管理前端
- chigua-web-prod.tar.gz: 用户前端

## 部署说明

### 1. Java后端部署
```bash
# 上传 ruoyi-admin.jar 到服务器
scp ruoyi-admin.jar root@server:/opt/chigua/

# 在服务器上启动
ssh root@server "cd /opt/chigua && nohup java -jar ruoyi-admin.jar > app.log 2>&1 &"
```

### 2. Vue后台管理部署
```bash
# 上传并解压到nginx目录
scp vue-admin-prod.tar.gz root@server:/var/www/admin/
ssh root@server "cd /var/www/admin && tar -xzf vue-admin-prod.tar.gz && systemctl reload nginx"
```

### 3. 用户前端部署
```bash
# 上传并解压到nginx目录
scp chigua-web-prod.tar.gz root@server:/var/www/chigua-web/
ssh root@server "cd /var/www/chigua-web && tar -xzf chigua-web-prod.tar.gz && systemctl reload nginx"
```

## 访问地址
- 用户前端: http://服务器IP
- 后台管理: http://服务器IP:81
- API接口: http://服务器IP:8080
