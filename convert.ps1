﻿param (
    [string]$directory = (Get-Location)
)

# 获取所有Java文件
$files = Get-ChildItem -Path $directory -Include "*.java","*.txt","*.xml","*.yml" -Recurse

foreach ($file in $files) {
    Write-Host "Processing: $($file.FullName)"
    try {
        # 读取文件内容
        $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
        
        # 使用UTF8编码（带BOM）重新写入文件
        $utf8WithBom = New-Object System.Text.UTF8Encoding $true
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8WithBom)
        
        Write-Host "Completed: $($file.FullName)"
    } catch {
        Write-Host "Error processing $($file.FullName): $_"
    }
}

Write-Host "All files have been processed."
