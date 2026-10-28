# 秒杀接口压测执行：JMeter 容器（justb4/jmeter）跑 seckill-load.jmx 并生成 HTML 报告
# 前置：已执行 prepare-loadtest.ps1 生成 tokens.csv / activity.json，Docker 与网关(8080)在运行
# 用法：.\run-loadtest.ps1 -Threads 200 -Ramp 5 -Loops 1
param(
    [int]$Threads = 200,
    [int]$Ramp = 5,
    [int]$Loops = 1,
    [string]$Host_ = "host.docker.internal",
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

$activityFile = Join-Path $PSScriptRoot "activity.json"
if (-not (Test-Path $activityFile)) { throw "缺少 activity.json，请先执行 .\prepare-loadtest.ps1" }
$activity = (Get-Content $activityFile -Raw | ConvertFrom-Json).activityId

# JMeter 要求 -l 结果文件与 -o 报告目录为空，先清掉上一次的产物
if (Test-Path (Join-Path $PSScriptRoot "results.jtl")) { Remove-Item (Join-Path $PSScriptRoot "results.jtl") -Force }
if (Test-Path (Join-Path $PSScriptRoot "report")) { Remove-Item (Join-Path $PSScriptRoot "report") -Recurse -Force }

# JMeter 容器访问宿主机上的网关用 host.docker.internal（Docker Desktop 内置）
# 用数组传参：避免行尾续行符/变量展开被 PowerShell 原生命令调用破坏
$jmeterArgs = @(
    "run", "--rm",
    "-v", "${PSScriptRoot}:/loadtest",
    "justb4/jmeter",
    "-n", "-t", "/loadtest/seckill-load.jmx",
    "-l", "/loadtest/results.jtl",
    "-e", "-o", "/loadtest/report",
    "-Jhost=$Host_",
    "-Jport=$Port",
    "-Jactivity=$activity",
    "-Jthreads=$Threads",
    "-Jramp=$Ramp",
    "-Jloops=$Loops",
    "-JtokenFile=/loadtest/tokens.csv"
)
& docker @jmeterArgs

Write-Output ""
Write-Output "压测完成，HTML 报告：$PSScriptRoot\report\index.html"
