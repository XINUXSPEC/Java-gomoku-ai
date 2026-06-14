# 五子棋 AI 对战系统

基于 Java 实现的五子棋游戏，支持人机对战和双人对战模式，集成了 MiniMax 和 KataGomo 两种 AI 引擎。

## 🎮 功能特性

- **游戏模式**
  - 人机对战：玩家 vs AI
  - 双人对战：玩家 vs 玩家
  - 先手选择：支持玩家先手或 AI 先手

- **AI 引擎**
  - **MiniMax**：速度快，落子即时，支持3档难度选择
  - **KataGomo (AlphaZero)**：棋力强，思考时间较长，基于神经网络

- **游戏操作**
  - 悔棋功能
  - 重新开始（切换先手）
  - 返回主页
  - 实时比分统计

- **界面特性**
  - 现代风格 UI 设计
  - 胜利动画效果
  - 玩家头像显示
  - 实时提示信息

## 🛠️ 技术栈

- **语言**：Java 8+
- **UI 框架**：Swing
- **AI 算法**：MiniMax + Alpha-Beta 剪枝 + 置换表 + 杀手棋
- **外部引擎**：KataGomo（基于 GTP 协议通信）

## 📁 项目结构

```
Gobang/
├── src/main/java/
│   ├── Controller/          # 控制器层
│   │   ├── GameController.java      # 游戏主控制器
│   │   └── ChessInputListener.java  # 棋盘点击监听器
│   ├── logic/               # 业务逻辑层
│   │   ├── GameSystem.java          # 游戏核心逻辑
│   │   └── EvaluatorRule.java       # 评估函数
│   ├── State/               # 状态管理
│   │   └── GameState.java           # 游戏状态对象
│   ├── ai/                  # AI 引擎模块
│   │   ├── AI_Engine.java           # AI 引擎接口
│   │   ├── Minimax/                 # MiniMax 实现
│   │   │   ├── MiniMaxEngine.java
│   │   │   ├── TranspositionTable.java
│   │   │   └── ZobristHash.java
│   │   └── KataGomo/                # KataGomo 接口
│   │       └── KataGomoEngine.java
│   ├── ui/                  # 界面层
│   │   ├── GameGui.java             # 游戏主界面
│   │   ├── StartPage.java           # 开始页面
│   │   ├── ToastNotification.java   # 提示组件
│   │   └── ModernConfirmDialog.java # 确认对话框
│   └── utils/               # 工具类
│       ├── GameConstants.java       # 常量定义
│       ├── BoardUtils.java          # 棋盘工具
│       └── SoundManager.java        # 音效管理
├── resources/               # 资源文件
│   └── images/              # 头像图片
└── out/                     # 编译输出目录
```

## 🚀 快速开始

### 编译项目

```bash
cd Gobang
javac -d out -sourcepath src/main/java src/main/java/**/*.java
```

### 运行游戏

```bash
java -cp out ui.StartPage
```

### 运行 AI 对战测试

```bash
java -cp out test.AIVsAITest
```

## 🎯 使用说明

### 开始游戏

1. 运行程序，进入开始页面
2. 选择游戏模式：人机对战或双人对战
3. 选择先手玩家（黑棋或白棋）
4. 选择 AI 引擎（仅人机对战）
   - **MiniMax**：速度快，适合快速对战
   - **KataGomo**：棋力强，适合挑战高难度

### 游戏操作

- **落子**：点击棋盘上的空位
- **悔棋**：点击悔棋按钮（可撤销上一步）
- **重新开始**：点击重新开始按钮（切换先手）
- **返回主页**：点击返回按钮（返回开始页面）

### AI 难度设置

- **简单**：MiniMax 搜索深度 3
- **中等**：MiniMax 搜索深度 5
- **困难**：MiniMax 搜索深度 7

## 🧠 AI 技术实现

### MiniMax 优化技术

