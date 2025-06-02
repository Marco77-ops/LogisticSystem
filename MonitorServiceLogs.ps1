# PowerShell script to monitor service log files and report errors to Windows Event Log

# Define log file paths
$analyticServiceLogPath = "$env:TEMP\analyticservice.log"
$notificationServiceLogPath = "$env:TEMP\notificationservice.log"

# Create a new event source if it doesn't exist
$eventSourceName = "LogisticsSystem"
if (-not [System.Diagnostics.EventLog]::SourceExists($eventSourceName)) {
    [System.Diagnostics.EventLog]::CreateEventSource($eventSourceName, "Application")
    Write-Host "Created event source: $eventSourceName"
}

# Function to monitor a log file and report errors
function Monitor-LogFile {
    param (
        [string]$LogFilePath,
        [string]$ServiceName
    )

    if (Test-Path $LogFilePath) {
        $lastErrors = Get-Content $LogFilePath | Where-Object { $_ -match "ERROR" }
        
        if ($lastErrors) {
            foreach ($error in $lastErrors) {
                # Write to Windows Event Log
                Write-EventLog -LogName Application -Source $eventSourceName -EntryType Error -EventId 1001 -Message "[$ServiceName] $error"
                Write-Host "Logged error from $ServiceName to Windows Event Log: $error"
            }
            
            # Clear the log file after processing
            Clear-Content $LogFilePath
        }
    }
}

# Main monitoring loop
Write-Host "Starting log monitoring for analyticservice and notificationservice..."
try {
    while ($true) {
        Monitor-LogFile -LogFilePath $analyticServiceLogPath -ServiceName "analyticservice"
        Monitor-LogFile -LogFilePath $notificationServiceLogPath -ServiceName "notificationservice"
        
        # Wait before checking again
        Start-Sleep -Seconds 60
    }
} catch {
    Write-EventLog -LogName Application -Source $eventSourceName -EntryType Error -EventId 1002 -Message "Error in monitoring script: $_"
    Write-Host "Error: $_"
}