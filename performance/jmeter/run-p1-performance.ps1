param(
    [Parameter(Mandatory = $true)]
    [long]$ArticleId,
    [string]$HostName = '127.0.0.1',
    [int]$Port = 8080,
    [ValidateSet('http', 'https')]
    [string]$Protocol = 'http',
    [string]$JMeterBin = 'jmeter',
    [string]$Username = 'perf_user_001',
    [string]$Password = '123456',
    [int]$Threads = 10,
    [int]$WriteThreads = 10,
    [int]$RampSeconds = 30,
    [int]$WarmupSeconds = 120,
    [int]$RunSeconds = 300,
    [int]$FormalRuns = 3,
    [int]$StabilitySeconds = 1800,
    [ValidateSet('PERF-01', 'PERF-02', 'PERF-03', 'PERF-04', 'PERF-05', 'PERF-06', 'PERF-07', 'PERF-08', 'PERF-09')]
    [string[]]$Scenarios = @('PERF-01', 'PERF-02', 'PERF-03', 'PERF-04', 'PERF-05', 'PERF-06', 'PERF-07', 'PERF-08', 'PERF-09'),
    [string]$OutRoot = (Join-Path $HOME 'blog-p1-acceptance\performance')
)

$ErrorActionPreference = 'Stop'
$scriptDir = $PSScriptRoot
$runId = Get-Date -Format 'yyyyMMdd_HHmmss'
$outDir = Join-Path $OutRoot $runId
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

if (-not (Get-Command $JMeterBin -ErrorAction SilentlyContinue)) {
    throw "JMeter command not found: $JMeterBin"
}

$publicJmx = Join-Path $scriptDir 'blog-p1-public-read.jmx'
$writeJmx = Join-Path $scriptDir 'blog-p1-write-limit.jmx'
$stabilityJmx = Join-Path $scriptDir 'blog-p1-stability.jmx'
foreach ($file in @($publicJmx, $writeJmx, $stabilityJmx)) {
    if (-not (Test-Path -LiteralPath $file)) { throw "Missing JMX: $file" }
}

function Invoke-JMeterRun {
    param(
        [string]$Jmx,
        [string]$Name,
        [string[]]$Properties,
        [switch]$AllowExpectedHttpErrors
    )

    $jtl = Join-Path $outDir "$Name.jtl"
    $report = Join-Path $outDir "$Name-report"
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Starting $Name"
    $arguments = @(
        '-n',
        '-t', $Jmx,
        "-Jhost=$HostName",
        "-Jport=$Port",
        "-Jprotocol=$Protocol"
    ) + $Properties + @(
        '-l', $jtl,
        '-e',
        '-o', $report
    )
    & $JMeterBin @arguments
    if ($LASTEXITCODE -ne 0) { throw "JMeter run failed: $Name (exit code $LASTEXITCODE)" }

    if (-not $AllowExpectedHttpErrors) {
        $failedSamples = @(Import-Csv -LiteralPath $jtl | Where-Object { $_.success -ne 'true' }).Count
        if ($failedSamples -gt 0) {
            throw "JMeter run contains $failedSamples failed samples: $Name. Check $jtl before continuing."
        }
    }
}

function Assert-WriteLimitResult {
    param([string]$Scenario)

    $jtl = Join-Path $outDir "$Scenario.jtl"
    $writeLabel = if ($Scenario -eq 'PERF-07') { 'PERF-07 Concurrent Comment' } else { 'PERF-08 Concurrent Message' }
    $rows = @(Import-Csv -LiteralPath $jtl | Where-Object { $_.label -eq $writeLabel })
    $successCount = @($rows | Where-Object { $_.responseCode -eq '200' -and $_.responseMessage -eq 'EXPECTED_WRITE_SUCCESS' }).Count
    $expectedRateCode = if ($Scenario -eq 'PERF-07') { 'EXPECTED_RATE_LIMIT_106004' } else { 'EXPECTED_RATE_LIMIT_108004' }
    $rateLimitedCount = @($rows | Where-Object { $_.responseCode -eq '429' -and $_.responseMessage -eq $expectedRateCode }).Count
    $unexpectedCount = $rows.Count - $successCount - $rateLimitedCount

    if ($rows.Count -ne $WriteThreads -or $successCount -ne 1 -or $rateLimitedCount -ne ($WriteThreads - 1) -or $unexpectedCount -ne 0) {
        throw "$Scenario expected 1 HTTP 200 and $($WriteThreads - 1) HTTP 429 responses; actual total=$($rows.Count), success=$successCount, rateLimited=$rateLimitedCount, unexpected=$unexpectedCount. Check $jtl."
    }
    Write-Host "$Scenario passed: HTTP 200=$successCount, HTTP 429=$rateLimitedCount"
}

function Get-Percentile {
    param([double[]]$SortedValues, [double]$Percentile)
    if ($SortedValues.Count -eq 0) { return 0 }
    $index = [Math]::Ceiling($Percentile * $SortedValues.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($index, $SortedValues.Count - 1))
    return $SortedValues[$index]
}

