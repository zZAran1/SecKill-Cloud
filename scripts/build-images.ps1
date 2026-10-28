# 版本化镜像构建：镜像打 git 短 SHA 作为版本号（无 git 环境用时间戳），并同时打 latest
# 用法：.\scripts\build-images.ps1            # 版本号 = git 短 SHA
#       .\scripts\build-images.ps1 -Tag v1.0  # 手动指定版本号
# 部署指定版本：$env:IMAGE_TAG='版本号'; docker compose up -d
param([string]$Tag)

$ErrorActionPreference = "Stop"

if (-not $Tag) {
    $Tag = (git rev-parse --short HEAD 2>$null)
    if (-not $Tag) { $Tag = Get-Date -Format yyyyMMddHHmmss }
}

$env:IMAGE_TAG = $Tag
Write-Output "==> IMAGE_TAG=$Tag，开始构建"
docker compose build

# 同时打 latest，保证不带 IMAGE_TAG 的 docker compose up 也能跑
$services = @("gateway", "user", "product", "order", "frontend")
foreach ($s in $services) {
    docker tag "seckill-${s}:$Tag" "seckill-${s}:latest"
}

Write-Output "==> 镜像清单："
docker images --format "{{.Repository}}:{{.Tag}}`t{{.Size}}" | Select-String "seckill"
Write-Output ""
Write-Output "部署指定版本：`$env:IMAGE_TAG='$Tag'; docker compose up -d"
