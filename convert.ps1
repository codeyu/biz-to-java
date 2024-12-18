param (
    [string]$directory = (Get-Location),  # 如果没有输入目录，使用当前目录
    [string]$encoding = 'utf-8'           # 默认编码为 'utf-8'
)

# 检查指定的编码是否有效
$validEncodings = @('utf-8', 'utf-16', 'ascii', 'unicode')
if ($validEncodings -notcontains $encoding.ToLower()) {
    Write-Host "无效的编码格式。支持的编码格式有：utf-8, utf-16, ascii, unicode"
    exit
}

# 获取脚本所在的目录，如果用户未指定目录，则使用脚本目录
if ($directory -eq (Get-Location)) {
    $directory = Split-Path -Parent $MyInvocation.MyCommand.Definition
    Write-Host "未指定目录，使用当前脚本所在目录：$directory"
}

# 获取所有文件，递归遍历指定目录
$files = Get-ChildItem -Path $directory -Recurse -File

foreach ($file in $files) {
    Write-Host "正在处理文件: $($file.FullName)"
    try {
        # 读取文件内容
        $content = Get-Content -Path $file.FullName -Raw
        
        # 移除BOM标记
        $content = $content.TrimStart([char]0xFEFF)
        
        # 使用指定编码保存文件（如果是 UTF-8，带 BOM 会自动添加）
        switch ($encoding.ToLower()) {
            'utf-8' {
                $utf8NoBom = New-Object System.Text.UTF8Encoding $false
                $content | Set-Content -Path $file.FullName -Encoding UTF8 -NoNewline
                break
            }
            'utf-16' {
                $content | Set-Content -Path $file.FullName -Encoding Unicode
                break
            }
            'ascii' {
                $content | Set-Content -Path $file.FullName -Encoding ASCII
                break
            }
            'unicode' {
                $content | Set-Content -Path $file.FullName -Encoding Unicode
                break
            }
        }
        Write-Host "文件 $($file.FullName) 编码转换完成"
    } catch {
        Write-Host "处理文件 $($file.FullName) 时出错: $_"
    }
}

Write-Host "所有文件处理完成"
