@echo off
rem Opens a visible window and runs the advice test orchestrator. Extra args go to the script, e.g.
rem   scripts\run_advice_cases.cmd -AdviceUrl https://cropcast-advice.<you>.workers.dev -Language Hiligaynon
start "CropCast advice tests" cmd /k pwsh -NoProfile -File "%~dp0run_advice_cases.ps1" %*
