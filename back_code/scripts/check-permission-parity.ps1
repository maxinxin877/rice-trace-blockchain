<#
权限一致性校验（前后端 + 菜单 + 权限表）
用法：pwsh back_code/scripts/check-permission-parity.ps1 [-RepoRoot <仓库根目录>]

校验三件事：
  1. 后端 PermissionConstants.ROLE_PERMISSIONS 与前端 stores/user.ts 的 ROLE_PERMISSIONS 完全一致
  2. 侧边菜单（layouts/MainLayout.vue）可见角色都持有对应页面的查看权限
  3. 路由（router/index.ts）meta.roles 可见角色都持有对应页面的查看权限
全部通过退出码 0，否则退出码 1 并打印差异。
#>
param(
    [string]$RepoRoot = 'D:\develop\githubdesktop\rice-trace-blockchain'
)

$ErrorActionPreference = 'Stop'
$backFile = Join-Path $RepoRoot 'back_code\src\main\java\com\itheima\qukuailian\utils\PermissionConstants.java'
$userFile = Join-Path $RepoRoot 'front_code\src\stores\user.ts'
$menuFile = Join-Path $RepoRoot 'front_code\src\layouts\MainLayout.vue'
$routerFile = Join-Path $RepoRoot 'front_code\src\router\index.ts'

foreach ($f in @($backFile, $userFile, $menuFile, $routerFile)) {
    if (-not (Test-Path $f)) { throw "文件不存在: $f" }
}

# ---------- 解析 ----------
$beText = [System.IO.File]::ReadAllText($backFile, [System.Text.Encoding]::UTF8)
$be = @{}
foreach ($m in [regex]::Matches($beText, 'Map\.entry\("(?<role>[A-Z_]+)",\s*Set\.of\((?<codes>.*?)\)\)', 'Singleline')) {
    $be[$m.Groups['role'].Value] = @(([regex]::Matches($m.Groups['codes'].Value, 'RICE_[A-Z_]+') | ForEach-Object { $_.Value }) | Sort-Object -Unique)
}

$feText = [System.IO.File]::ReadAllText($userFile, [System.Text.Encoding]::UTF8)
$block = [regex]::Match($feText, 'const ROLE_PERMISSIONS[^{]*\{(?<body>.*?)\n\}', 'Singleline').Groups['body'].Value
$fe = @{}
foreach ($m in [regex]::Matches($block, '(?<role>[A-Z_]+):\s*\[(?<codes>.*?)\]', 'Singleline')) {
    $fe[$m.Groups['role'].Value] = @(([regex]::Matches($m.Groups['codes'].Value, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }) | Sort-Object -Unique)
}

# 后端角色名 -> 前端角色名
$roleMap = @{
    'RICE_ADMIN' = 'RICE_ADMIN'; 'FARMER' = 'FARMER'; 'WAREHOUSE_KEEPER' = 'WAREHOUSE'
    'PROCESSING_FACTORY' = 'FACTORY'; 'BRAND_OPERATOR' = 'BRAND'; 'REGULATOR' = 'REGULATOR'
}

# 页面 -> 所需查看权限
$need = @{
    '/rice/dashboard'                = 'RICE_DASHBOARD_VIEW'
    '/rice/fields'                   = 'RICE_FIELD_VIEW'
    '/rice/planting-batches'         = 'RICE_PLANTING_BATCH_VIEW'
    '/rice/farming-logs'             = 'RICE_FARMING_LOG_VIEW'
    '/rice/environment-records'      = 'RICE_ENV_RECORD_VIEW'
    '/rice/storage-receipts'         = 'RICE_STORAGE_VIEW'
    '/rice/milling-batches'          = 'RICE_MILLING_VIEW'
    '/rice/product-batches'          = 'RICE_PRODUCT_BATCH_VIEW'
    '/rice/trace-codes'              = 'RICE_TRACE_CODE_VIEW'
    '/rice/channel-warnings'         = 'RICE_CHANNEL_WARNING_VIEW'
    '/rice/regulation/risk-warnings' = 'RICE_REGULATION_VIEW'
    '/rice/regulation/audit-logs'    = 'RICE_AUDIT_VIEW'
    '/rice/regulation/yield-balance' = 'RICE_REGULATION_VIEW'
    '/rice/regulation/chain-proofs'  = 'RICE_CHAIN_PROOF_VIEW'
}