| 优化技术 | 作用 |
|---------|------|
| **Alpha-Beta 剪枝** | 减少搜索树节点数量 |
| **置换表** | 缓存已计算的棋盘状态 |
| **Zobrist 哈希** | 快速计算棋盘唯一标识 |
| **杀手棋** | 优先搜索导致剪枝的走法 |
| **迭代加深** | 逐步加深搜索深度，支持时间控制 |
| **分层 Bucket 排序** | 按权重分组优先搜索 |

### KataGomo 集成

- 通过 GTP（Go Text Protocol）协议与外部进程通信
- 支持进程预热，减少首次进入游戏等待时间
- 进程复用机制，避免重复创建进程

### KataGomo 模型导入教程

#### 环境要求

| 要求 | 说明 |
|------|------|
| **GPU** | NVIDIA GPU，支持 CUDA 12.x |
| **CUDA Toolkit** | 版本 12.0 或更高 |
| **操作系统** | Windows 10/11 64位 |

#### 下载引擎文件

1. 访问 KataGomo 官方发布页面：[https://github.com/hzyhhzy/KataGomo/releases](https://github.com/hzyhhzy/KataGomo/releases)

2. 下载适合您系统的版本：
   - 文件名格式：`kata-gomo-xxx-win64.zip`（xxx 为版本号）
   - 包含文件：`gom15x_trt.exe`、各种 CUDA DLL 文件

#### 目录结构设置

项目根目录下已包含 `engine` 文件夹，且已配置好 `gtp.cfg` 配置文件：

```
Gobang/
├── engine/                    # KataGomo 引擎目录（已配置）
│   ├── gom15x_trt.exe         # 15x15 五子棋推理引擎
│   ├── gtp.cfg                # GTP 协议配置文件（已配置）
│   ├── zhizi_renju28b_s1600.bin.gz  # 模型文件
│   ├── nvinfer_10.dll         # NVIDIA TensorRT DLL
│   ├── nvinfer_builder_resource_10.dll
│   ├── nvrtc64_120_0.dll
│   ├── nvJitLink_120_0.dll
│   ├── nvinfer_plugin_10.dll
│   ├── nvinfer_lean_10.dll
│   ├── nvonnxparser_10.dll
│   ├── nvjpeg64_12.dll
│   ├── nvrtc-builtins64_128.dll
│   └── msvcr110.dll           # Visual C++ 运行时
├── src/
├── resources/
└── out/
```

> **注意**：`engine` 目录已存在且包含 `gtp.cfg` 配置文件，无需手动创建。

#### CUDA 环境配置

1. 安装 NVIDIA 显卡驱动（版本 >= 530.00）
2. 安装 CUDA Toolkit 12.0 或更高版本
3. 确保以下环境变量已正确设置：

```powershell
# 设置 CUDA 路径（示例）
$env:PATH += ";C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.0\bin"
$env:PATH += ";C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.0\libnvvp"
```

#### 验证安装

1. 运行游戏程序
2. 在开始页面选择 **KataGomo** 作为 AI 引擎
3. 如果配置正确，引擎会自动初始化并预热
4. 如果出现错误，请检查：
   - 引擎目录是否存在且文件完整
   - CUDA 环境变量是否正确
   - GPU 是否支持 CUDA

> **注意**：首次启动 KataGomo 引擎可能需要等待几秒钟进行模型加载和预热。

## 📊 AI 对战测试

运行 `test.AIVsAITest` 可以进行 AI 对战测试：

- MiniMax（黑棋/先手） vs KataGomo（白棋）
- 搜索深度：8 层
- 默认进行 100 局对战
- 输出每局结果和胜率统计

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

1. 遵循 Java 代码规范
2. 使用 Javadoc 注释
3. 保持代码简洁清晰
4. 添加必要的单元测试

## 📄 许可证

MIT License

## 🙋‍♂️ 联系方式
qq邮箱:2037436957@qq.com
如有问题或建议，请提交 Issue。

---

*Enjoy playing Gobang! 🎮*
