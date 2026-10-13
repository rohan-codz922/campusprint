# Runs the backend on your laptop with safe, temporary settings.
# Nothing secret is stored in any file: the JWT secret is random on every run,
# and you type the admin email and password when asked.
$env:JWT_SECRET = [Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")
if (-not $env:ADMIN_EMAIL) { $env:ADMIN_EMAIL = Read-Host "Admin email for local testing" }
if (-not $env:ADMIN_PASSWORD) {
  $secure = Read-Host "Admin password (8+ characters)" -AsSecureString
  $env:ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
$env:MAIL_LOG_CODES = "true"   # no email service locally, so codes are printed in this window
mvn spring-boot:run
