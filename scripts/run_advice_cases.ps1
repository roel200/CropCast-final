# Orchestrates the CropCast advice tests (run from run_advice_cases.cmd so you can watch).
#   1. gradle: all data unit tests; AdviceCasesTest writes one facts file per farm
#   2. each farm's facts go to the deployed AI worker (free OpenRouter / xKiro models)
#   3. claude-personal, claude-work and agy each judge every case, in parallel
# With no worker URL, step 2 is skipped and the reviewers judge the engine's ranking itself.
param(
    [string]$AdviceUrl = $(if ($env:CROPCAST_ADVICE_URL) { $env:CROPCAST_ADVICE_URL } else { (Select-String -Path "$PSScriptRoot\..\gradle.properties" -Pattern '^cropcast\.adviceUrl=(.+)$' | ForEach-Object { $_.Matches[0].Groups[1].Value }) }),
    [string]$Language = "English",
    [string]$ClaudeModel = "claude-sonnet-5-5",
    [string]$AgyModel = "claude-sonnet-5-5-high",
    [string]$Reviewers = "claude-personal,claude-work,agy"   # comma list; drop agy when its quota is out
)

$root = Resolve-Path "$PSScriptRoot\.."
$casesDir = "$root\app\build\advice-cases"
New-Item -ItemType Directory -Force $casesDir | Out-Null
Start-Transcript -Path "$casesDir\report.txt" -Force | Out-Null

Write-Host "`n=== 1/3 Engine unit tests ===" -ForegroundColor Cyan
Push-Location $root
& .\gradlew.bat :app:testDebugUnitTest --tests "com.cropcast.app.data.*" -q 2>&1 | Select-Object -Last 15
$gradleOk = $LASTEXITCODE -eq 0
Pop-Location
Write-Host ("gradle: " + $(if ($gradleOk) { "PASS" } else { "FAIL" })) -ForegroundColor $(if ($gradleOk) { "Green" } else { "Red" })

if (-not $AdviceUrl) {
    $AdviceUrl = Read-Host "Paste the deployed worker URL (https://cropcast-advice.<you>.workers.dev) or press Enter to skip the AI worker"
}

$cases = Get-ChildItem "$casesDir\*.txt" | Where-Object Name -ne "report.txt"
Write-Host "`n=== 2/3 AI worker advice for $($cases.Count) farms ===" -ForegroundColor Cyan
$work = foreach ($case in $cases) {
    $facts = (Get-Content $case.FullName -Raw).Trim()
    $advice = $null; $note = "no worker"
    if ($AdviceUrl) {
        try {
            $body = @{ facts = $facts; language = $Language } | ConvertTo-Json
            $watch = [Diagnostics.Stopwatch]::StartNew()
            $advice = (Invoke-RestMethod -Uri $AdviceUrl -Method Post -ContentType "application/json" -Body $body -TimeoutSec 60).text
            $words = ($advice -split '\s+' | Where-Object { $_ }).Count
            $markdown = $advice -match '[*#`]|^\s*-\s'
            $note = "{0:N1}s, {1} words{2}" -f $watch.Elapsed.TotalSeconds, $words, $(if ($markdown) { ", MARKDOWN" } else { "" })
            if ($words -gt 170 -or $markdown) { $note += " <- rule broken" }
        } catch { $note = "worker error: $($_.Exception.Message)" }
    }
    Write-Host ("{0,-28} {1}" -f $case.BaseName, $note)
    [pscustomobject]@{ Name = $case.BaseName; Facts = $facts; Advice = $advice; Note = $note }
}

Write-Host "`n=== 3/3 Reviewers: $Reviewers ($ClaudeModel / agy $AgyModel), effort high ===" -ForegroundColor Cyan
$reviewers = $Reviewers -split ","
$jobs = foreach ($item in $work) { foreach ($reviewer in $reviewers) { [pscustomobject]@{ Item = $item; Reviewer = $reviewer } } }
$verdicts = $jobs | ForEach-Object -ThrottleLimit 6 -Parallel {
    $item = $_.Item; $reviewer = $_.Reviewer; $model = $using:ClaudeModel; $agyModel = $using:AgyModel
    $facts = $item.Facts -replace '"', "'"
    if ($item.Advice) {
        $task = "ADVICE (written by a small AI model for the farmer):`n$($item.Advice -replace '"', "'")`n`nCheck: 1) names only crops that appear in FACTS, 2) every number comes from FACTS, 3) at most 150 words, 4) no markdown, 5) says what to plant and the top things to do. Do not use any tools."
    } else {
        $task = "Judge only the ranking in FACTS as a Philippine agronomist: is each crop plausible for the stated soil pH, rainfall and season? Do not use any tools."
    }
    $prompt = "You are a strict reviewer for a Philippine smallholder crop app.`nFACTS (ground truth from the app engine):`n$facts`n`n$task`nReply with PASS or FAIL as the first word, then one short reason."
    $out = if ($reviewer -eq "agy") { & agy --model $agyModel --effort high --print-timeout 180s -p $prompt 2>&1 | Out-String }
           else { $prompt | & "$reviewer.cmd" -p --model $model --effort high 2>&1 | Out-String }
    $verdict = if ($out -match '\b(PASS|FAIL)\b') { $Matches[1] } else { "NO ANSWER" }
    $reason = (($out -split "`n" | Where-Object { $_.Trim() } | Select-Object -First 3) -join " ").Trim()
    if ($reason.Length -gt 160) { $reason = $reason.Substring(0, 160) + "..." }
    [pscustomobject]@{ Case = $item.Name; Reviewer = $reviewer; Verdict = $verdict; Reason = $reason }
}

foreach ($group in $verdicts | Group-Object Case) {
    Write-Host "`n$($group.Name)" -ForegroundColor Yellow
    foreach ($v in $group.Group | Sort-Object Reviewer) {
        $color = switch ($v.Verdict) { "PASS" { "Green" } "FAIL" { "Red" } default { "DarkYellow" } }
        Write-Host ("  {0,-16} {1,-9} {2}" -f $v.Reviewer, $v.Verdict, $v.Reason) -ForegroundColor $color
    }
}
$fails = @($verdicts | Where-Object Verdict -ne "PASS").Count
Write-Host ("`nSUMMARY: engine tests {0}; {1}/{2} reviewer verdicts PASS; report: {3}" -f $(if ($gradleOk) { "PASS" } else { "FAIL" }), ($verdicts.Count - $fails), $verdicts.Count, "$casesDir\report.txt") -ForegroundColor Cyan
$verdicts | Export-Csv "$casesDir\verdicts.csv" -NoTypeInformation
Stop-Transcript | Out-Null