function Get-JtlMetrics {
    param([string]$Name)
    $jtl = Join-Path $outDir "$Name.jtl"
    $rows = @(Import-Csv -LiteralPath $jtl)
    if ($rows.Count -eq 0) { throw "JTL contains no samples: $jtl" }

    [double[]]$elapsed = $rows | ForEach-Object { [double]$_.elapsed } | Sort-Object
    [double]$start = ($rows | Measure-Object -Property timeStamp -Minimum).Minimum
    [double]$end = ($rows | ForEach-Object { [double]$_.timeStamp + [double]$_.elapsed } | Measure-Object -Maximum).Maximum
    $durationSeconds = [Math]::Max(($end - $start) / 1000, 0.001)
    $errors = @($rows | Where-Object { $_.success -ne 'true' }).Count

    return [pscustomobject]@{
        Run = $Name
        Scenario = if ($Name -match '^(PERF-\d{2})') { $Matches[1] } else { $Name }
        SampleCount = $rows.Count
        ThroughputRps = [Math]::Round($rows.Count / $durationSeconds, 3)
        ErrorPct = [Math]::Round($errors * 100 / $rows.Count, 3)
        P50Ms = Get-Percentile $elapsed 0.50
        P95Ms = Get-Percentile $elapsed 0.95
        P99Ms = Get-Percentile $elapsed 0.99
    }
}

function Get-Median {
    param([double[]]$Values)
    [double[]]$sorted = $Values | Sort-Object
    if ($sorted.Count -eq 0) { return 0 }
    $middle = [Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 1) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}

$runMetrics = @()

foreach ($scenario in @('PERF-01', 'PERF-02', 'PERF-03', 'PERF-04', 'PERF-05', 'PERF-06') | Where-Object { $_ -in $Scenarios }) {
    $throughput = if ($scenario -in @('PERF-01', 'PERF-02')) { 600 } elseif ($scenario -in @('PERF-03', 'PERF-04', 'PERF-05')) { 300 } else { 900 }
    $common = @(
        "-Jscenario=$scenario",
        "-JarticleId=$ArticleId",
        "-Jthreads=$Threads",
        "-JrampSeconds=$RampSeconds",
        "-JthroughputPerMinute=$throughput"
    )
    Invoke-JMeterRun $publicJmx "$scenario-warmup" ($common + "-JdurationSeconds=$WarmupSeconds")
    for ($run = 1; $run -le $FormalRuns; $run++) {
        $runName = "$scenario-run-$run"
        Invoke-JMeterRun $publicJmx $runName ($common + "-JdurationSeconds=$RunSeconds")
        $runMetrics += Get-JtlMetrics $runName
    }
}

if ($runMetrics.Count -gt 0) {
    & (Join-Path $scriptDir 'summarize-p1-results.ps1') -ResultDir $outDir
}

foreach ($scenario in @('PERF-07', 'PERF-08') | Where-Object { $_ -in $Scenarios }) {
    Invoke-JMeterRun $writeJmx $scenario @(
        "-Jscenario=$scenario",
        "-JarticleId=$ArticleId",
        "-Jusername=$Username",
        "-Jpassword=$Password",
        "-JwriteThreads=$WriteThreads"
    ) -AllowExpectedHttpErrors
    Assert-WriteLimitResult $scenario
}

if ('PERF-09' -in $Scenarios) {
    Invoke-JMeterRun $stabilityJmx 'PERF-09' @(
        "-JarticleId=$ArticleId",
        "-Jthreads=$Threads",
        "-JrampSeconds=$RampSeconds",
        "-JdurationSeconds=$StabilitySeconds",
        '-JthroughputPerMinute=600'
    )
    $runMetrics += Get-JtlMetrics 'PERF-09'
}

$runMetrics | Export-Csv -LiteralPath (Join-Path $outDir 'run-results.csv') -NoTypeInformation -Encoding UTF8
$baselineSummary = $runMetrics |
    Group-Object Scenario |
    ForEach-Object {
        $group = @($_.Group)
        [pscustomobject]@{
            Scenario = $_.Name
            FormalRuns = $group.Count
            SampleCount = [Math]::Round((Get-Median ([double[]]$group.SampleCount)), 0)
            ThroughputRps = [Math]::Round((Get-Median ([double[]]$group.ThroughputRps)), 3)
            ErrorPct = [Math]::Round((Get-Median ([double[]]$group.ErrorPct)), 3)
            P50Ms = [Math]::Round((Get-Median ([double[]]$group.P50Ms)), 3)
            P95Ms = [Math]::Round((Get-Median ([double[]]$group.P95Ms)), 3)
            P99Ms = [Math]::Round((Get-Median ([double[]]$group.P99Ms)), 3)
        }
    }
$baselineSummary | Export-Csv -LiteralPath (Join-Path $outDir 'baseline-summary.csv') -NoTypeInformation -Encoding UTF8

Write-Host "P1 performance suite completed: $outDir"
