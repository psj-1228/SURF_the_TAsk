param(
    [string] $AdminUser = "root",
    [string] $AdminPassword,
    [string] $HostName = "localhost",
    [int] $Port = 3306
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $scriptDir
$dbDir = Join-Path $projectDir "src/main/resources/db/mysql"

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    throw "mysql command was not found. Install MySQL Server or add mysql.exe to PATH."
}

$previousMysqlPassword = $env:MYSQL_PWD

try {
    if ([string]::IsNullOrEmpty($AdminPassword)) {
        throw "Pass -AdminPassword with a MySQL administrator password."
    }

    $env:MYSQL_PWD = $AdminPassword

    Get-ChildItem -LiteralPath $dbDir -Filter "*.sql" |
        Sort-Object Name |
        ForEach-Object {
            Get-Content -Raw -LiteralPath $_.FullName |
                mysql -h $HostName -P $Port -u $AdminUser --default-character-set=utf8mb4
        }
}
finally {
    $env:MYSQL_PWD = $previousMysqlPassword
}

Write-Host "MySQL schema setup completed: surf_the_task"
