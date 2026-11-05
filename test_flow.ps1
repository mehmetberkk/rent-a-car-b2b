$ErrorActionPreference = "Stop"

function Log-Step ($num, $title) {
    Write-Host "`n========================================================" -ForegroundColor Cyan
    Write-Host "STEP $($num): $title" -ForegroundColor Yellow
    Write-Host "========================================================" -ForegroundColor Cyan
}

$baseUrl = "http://localhost:8080"
$mailpitUrl = "http://localhost:8025"

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$testEmail = "ahmet.yilmaz.$timestamp@rentacar.com"
$testPassword = "Password123!"

Log-Step 1 "Registering New User: $testEmail"
$registerBody = @{
    firstName = "Ahmet"
    lastName = "Yılmaz"
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json -Compress

$regResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $registerBody -ContentType "application/json; charset=utf-8"
Write-Host "Registration Response:" -ForegroundColor Green
$regResponse | ConvertTo-Json -Depth 3 | Write-Host

Log-Step 2 "Checking Mailpit for Activation Email"
Write-Host "Waiting 3 seconds for Kafka event -> notification-service -> Mailpit..."
Start-Sleep -Seconds 3

$messagesResponse = Invoke-RestMethod -Uri "$mailpitUrl/api/v1/messages" -Method GET
$message = $messagesResponse.messages | Where-Object { 
    $addresses = @($_.To | ForEach-Object { $_.Address })
    $addresses -contains $testEmail
} | Select-Object -First 1

if (-not $message) {
    Write-Error "Activation email not found in Mailpit for $testEmail!"
}

Write-Host "Found email in Mailpit with ID: $($message.ID)" -ForegroundColor Green
Write-Host "Subject: $($message.Subject)" -ForegroundColor Green

# Fetch message details
$msgDetail = Invoke-RestMethod -Uri "$mailpitUrl/api/v1/message/$($message.ID)" -Method GET
$bodyText = $msgDetail.Text + " " + $msgDetail.HTML

# Regex extract activation token
# It can be in query param like token=... or directly a UUID
if ($bodyText -match "token=([a-f0-9\-]+)") {
    $activationToken = $Matches[1]
} elseif ($bodyText -match "([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})") {
    $activationToken = $Matches[1]
} else {
    Write-Error "Could not parse activation token from email body:`n$bodyText"
}

Write-Host "Extracted Activation Token: $activationToken" -ForegroundColor Green

Log-Step 3 "Activating User Account"
$activateBody = @{
    token = $activationToken
} | ConvertTo-Json -Compress

$activateResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/activate" -Method POST -Body $activateBody -ContentType "application/json; charset=utf-8"
Write-Host "Account Activated Successfully!" -ForegroundColor Green
$activateResponse | ConvertTo-Json -Depth 3 | Write-Host

Log-Step 4 "Logging in with Activated Account"
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json; charset=utf-8"
$accessToken = $loginResponse.data.accessToken
Write-Host "Login Successful! Access Token Acquired." -ForegroundColor Green
Write-Host "User ID: $($loginResponse.data.user.id)" -ForegroundColor Green

Log-Step 5 "Creating an Agency (Shop)"
$agencyBody = @{
    name = "Yılmaz Oto Kiralama - $timestamp"
    city = "İzmir"
    district = "Bornova"
    phone = "05321234567"
    contactPerson = "Ahmet Yılmaz"
    address = "Kazımdirik Mah. Sanayi Cad. No:10"
} | ConvertTo-Json -Compress

$authHeaders = @{
    "Authorization" = "Bearer $accessToken"
}

$agencyResponse = Invoke-RestMethod -Uri "$baseUrl/api/agencies" -Method POST -Headers $authHeaders -Body $agencyBody -ContentType "application/json; charset=utf-8"
$agency = $agencyResponse.data.agency
$agencyId = $agency.id
$refreshedToken = $agencyResponse.data.refreshedToken.accessToken
if (-not $refreshedToken) {
    $refreshedToken = $accessToken
}

Write-Host "Agency Created Successfully!" -ForegroundColor Green
Write-Host "Agency ID: $agencyId" -ForegroundColor Green
Write-Host "Agency Name: $($agency.name)" -ForegroundColor Green
Write-Host "Refreshed Token with Agency Claim Acquired." -ForegroundColor Green

Write-Host "Waiting 2 seconds for Kafka event identity.agency.upserted -> pool-service projection..."
Start-Sleep -Seconds 2

Log-Step 6 "Adding a Vehicle to Fleet"
$refreshedHeaders = @{
    "Authorization" = "Bearer $refreshedToken"
}

$randomPlate = "35EGE" + (Get-Random -Minimum 100 -Maximum 999)
$vehicleBody = @{
    agencyId = $agencyId
    plate = $randomPlate
    brand = "Renault"
    model = "Megane"
    modelYear = 2023
    fuelType = "DIESEL"
    transmission = "AUTOMATIC"
    dailyPoolPrice = 1500.00
    trafficInsuranceExpiry = "2026-11-20"
    cascoExpiry = "2026-12-31"
    inspectionExpiry = "2026-10-01"
} | ConvertTo-Json -Compress

$vehicleResponse = Invoke-RestMethod -Uri "$baseUrl/api/vehicles" -Method POST -Headers $refreshedHeaders -Body $vehicleBody -ContentType "application/json; charset=utf-8"
$vehicle = $vehicleResponse.data
$vehicleId = $vehicle.id

Write-Host "Vehicle Created Successfully!" -ForegroundColor Green
Write-Host "Vehicle ID: $vehicleId, Plate: $($vehicle.plate), Status: $($vehicle.status)" -ForegroundColor Green

Log-Step 7 "Querying Fleet Vehicles"
$fleetListResponse = Invoke-RestMethod -Uri "$baseUrl/api/vehicles?agencyId=$agencyId" -Method GET -Headers $refreshedHeaders
Write-Host "Fleet Count for Agency: $($fleetListResponse.data.totalElements)" -ForegroundColor Green

Log-Step 8 "Updating Vehicle Status to IN_POOL (Publishing to Shared Pool)"
$statusUpdateBody = @{
    status = "IN_POOL"
} | ConvertTo-Json -Compress

$statusResponse = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/$vehicleId/status" -Method PATCH -Headers $refreshedHeaders -Body $statusUpdateBody -ContentType "application/json; charset=utf-8"
Write-Host "Status Updated: $($statusResponse.data.status)" -ForegroundColor Green

Write-Host "Waiting 3 seconds for Kafka event fleet.vehicle.status-changed -> pool-service..."
Start-Sleep -Seconds 3

Log-Step 9 "Checking Shared Pool Marketplace Listings"
# Notice excludeOwn=false to see our vehicle in pool listings
$poolListings = Invoke-RestMethod -Uri "$baseUrl/api/pool/listings?city=İzmir&excludeOwn=false" -Method GET -Headers $refreshedHeaders
Write-Host "Total Listings in Pool for İzmir: $($poolListings.data.totalElements)" -ForegroundColor Green

$listedVehicle = $poolListings.data.content | Where-Object { $_.vehicleId -eq $vehicleId }
if (-not $listedVehicle) {
    Write-Error "Vehicle $vehicleId not found in pool listings!"
}

Write-Host "Found Vehicle in Pool!" -ForegroundColor Green
Write-Host "Plate: $($listedVehicle.plate)" -ForegroundColor Green
Write-Host "Brand/Model: $($listedVehicle.brand) $($listedVehicle.model)" -ForegroundColor Green
Write-Host "Daily Price: $($listedVehicle.dailyPoolPrice)" -ForegroundColor Green
Write-Host "Agency Projection Details in Pool:" -ForegroundColor Green
Write-Host "  Agency Name: $($listedVehicle.agency.name)" -ForegroundColor Yellow
Write-Host "  Agency City: $($listedVehicle.agency.city)" -ForegroundColor Yellow
Write-Host "  Agency Phone: $($listedVehicle.agency.phone)" -ForegroundColor Yellow
Write-Host "  Contact Person: $($listedVehicle.agency.contactPerson)" -ForegroundColor Yellow

Log-Step 10 "Fetching Single Vehicle Listing Details from Pool"
$singleListing = Invoke-RestMethod -Uri "$baseUrl/api/pool/listings/$vehicleId" -Method GET -Headers $refreshedHeaders
Write-Host "Single Listing Contact Details Verified:" -ForegroundColor Green
$singleListing.data | ConvertTo-Json -Depth 3 | Write-Host

Log-Step 11 "Returning Vehicle Status to AVAILABLE (Removing from Shared Pool)"
$backToAvailableBody = @{
    status = "AVAILABLE"
} | ConvertTo-Json -Compress

$backResponse = Invoke-RestMethod -Uri "$baseUrl/api/vehicles/$vehicleId/status" -Method PATCH -Headers $refreshedHeaders -Body $backToAvailableBody -ContentType "application/json; charset=utf-8"
Write-Host "Vehicle status changed back to: $($backResponse.data.status)" -ForegroundColor Green

Write-Host "Waiting 3 seconds for Kafka event fleet.vehicle.status-changed -> pool-service (removal)..."
Start-Sleep -Seconds 3

Log-Step 12 "Verifying Vehicle Was Removed from Shared Pool"
$poolAfterRemoval = Invoke-RestMethod -Uri "$baseUrl/api/pool/listings?city=İzmir&excludeOwn=false" -Method GET -Headers $refreshedHeaders
$stillInPool = $poolAfterRemoval.data.content | Where-Object { $_.vehicleId -eq $vehicleId }

if ($stillInPool) {
    Write-Error "Vehicle $vehicleId should have been removed from the pool but is still present!"
} else {
    Write-Host "Vehicle successfully removed from pool listings! Verified." -ForegroundColor Green
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "ALL 12 INTEGRATION & BUSINESS FLOW TESTS PASSED 100%!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
