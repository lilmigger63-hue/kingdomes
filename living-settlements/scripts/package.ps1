$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$versionLine = Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1
$version = ($versionLine -split '=', 2)[1].Trim()
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid addon version in gradle.properties.' }
$addonName = "living-settlements-$version.jar"
$addonPath = Join-Path $projectRoot "build/libs/$addonName"
if (-not (Test-Path -LiteralPath $addonPath)) { throw 'Run scripts/build.ps1 first.' }
$distributionRoot = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Force -Path $distributionRoot | Out-Null
Copy-Item -LiteralPath $addonPath -Destination (Join-Path $distributionRoot $addonName) -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'defaults/living-settlements-server.toml') -Destination (Join-Path $distributionRoot 'living-settlements-server.toml') -Force

# Stage a new copy; never edit the original imported modpack or a player profile.
$stageRoot = Join-Path $projectRoot ('build/packaging/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'modpack') -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $stageRoot -Recurse
}
$packMods = Join-Path $stageRoot 'overrides/mods'
New-Item -ItemType Directory -Force -Path $packMods | Out-Null
Copy-Item -LiteralPath $addonPath -Destination (Join-Path $packMods $addonName)
Copy-Item -LiteralPath (Join-Path $projectRoot 'defaults/living-settlements-server.toml') -Destination (Join-Path $stageRoot 'overrides/config/living-settlements-server.toml') -Force
$manifestPath = Join-Path $stageRoot 'manifest.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$manifest.version = "living-settlements-$version"
$manifest | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $manifestPath -Encoding utf8
$modlistPath = Join-Path $stageRoot 'modlist.html'
$modlist = Get-Content -LiteralPath $modlistPath -Raw
$modlist.Replace('</ul>', "<li>Living Settlements $version (local addon)</li>`n</ul>") | Set-Content -LiteralPath $modlistPath -Encoding utf8
$packPath = Join-Path $distributionRoot "kingdoms-living-settlements-$version.zip"
Compress-Archive -Path (Join-Path $stageRoot '*') -DestinationPath $packPath -Force
Write-Output "Addon: $(Join-Path $distributionRoot $addonName)"
Write-Output "CurseForge import: $packPath"
