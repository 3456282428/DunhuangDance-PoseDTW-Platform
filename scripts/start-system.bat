@echo off
rem ============================================================
rem 舞韵敦煌 —— 一键启动（后端8080 + 前端5173 + 评分服务8000）
rem 本脚本必须放在 01_可运行程序\scripts 目录下运行
rem 首次运行评分服务前，请先按《运行或使用说明》安装 Python 依赖
rem ============================================================
chcp 65001 >nul
setlocal

set "ROOT=%~dp0.."

if not exist "%ROOT%\backend\target\art-training-backend-0.0.1-SNAPSHOT.jar" (
    echo [错误] 未找到后端可执行 jar，请确认目录完整，或在 backend 目录执行 mvn package。
    pause
    exit /b 1
)

echo [1/3] 启动后端 Spring Boot (8080) ...
start "art-backend" /d "%ROOT%\backend" cmd /k "java -jar target\art-training-backend-0.0.1-SNAPSHOT.jar"

echo [2/3] 启动前端 Vite (5173) ...
if exist "%ROOT%\frontend\node_modules" (
    start "art-frontend" /d "%ROOT%\frontend" cmd /k "npm.cmd run dev"
) else (
    start "art-frontend" /d "%ROOT%\frontend" cmd /k "npm.cmd install && npm.cmd run dev"
)

echo [3/3] 启动 Python 评分服务 (8000) ...
start "dance-ai" /d "%ROOT%\dance" cmd /k "set DANCE_API_HOST=127.0.0.1&& python api_server.py"

echo.
echo 三个命令行窗口请保持打开，关闭窗口即停止对应服务。
echo 前端访问: http://localhost:5173/
echo 管理员 admin / 教师 T001 / 学员 20240102，密码均为 123456
echo 评分服务依赖 conda 环境，若窗口提示缺少模块，请先 conda activate dance。
echo 结束系统请运行: scripts\stop-system.bat
pause
