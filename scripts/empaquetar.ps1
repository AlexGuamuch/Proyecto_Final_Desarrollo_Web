<#
.SYNOPSIS
    Genera el zip de entrega: proyecto-fase-<fase>-<carné>.zip en la raíz del proyecto.

.DESCRIPTION
    Comprime el proyecto completo dentro de una carpeta con el mismo nombre del zip.
    Excluye target/, .git/, .idea/ y node_modules/ en cualquier nivel, además de la carpeta
    Fonts/ (descarga original de las fuentes), .claude/ y zips de entregas anteriores.
    Las rutas internas usan "/" para que el zip se abra igual en Windows, Linux y macOS.

.EXAMPLE
    .\scripts\empaquetar.ps1 -Carne 0000-00-000
.EXAMPLE
    .\scripts\empaquetar.ps1 -Carne 0000-00-000 -Fase 2
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = 'Número de carné, por ejemplo 0000-00-000')]
    [ValidatePattern('^\d{4}-\d{2}-\d{3,6}$')]
    [string]$Carne,

    [ValidateRange(1, 2)]
    [int]$Fase = 1
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$nombre = "proyecto-fase-$Fase-$Carne"
$destino = Join-Path $raiz "$nombre.zip"

$excluirEnCualquierNivel = @('target', '.git', '.idea', 'node_modules')
$excluirEnRaiz = @('Fonts', '.claude')

function Debe-Incluirse([string]$relativa) {
    $partes = $relativa -split '[\\/]'
    if ($excluirEnRaiz -contains $partes[0]) { return $false }
    foreach ($parte in $partes) {
        if ($excluirEnCualquierNivel -contains $parte) { return $false }
    }
    return -not ($partes.Count -eq 1 -and $partes[0] -like 'proyecto-fase-*.zip')
}

if (Test-Path $destino) {
    Remove-Item $destino
}

$archivos = Get-ChildItem -Path $raiz -Recurse -File -Force |
    Where-Object { Debe-Incluirse $_.FullName.Substring($raiz.Length + 1) }

$zip = [System.IO.Compression.ZipFile]::Open($destino, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($archivo in $archivos) {
        $relativa = $archivo.FullName.Substring($raiz.Length + 1) -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $archivo.FullName, "$nombre/$relativa",
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
}
finally {
    $zip.Dispose()
}

$tamano = [math]::Round((Get-Item $destino).Length / 1MB, 2)
Write-Host "Listo: $destino"
Write-Host "$($archivos.Count) archivos, $tamano MB"
Write-Host 'Antes de subirlo, descomprímelo en otra carpeta y verifica que compile (mvnw verify).'
