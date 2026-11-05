param(
    [Parameter(Mandatory = $true)]
    [string]$ResultDir
)

$ErrorActionPreference = 'Stop'
$resultPath = (Resolve-Path -LiteralPath $ResultDir).Path

function Get-Percentile {
    param([double[]]$SortedValues, [double]$Percentile)
    if ($SortedValues.Count -eq 0) { return 0 }
    $index = [Math]::Ceiling($Percentile * $SortedValues.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($index, $SortedValues.Count - 1))
    return $SortedValues[$index]
}

function Get-Median {
    param([double[]]$Values)
    [double[]]$sorted = $Values | Sort-Object
    if ($sorted.Count -eq 0) { return 0 }
    $middle = [Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 1) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}

$metrics = foreach ($file in Get-ChildItem -LiteralPath $resultPath -File -Filter '*.jtl') {
    if ($file.BaseName -notmatch '^(PERF-0[1-6]-run-\d+|PERF-09)$') { continue }
    $rows = @(Import-Csv -LiteralPath $file.FullName)
    if ($rows.Count -eq 0) { continue }

    [double[]]$elapsed = $rows | ForEach-Object { [double]$_.elapsed } | Sort-Object
    [double]$start = ($rows | Measure-Object -Property timeStamp -Minimum).Minimum
    [double]$end = ($rows | ForEach-Object { [double]$_.timeStamp + [double]$_.elapsed } | Measure-Object -Maximum).Maximum
    $durationSeconds = [Math]::Max(($end - $start) / 1000, 0.001)
    $errors = @($rows | Where-Object { $_.success -ne 'true' }).Count

    [pscustomobject]@{
        Run = $file.BaseName
        Scenario = if ($file.BaseName -match '^(PERF-\d{2})') { $Matches[1] } else { $file.BaseName }
        SampleCount = $rows.Count
        ThroughputRps = [Math]::Round($rows.Count / $durationSeconds, 3)
        ErrorPct = [Math]::Round($errors * 100 / $rows.Count, 3)
        P50Ms = Get-Percentile $elapsed 0.50
        P95Ms = Get-Percentile $elapsed 0.95
        P99Ms = Get-Percentile $elapsed 0.99
    }
}

$metrics = @($metrics | Sort-Object Scenario, Run)
if ($metrics.Count -eq 0) { throw "No completed formal JTL files found in $resultPath" }
$metrics | Export-Csv -LiteralPath (Join-Path $resultPath 'run-results.csv') -NoTypeInformation -Encoding UTF8

$summary = $metrics | Group-Object Scenario | ForEach-Object {
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
$summary | Export-Csv -LiteralPath (Join-Path $resultPath 'baseline-summary.csv') -NoTypeInformation -Encoding UTF8
$summary | Format-Table -AutoSize
Write-Host "Summary written to $resultPath"
