# 远程服务器部署与控制

## 服务器信息

| 项 | 值 |
|---|---|
| 公网 IP | `120.76.241.77` |
| 系统 | Ubuntu 24.04 LTS（x86_64，2 核） |
| 访问地址 | http://120.76.241.77:8080 |
| SSH | `root@120.76.241.77`，本机私钥 `~/.ssh/campus_deploy` |
| 应用目录 | `/opt/campus-secondhand` |
| 启动方式 | systemd：`campus-secondhand.service`（已启用开机自启，`Restart=always`） |
| 运行用户 | `campusapp`（非 root） |
| 数据库 | 本机 MySQL 8.0.46，库 `campus_secondhand`，应用账号 `campus@localhost` |
| 数据库密码 | 只保存在服务器 `/opt/campus-secondhand/app.env`（权限 600，已 gitignore 不涉及） |

> 8080 已在云服务器安全组放行；如果以后换端口，需要同时改 systemd 的 `--server.port` 和安全组。

## 本机一键控制脚本

双击 `.bat`，或在 PowerShell 执行 `.ps1`：

| 脚本 | 作用 |
|---|---|
| [`远程启动.bat`](远程启动.bat) / [`remote-start.ps1`](remote-start.ps1) | 启动 systemd 服务，等待健康检查通过，然后打开浏览器 |
| [`远程停止.bat`](远程停止.bat) / [`remote-stop.ps1`](remote-stop.ps1) | 停止服务（保留应用与数据库，可随时再启动） |
| [`远程状态.bat`](远程状态.bat) / [`remote-status.ps1`](remote-status.ps1) | 查看 systemd 状态、端口和 HTTP 健康检查 |
| [`远程卸载.bat`](远程卸载.bat) / [`remote-uninstall.ps1`](remote-uninstall.ps1) | 停止 + 取消开机自启 + 删除 unit 和应用目录；默认保留数据库 |

脚本依赖：

- 本机 Windows 自带的 `ssh`；
- 部署私钥 `C:\Users\<你>\.ssh\campus_deploy`（部署时生成，勿泄露、勿提交）；
- 服务器 root 已授权该公钥，无需输入密码。

常用参数：

```powershell
.\deploy\remote-start.ps1 -NoBrowser     # 启动但不自动开浏览器
.\deploy\remote-uninstall.ps1 -DryRun    # 只打印卸载命令，不执行
.\deploy\remote-uninstall.ps1 -Force     # 跳过 UNINSTALL 输入确认（谨慎）
```

## 查看日志

```powershell
ssh -i "$env:USERPROFILE\.ssh\campus_deploy" root@120.76.241.77 "journalctl -u campus-secondhand -f"
```

## 更新线上版本

1. 本地重新构建带前端的 Jar：

   ```powershell
   .\start.ps1 -Rebuild
   ```

2. 上传并重启：

   ```powershell
   scp -i "$env:USERPROFILE\.ssh\campus_deploy" backend\target\campus-secondhand-backend-1.0.0.jar root@120.76.241.77:/opt/campus-secondhand/app.jar
   ssh -i "$env:USERPROFILE\.ssh\campus_deploy" root@120.76.241.77 "systemctl restart campus-secondhand"
   ```

3. 检查：`.\deploy\remote-start.ps1 -NoBrowser` 或 `curl http://120.76.241.77:8080/api/items`。

## 卸载说明

`远程卸载` 会：

- 停止并禁用 `campus-secondhand.service`；
- 删除 `/etc/systemd/system/campus-secondhand.service`；
- 删除 `/opt/campus-secondhand`（Jar、SQL、app.env）；
- 可选：删除数据库 `campus_secondhand` 和数据库用户 `campus`（默认保留）；
- **不会**卸载系统 JDK、MySQL，避免影响服务器上其他项目。

## 安全提醒

- `campus_deploy` 私钥权限建议只保留当前 Windows 用户可读；不要上传到仓库或群文件。
- 8080 对公网开放仅用于课程验收；演示结束后可在安全组收紧到本机 IP。
- 若要换数据库密码：修改服务器 `/opt/campus-secondhand/app.env` 的 `DB_PASSWORD`，同步改 MySQL 用户密码，再 `systemctl restart campus-secondhand`。
