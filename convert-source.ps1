# 用于处理源代码文件（不带BOM）
param (
    [string]$directory = (Get-Location)
)

# 获取所有Java源文件
$files = Get-ChildItem -Path "$directory\src\main" -Include "*.java" -Recurse

foreach ($file in $files) {
    Write-Host "Processing source file: $($file.FullName)"
    try {
        # 读取文件内容
        $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
        
        # 使用UTF8编码（不带BOM）重新写入文件
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        
        Write-Host "Completed: $($file.FullName)"
    } catch {
        Write-Host "Error processing $($file.FullName): $_"
    }
}

Write-Host "All source files have been processed." 