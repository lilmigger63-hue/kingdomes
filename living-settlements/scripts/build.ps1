param([string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location -LiteralPath $projectRoot
try {
    if (-not $env:JAVA_HOME) {
        $portableJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk25') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($portableJdk) { $env:JAVA_HOME = $portableJdk.FullName }
    }
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
        throw 'Install JDK 25 and set JAVA_HOME to its installation directory.'
    }
    # Use the Windows trust roots, keeping normal TLS certificate verification enabled.
    $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE".Trim()
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-home'
    & .\gradlew.bat @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
