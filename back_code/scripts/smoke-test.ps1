# =====================================================================
# 水稻溯源后端 全流程冒烟测试脚本
# 运行前提：MySQL/Redis 已启动、sql/init.sql 已执行、后端已启动（模拟链即可）
# 用法：pwsh scripts/smoke-test.ps1
#       （可指定参数：-BaseUrl http://localhost:8080/api/v1 -Username admin -Password 123456）
# =====================================================================
param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [string]$Username = "admin",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"
$token = ""
$results = @()   # 记录每个步骤 名称/是否成功/说明

function Add-StepResult {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $script:results += [PSCustomObject]@{ 步骤 = $Name; 结果 = $(if ($Ok) { "PASS" } else { "FAIL" }); 说明 = $Detail }
    if ($Ok) { Write-Host "[PASS] $Name - $Detail" -ForegroundColor Green }
    else { Write-Host "[FAIL] $Name - $Detail" -ForegroundColor Red }
}

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [bool]$Public = $false)
    $headers = @{ "Content-Type" = "application/json" }
    if (-not $Public -and $token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $headers }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 10) }
    $resp = Invoke-RestMethod @params
    if ($resp.code -ne 0) { throw "API 错误 $Method $Path : code=$($resp.code) message=$($resp.message)" }
    return $resp.data
}

