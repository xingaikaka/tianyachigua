# ChiguaVideo 后台配置指南

## 概述

本项目采用Spring Profiles机制来管理不同环境的配置，支持开发环境和生产环境的灵活切换。

## 配置文件结构

```
backend/ruoyi-admin/src/main/resources/
├── application.yml                    # 主配置文件（通用配置）
├── application-dev.yml               # 开发环境配置
├── application-prod.yml              # 生产环境配置
├── application-chigua-dev.yml        # Chigua模块开发环境配置
├── application-chigua-prod.yml       # Chigua模块生产环境配置
├── application-encryption-dev.yml    # 数据加密开发环境配置
└── application-encryption-prod.yml   # 数据加密生产环境配置
```

## 环境切换

### 方式一：使用启动脚本（推荐）

#### 开发环境
```bash
# 进入backend目录
cd backend

# 复制并配置开发环境变量文件
cp env.development.template .env.development
# 编辑 .env.development 文件，填入开发环境配置

# 启动开发环境
./start-dev.sh
```

#### 生产环境
```bash
# 进入backend目录
cd backend

# 复制并配置生产环境变量文件
cp env.production.template .env.production
# 编辑 .env.production 文件，填入生产环境配置

# 启动生产环境
./start-prod.sh
```

### 方式二：环境变量切换

#### 开发环境
```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_PROFILES_INCLUDE=encryption-dev,chigua-dev
mvn spring-boot:run -pl ruoyi-admin
```

#### 生产环境
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_PROFILES_INCLUDE=encryption-prod,chigua-prod
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 方式三：启动参数切换

#### 开发环境
```bash
mvn spring-boot:run -pl ruoyi-admin -Dspring-boot.run.profiles=dev
```

#### 生产环境
```bash
java -jar ruoyi-admin.jar --spring.profiles.active=prod --spring.profiles.include=encryption-prod,chigua-prod
```

## 配置说明

### 主要差异

| 配置项 | 开发环境 | 生产环境 |
|--------|----------|----------|
| 数据库连接池 | 较小（最大10） | 较大（最大50） |
| Swagger文档 | 启用 | 禁用 |
| 日志级别 | DEBUG | INFO/WARN |
| 数据加密 | 可选 | 强制启用 |
| 图片加密 | 可选 | 强制启用 |
| URL签名 | 可选 | 强制启用 |
| Redis连接池 | 较小 | 较大 |

### 环境变量配置

#### 必须配置的生产环境变量

```bash
# 数据库配置
DB_PASSWORD=your-production-db-password

# R2存储配置  
R2_ACCESS_KEY_ID=your-production-r2-access-key-id
R2_SECRET_KEY=your-production-r2-secret-key

# 加密配置
IMAGE_ENCRYPTION_KEY=your-production-image-encryption-key
URL_SIGNATURE_SECRET=your-production-url-signature-secret
DATA_ENCRYPTION_SECRET_KEY=your-production-data-encryption-secret

# JWT配置
JWT_SECRET=your-very-strong-production-jwt-secret-key
```

#### 可选配置的环境变量

```bash
# 服务器配置
SERVER_PORT=8080
CONTEXT_PATH=/

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# 应用配置
APP_NAME=ChiguaVideo
UPLOAD_PATH=./uploadPath
LOG_PATH=./logs
```

## 安全建议

### 开发环境
- 可以使用默认配置进行快速开发
- 敏感配置可以暂时硬编码在配置文件中
- 建议关闭加密功能以便调试

### 生产环境
- **必须**使用环境变量或密钥管理系统配置敏感信息
- **禁止**在配置文件中硬编码密钥
- **强制**启用所有安全功能（加密、签名等）
- 定期轮换密钥和证书

## 配置验证

### 检查当前配置
```bash
# 查看激活的配置文件
curl http://localhost:8080/actuator/env | jq '.propertySources[].name'

# 查看具体配置值（开发环境）
curl http://localhost:8080/actuator/configprops
```

### 验证环境变量
```bash
# 检查必要的环境变量是否已设置
env | grep -E "(SPRING_PROFILES|DB_|REDIS_|R2_|JWT_|ENCRYPTION_)"
```

## 故障排除

### 常见问题

1. **启动时提示配置文件未找到**
   - 检查 `SPRING_PROFILES_INCLUDE` 是否正确设置
   - 确认配置文件名称是否正确

2. **数据库连接失败**
   - 检查数据库URL、用户名、密码是否正确
   - 确认数据库服务是否启动

3. **Redis连接失败**
   - 检查Redis配置是否正确
   - 确认Redis服务是否启动

4. **R2存储访问失败**
   - 检查R2访问密钥是否正确
   - 确认网络连接是否正常

### 日志查看

#### 开发环境
```bash
# 实时查看日志
tail -f logs/spring.log

# 查看特定级别日志
grep -i error logs/spring.log
```

#### 生产环境
```bash
# 查看应用日志
tail -f logs/application.log

# 查看GC日志
tail -f logs/gc.log

# 查看错误日志
grep -i error logs/application.log
```

## 迁移指南

如果从旧版本配置迁移，请按以下步骤操作：

1. **备份现有配置**
   ```bash
   cp -r src/main/resources src/main/resources.backup
   ```

2. **更新配置文件**
   - 删除旧的 `application-druid.yml`、`application-chigua.yml`、`application-encryption.yml`
   - 使用新的环境特定配置文件

3. **配置环境变量**
   - 复制环境变量模板文件
   - 填入实际配置值

4. **测试配置**
   - 先在开发环境测试
   - 确认无误后再部署到生产环境

## 联系方式

如有配置问题，请联系开发团队或查看项目文档。
