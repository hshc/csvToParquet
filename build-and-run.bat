@echo off
echo ========================================
echo Athena SQL Executor - Build and Run
echo ========================================

echo.
echo 1. Compilation du projet...
call mvn clean package

if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: La compilation a échoué!
    pause
    exit /b 1
)

echo.
echo 2. Compilation réussie!
echo.

echo 3. Exécution de l'application...
echo Usage: java -jar target/sql-executor-1.0-snapshot.jar <sql-file> <config-file.toml>
echo.

if exist "example.sql" (
    if exist "config.toml" (
        echo Exécution avec les fichiers d'exemple...
        java -jar target/sql-executor-1.0-snapshot.jar example.sql config.toml
    ) else (
        echo Fichier config.toml manquant. Créez-le d'abord.
    )
) else (
    echo Fichier example.sql manquant. Créez-le d'abord.
)

echo.
echo ========================================
echo Terminé!
echo ========================================
pause
