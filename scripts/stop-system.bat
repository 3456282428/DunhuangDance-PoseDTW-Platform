@echo off
rem ============================================================
rem 舞韵敦煌 —— 停止本机服务（8080 后端 / 5173 前端 / 8000 评分）
rem ============================================================
chcp 65001 >nul

echo [1/3] 停止后端 (8080) ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo [2/3] 停止前端 (5173) ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173 " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo [3/3] 停止评分服务 (8000) ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8000 " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo.
echo 本机服务已全部结束。
pause