$problems = @()

# ---------- 校验 1：两张权限表一致 ----------
Write-Host '--- 1. 后端 vs 前端 权限表 ---' -ForegroundColor Cyan
foreach ($beRole in ($be.Keys | Sort-Object)) {
    $feRole = $roleMap[$beRole]
    if (-not $feRole) { $problems += "后端角色 $beRole 未在前端角色映射中登记"; continue }
    if (-not $fe.ContainsKey($feRole)) { $problems += "前端缺少角色 $feRole"; continue }
    $b = $be[$beRole]; $f = $fe[$feRole]
    $onlyBe = @($b | Where-Object { $f -notcontains $_ })
    $onlyFe = @($f | Where-Object { $b -notcontains $_ })
    if ($onlyBe.Count -eq 0 -and $onlyFe.Count -eq 0) {
        Write-Host ("  [OK]   {0,-18} {1,2} 项一致" -f $beRole, $b.Count) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0,-18} 后端多=[{1}] 前端多=[{2}]" -f $beRole, ($onlyBe -join ', '), ($onlyFe -join ', ')) -ForegroundColor Red
        $problems += "$beRole 权限不一致：后端多=[$($onlyBe -join ', ')] 前端多=[$($onlyFe -join ', ')]"
    }
}

# ---------- 校验 2 / 3：菜单、路由 与权限表 ----------
function Check-Menu([string]$title, [string]$file, [string]$pathPattern, [string]$rolesPattern) {
    Write-Host "--- $title ---" -ForegroundColor Cyan
    $text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
    $bad = 0
    foreach ($m in [regex]::Matches($text, $pathPattern, 'Singleline')) {
        $path = $m.Groups['path'].Value
        if (-not $need.ContainsKey($path)) { continue }
        $perm = $need[$path]
        $roles = @()
        $rm = [regex]::Match($m.Value, $rolesPattern)
        if ($rm.Success) {
            $roles = @([regex]::Matches($rm.Groups['r'].Value, "'([^']+)'") | ForEach-Object { $_.Groups[1].Value })
        } else {
            $roles = @($fe.Keys)   # 未声明 roles = 所有角色可见
        }
        foreach ($role in $roles) {
            if (-not ($fe[$role] -contains $perm)) {
                $bad++
                Write-Host ("  [FAIL] {0} 对 {1} 可见，但该角色缺少 {2}" -f $path, $role, $perm) -ForegroundColor Red
                $problems += "${title}: $path 对 $role 可见但缺 $perm"
            }
        }
    }
    if ($bad -eq 0) { Write-Host '  [OK]   全部菜单项与其可见角色的权限一致' -ForegroundColor Green }
}

Check-Menu '2. 侧边菜单 MainLayout.vue' $menuFile "path:\s*'(?<path>[^']+)',[^\r\n]*roles:\s*\[(?<r>[^\]]*)\]" "roles:\s*\[(?<r>[^\]]*)\]"
Check-Menu '3. 路由 router/index.ts' $routerFile "path:\s*'(?<path>[^']+)',(?<blk>.*?)(?=\n  \{|\n\])" "roles:\s*\[(?<r>[^\]]*)\]"

Write-Host ''
if ($problems.Count -eq 0) {
    Write-Host '✅ 权限完全一致：前后端权限表相同，菜单/路由可见角色均具备对应权限' -ForegroundColor Green
    exit 0
} else {
    Write-Host "❌ 发现 $($problems.Count) 处不一致：" -ForegroundColor Red
    $problems | ForEach-Object { Write-Host "   - $_" -ForegroundColor Red }
    exit 1
}