Write-Host "===== 水稻溯源冒烟测试 =====" -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl`n"

# ---------- 1. 登录 ----------
try {
    $login = Invoke-Api -Method "POST" -Path "/auth/login" -Public -Body @{ username = $Username; password = $Password }
    $token = $login.token
    Add-StepResult "登录" $true "token 获取成功, 角色=$($login.user.role)"
} catch {
    Add-StepResult "登录" $false $_.Exception.Message
    Write-Host "无法登录，终止测试" -ForegroundColor Red
    exit 1
}

# ---------- 2. 创建地块 ----------
$fieldId = $null
try {
    $suffix = Get-Date -Format "MMddHHmmss"
    $field = Invoke-Api -Method "POST" -Path "/rice/fields" -Body @{
        fieldCode      = "FIELD-SMOKE-$suffix"
        fieldName      = "冒烟测试田"
        farmerId       = "FARMER10001"
        province       = "黑龙江省"; city = "哈尔滨市"; district = "五常市"
        address        = "冒烟测试基地"
        areaMu         = 120.5
        gisBoundary    = @(
            @{ lng = 127.158001; lat = 44.931001 },
            @{ lng = 127.160112; lat = 44.931256 },
            @{ lng = 127.160025; lat = 44.929811 }
        )
        soilType       = "黑土"
    }
    $fieldId = $field.fieldId
    Add-StepResult "创建地块" $true "fieldId=$fieldId, coordinateHash=$($field.coordinateHash)"
} catch { Add-StepResult "创建地块" $false $_.Exception.Message }

# ---------- 3. 创建种植批次 ----------
$plantingBatchId = $null
try {
    $batch = Invoke-Api -Method "POST" -Path "/rice/planting-batches" -Body @{
        fieldId     = $fieldId
        riceVariety = "稻花香2号"
        seedSource  = "五常市某种业有限公司"
        seedBatchNo = "SEED-SMOKE-$suffix"
        sowingDate  = (Get-Date).ToString("yyyy-MM-dd")
        expectedHarvestDate = (Get-Date).AddMonths(5).ToString("yyyy-MM-dd")
        organicCertified = $true; greenCertified = $true
    }
    $plantingBatchId = $batch.plantingBatchId
    Add-StepResult "创建种植批次" $true "plantingBatchId=$plantingBatchId, status=$($batch.status)"
} catch { Add-StepResult "创建种植批次" $false $_.Exception.Message }

# ---------- 4. 创建入库单 ----------
$storageReceiptId = $null
$grainBatchId = "GRAIN-SMOKE-$suffix"
try {
    $receipt = Invoke-Api -Method "POST" -Path "/rice/storage-receipts" -Body @{
        plantingBatchId = $plantingBatchId
        grainBatchId    = $grainBatchId
        warehouseId     = "WH-SMOKE"
        warehouseCode   = "A-01-01"
        harvestAreaMu   = 120.5
        wetGrainWeightKg = 48200
        moisturePercent = 15.2
        impurityPercent = 0.8
        grainGrade      = "一级"
        storageTime     = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        temperature     = 18.5
        humidity        = 62.0
        keeperSignature = "sign:keeper-smoke"
        farmerSignature = "sign:farmer-smoke"
    }
    $storageReceiptId = $receipt.storageReceiptId
    Add-StepResult "创建入库单" $true "storageReceiptId=$storageReceiptId, grainBatchId=$grainBatchId"
} catch { Add-StepResult "创建入库单" $false $_.Exception.Message }

# ---------- 5. 提交入库质检（PASS） ----------
$qualityTestId = $null
try {
    $qt = Invoke-Api -Method "POST" -Path "/rice/storage-receipts/$storageReceiptId/quality-tests" -Body @{
        testAgency    = "五常市农产品质量检测中心"
        testTime      = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        testItems     = @(
            @{ itemName = "水分"; value = 15.2; unit = "%"; standardValue = "<=15.5%"; result = "PASS" },
            @{ itemName = "杂质"; value = 0.8; unit = "%"; standardValue = "<=1.0%"; result = "PASS" }
        )
        overallResult = "PASS"
        remark        = "冒烟测试质检"
    }
    $qualityTestId = $qt.qualityTestId
    Add-StepResult "提交入库质检" $true "qualityTestId=$qualityTestId, result=$($qt.overallResult)"
} catch { Add-StepResult "提交入库质检" $false $_.Exception.Message }

# ---------- 6. 创建成品米批次 ----------
$productBatchId = "PROD-SMOKE-$suffix"
try {
    $pb = Invoke-Api -Method "POST" -Path "/rice/product-batches" -Body @{
        productBatchId = $productBatchId
        productName    = "五常稻花香大米"
        riceVariety    = "稻花香2号"
        brandName      = "冒烟测试品牌"
        packageSpec    = "5kg/袋"
        standardNo     = "GB/T 19266"
        expectedSaleRegion = "黑龙江省,北京市"
    }
    Add-StepResult "创建成品米批次" $true "productBatchId=$($pb.productBatchId)"
} catch { Add-StepResult "创建成品米批次" $false $_.Exception.Message }

# ---------- 7. 创建加工批次 ----------
$millingBatchId = $null
try {
    $mb = Invoke-Api -Method "POST" -Path "/rice/milling-batches" -Body @{
        grainBatchId     = $grainBatchId
        productBatchId   = $productBatchId
        factoryId        = "FACTORY-SMOKE"
        grainOutWeightKg = 10000
        processStartTime = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        processParams    = @{
            hulling      = @{ machineNo = "HULL-01"; speed = "中速" }
            polishing    = @{ times = 2; intensity = "medium" }
            colorSorting = @{ machineNo = "SORT-02"; sensitivity = 85 }
            packaging    = @{ lineNo = "PACK-01"; spec = "5kg/袋" }
        }
    }
    $millingBatchId = $mb.millingBatchId
    Add-StepResult "创建加工批次" $true "millingBatchId=$millingBatchId"
} catch { Add-StepResult "创建加工批次" $false $_.Exception.Message }

# ---------- 8. 完成加工（产出率 68%，正常区间） ----------
try {
    $done = Invoke-Api -Method "PUT" -Path "/rice/milling-batches/$millingBatchId/complete" -Body @{
        riceOutputWeightKg = 6800
        processEndTime     = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        processParams      = @{ polishing = @{ times = 2; intensity = "medium" } }
        qualitySummary     = "出厂质检合格"
        reason             = "冒烟测试完成加工"
    }
    Add-StepResult "完成加工" $true "yieldRate=$($done.yieldRate)%"
} catch { Add-StepResult "完成加工" $false $_.Exception.Message }

# ---------- 9. 生成防伪码 ----------
$traceCodes = @()
try {
    $task = Invoke-Api -Method "POST" -Path "/rice/trace-codes/generate" -Body @{
        productBatchId = $productBatchId
        quantity       = 3
        packageSpec    = "5kg/袋"
        expectedSaleRegion = "黑龙江省"
    }
    Add-StepResult "生成防伪码" $true "generateTaskId=$($task.generateTaskId), quantity=$($task.quantity)"
    $codes = Invoke-Api -Method "GET" -Path "/rice/trace-codes?productBatchId=$productBatchId&pageSize=10"
    $traceCodes = @($codes.records | ForEach-Object { $_.traceCode })
} catch { Add-StepResult "生成防伪码" $false $_.Exception.Message }

# ---------- 10. 激活防伪码 ----------
try {
    $act = Invoke-Api -Method "POST" -Path "/rice/trace-codes/activate" -Body @{
        productBatchId = $productBatchId
        traceCodes     = @($traceCodes[0], $traceCodes[1])
        activatedBy    = "USER10002"
        reason         = "冒烟测试激活"
    }
    Add-StepResult "激活防伪码" $true "activatedCount=$($act.activatedCount), failedCount=$($act.failedCount)"
} catch { Add-StepResult "激活防伪码" $false $_.Exception.Message }

# ---------- 11. 窜货预警查询 ----------
try {
    $warnings = Invoke-Api -Method "GET" -Path "/rice/channel-warnings?pageSize=10"
    Add-StepResult "窜货预警查询" $true "共 $($warnings.total) 条（扫码事件由小程序触发，当前为空属正常）"
} catch { Add-StepResult "窜货预警查询" $false $_.Exception.Message }

# ---------- 12. 产量平衡校验（FULL_CHAIN，数据应 PASS） ----------
try {
    $ybc = Invoke-Api -Method "POST" -Path "/rice/regulation/yield-balance/check" -Body @{
        grainBatchId = $grainBatchId
        checkScope   = "FULL_CHAIN"
    }
    Add-StepResult "产量平衡校验" $true "checkId=$($ybc.checkId), result=$($ybc.result)"
} catch { Add-StepResult "产量平衡校验" $false $_.Exception.Message }

# ---------- 13. 风险预警 / 审计日志 ----------
try {
    $warns = Invoke-Api -Method "GET" -Path "/rice/regulation/risk-warnings?pageSize=10"
    Add-StepResult "风险预警查询" $true "共 $($warns.total) 条"
} catch { Add-StepResult "风险预警查询" $false $_.Exception.Message }

try {
    $logs = Invoke-Api -Method "GET" -Path "/rice/regulation/audit-logs?pageSize=10"
    Add-StepResult "审计日志查询" $true "共 $($logs.total) 条"
} catch { Add-StepResult "审计日志查询" $false $_.Exception.Message }

# ---------- 14. 链上存证查询 + 核验（等异步上链完成） ----------
Start-Sleep -Seconds 2
try {
    $proof = Invoke-Api -Method "GET" -Path "/rice/chain-proofs/STORAGE_RECEIPT/$storageReceiptId"
    Add-StepResult "链上存证查询" $true "chainStatus=$($proof.chainStatus), txId=$($proof.txId)"
} catch { Add-StepResult "链上存证查询" $false $_.Exception.Message }

try {
    $verify = Invoke-Api -Method "POST" -Path "/rice/chain-proofs/STORAGE_RECEIPT/$storageReceiptId/verify"
    Add-StepResult "链上核验" $true "verified=$($verify.verified), blockHeight=$($verify.blockHeight)"
} catch { Add-StepResult "链上核验" $false $_.Exception.Message }

# ---------- 15. 看板统计 ----------
try {
    $dash = Invoke-Api -Method "GET" -Path "/rice/dashboard/summary"
    Add-StepResult "看板统计" $true "地块=$($dash.fieldCount), 批次=$($dash.plantingBatchCount), 防伪码=$($dash.traceCodeCount), 上链成功率=$($dash.chainSuccessRate)%"
} catch { Add-StepResult "看板统计" $false $_.Exception.Message }

# ---------- 汇总 ----------
Write-Host "`n===== 测试汇总 =====" -ForegroundColor Cyan
$results | Format-Table -AutoSize
$failed = @($results | Where-Object { $_.结果 -eq "FAIL" }).Count
if ($failed -eq 0) {
    Write-Host "全部 $($results.Count) 项通过 ✅" -ForegroundColor Green
} else {
    Write-Host "通过 $($results.Count - $failed) 项, 失败 $failed 项 ❌" -ForegroundColor Red
}
