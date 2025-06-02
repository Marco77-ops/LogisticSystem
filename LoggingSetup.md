# Logging Setup for Logistics System

This document describes the logging setup for the Logistics System, specifically for the `analyticservice` and `notificationservice` components.

## Overview

The system is configured to log errors to files and then report these errors to the Windows Event Log, allowing for centralized monitoring and alerting.

## Components

### 1. Service Logging Configuration

Both `analyticservice` and `notificationservice` use Logback for logging, configured via `logback-spring.xml` files. These configurations:

- Include Spring Boot's default logging configuration
- Add a file appender that only logs ERROR level messages
- Use rolling file policies to manage log file sizes and retention

### 2. Log Monitoring Script

The `MonitorServiceLogs.ps1` PowerShell script:

- Monitors the log files for both services
- Reports any errors found to the Windows Event Log
- Runs continuously, checking for new errors every 60 seconds
- Clears processed log entries to avoid duplicates

## Setup Instructions

1. Ensure the `logback-spring.xml` files are in the correct locations:
   - `analyticservice/src/main/resources/logback-spring.xml`
   - `notificationservice/src/main/resources/logback-spring.xml`

2. Run the monitoring script as an administrator:
   ```powershell
   powershell -ExecutionPolicy Bypass -File MonitorServiceLogs.ps1
   ```

3. To run the script as a Windows service, you can use NSSM (Non-Sucking Service Manager) or similar tools.

## Monitoring Errors

To check for errors from these services in the Windows Event Log, use the following PowerShell command:

```powershell
Check-EventLog -LogName Application | Where-Object { $_.EntryType -eq "Error" -and $_.Message -match "analyticsservice|notificationservice" }
```

## Troubleshooting

- If no errors appear in the Event Log, check if the log files are being created in the temp directory
- Verify that the services are running and properly configured
- Check if the monitoring script is running with sufficient permissions