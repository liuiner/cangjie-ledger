# 仓颉记账

面向单用户的本地优先安卓记账应用。数据保存在设备本地，不需要账号或服务器。

## 已实现功能

- 金额算式输入，支持加减乘除和括号
- 支出/收入、账本、分类、日期、备注和多标签
- 账单编辑、删除、按月汇总、按日分组和类型/标签筛选
- 多账本创建、重命名、归档、恢复和默认账本
- 备注关键词、分类、日期范围、标签组合搜索
- 本月分类占比、近 6 个月收支趋势和分类支出排行
- 分类月度预算、剩余进度和超支红色提醒
- 每月周期记账规则、启停和幂等自动生成
- SAF 目录自动备份、启动补备份和手动备份
- CSV 导出、重复检测导入和坏行跳过
- 浅色/深色/跟随系统、动态取色
- 桌面组件显示当日支出和本月结余，点击直达记账

## 技术栈

- Kotlin 2.0
- Jetpack Compose + Material 3
- Room + DataStore
- WorkManager
- Coroutines + Flow
- 单 Activity + Navigation Compose

金额统一以整数“分”存储，表达式使用 `BigDecimal` 解析。周期账单使用规则周期唯一键去重。

## 构建

环境要求：JDK 17、Android SDK 35。

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

正式版签名配置保存在本机 `keystore.properties` 和 `keystore/`，两者已被 Git 忽略。丢失签名密钥后将无法覆盖安装后续版本。

当前安装包：`dist/CangJie-1.0.0.apk`

## 公开分发注意

- 不要提交 `keystore/`、`keystore.properties`、`local.properties`、`.tools/`、构建目录或测试截图。
- 建议通过 Gitee 的发行版功能附加 APK，不要把安装包提交到源码历史中。
- 如果没有附加开源许可证，源码公开不代表他人拥有修改和再分发授权。

## 安全说明

- 应用未申请 `INTERNET` 权限，不包含账号、统计 SDK 或远程接口。
- 账目保存在应用私有目录中，系统云备份和设备迁移备份已关闭。
- 本地备份 JSON 和导出的 CSV 都是明文文件。放入网盘目录时，拥有该网盘或目录权限的人可能读取账目。
- 应用目前没有独立 PIN、数据库加密或加密备份。手机被解锁、Root 或备份目录共享时，应把账目视为可被读取。
