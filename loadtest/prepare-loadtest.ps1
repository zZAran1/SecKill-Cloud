# 压测数据准备：管理员建商品/秒杀活动并预热，批量注册压测账号生成 tokens.csv
# 用法：.\prepare-loadtest.ps1 -Gateway http://localhost:8080 -UserCount 200 -LeadSeconds 60
param(
    [string]$Gateway = "http://localhost:8080",
    [int]$UserCount = 200,
    # 活动开始前留出的准备时间（秒），须大于注册 $UserCount 个账号的耗时
    [int]$LeadSeconds = 60,
    [string]$OutDir = $PSScriptRoot
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body, [string]$Token)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    # 请求体必须按 UTF-8 字节发送，否则中文会被转成 ???（PowerShell 默认编码非 UTF-8）
    $bytes = $null
    if ($Body) { $bytes = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 5)) }
    return Invoke-RestMethod -Method $Method -Uri "$Gateway$Path" -Headers $headers `
        -ContentType "application/json; charset=utf-8" -Body $bytes
}

# 1. 管理员登录（种子账号 admin / Admin@123456）
$admin = Invoke-Api POST "/users/login" @{ phone = "13800000000"; password = "Admin@123456" }
if ($admin.code -ne 200) { throw "管理员登录失败: $($admin.msg)" }
$adminToken = $admin.data.token
Write-Output "[1/5] 管理员登录成功"

# 2. 建压测商品（创建接口不返回 id；列表按创建时间倒序，取第一条即刚创建的。
#    不按名称比对：PowerShell 解码中文响应会乱码，只信 id/数字字段）
$mark = "压测商品-$(Get-Date -Format yyyyMMddHHmmss)"
Invoke-Api POST "/products" @{ name = $mark; price = 99.00; stock = 100000; description = "压力测试专用商品" } $adminToken | Out-Null
$page = Invoke-Api GET "/products/page?status=1&page=1&size=10" $null $adminToken
$productId = $page.data.records[0].id
if (-not $productId) { throw "压测商品创建失败" }
Write-Output "[2/5] 压测商品已创建 productId=$productId"

# 3. 建秒杀活动（开始时间留出准备窗口，库存与账号数一致保证全部能抢到）
$startTime = (Get-Date).AddSeconds($LeadSeconds).ToString("yyyy-MM-ddTHH:mm:ss")
$endTime = (Get-Date).AddMinutes(30).ToString("yyyy-MM-ddTHH:mm:ss")
$activity = Invoke-Api POST "/seckill-activities" @{
    productId     = $productId
    seckillPrice  = 9.90
    stock         = $UserCount * 2
    startTime     = $startTime
    endTime       = $endTime
} $adminToken
if ($activity.code -ne 200) { throw "活动创建失败: $($activity.msg)" }
$activityId = $activity.data.id
Write-Output "[3/5] 秒杀活动已创建 activityId=$activityId，开始时间 $startTime"

# 4. 预热：活动信息与库存写入 Redis
Invoke-Api POST "/seckill-activities/$activityId/preload" $null $adminToken | Out-Null
Write-Output "[4/5] 活动预热完成"

# 5. 批量注册压测账号并登录，生成 tokens.csv
$tokens = New-Object System.Collections.Generic.List[string]
$tokens.Add("token")
for ($i = 1; $i -le $UserCount; $i++) {
    $phone = "139{0:D8}" -f $i
    $login = $null
    try {
        Invoke-Api POST "/users" @{
            email = "lt$i@load.test"; username = "lt$i"; password = "Abc12345"
            phone = $phone; age = 20; sex = "M"
        } | Out-Null
        $login = Invoke-Api POST "/users/login" @{ phone = $phone; password = "Abc12345" }
    } catch {
        # 账号已存在（重复执行）时直接登录
        $login = Invoke-Api POST "/users/login" @{ phone = $phone; password = "Abc12345" }
    }
    if ($login.code -ne 200) { throw "压测账号 $phone 登录失败: $($login.msg)" }
    $tokens.Add($login.data.token)
    if ($i % 50 -eq 0) { Write-Output "    已就绪 $i/$UserCount 个账号" }
}
$tokens | Set-Content (Join-Path $OutDir "tokens.csv") -Encoding UTF8
Write-Output "[5/5] 压测账号就绪 tokens.csv（$UserCount 个）"

# 输出活动参数供 run-loadtest.ps1 使用
@{
    activityId = $activityId
    startTime  = $startTime
    userCount  = $UserCount
} | ConvertTo-Json | Set-Content (Join-Path $OutDir "activity.json") -Encoding UTF8

Write-Output ""
Write-Output "准备完成。activityId=$activityId，请在 $startTime 后执行 .\run-loadtest.ps1"
