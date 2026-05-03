# Diesel Fault Advisor

A Spring Boot application that provides AI-powered diagnostic analysis for diesel truck fault codes using IBM watsonx.ai Granite model.

## Features

- **Fault Code Analysis**: Input SPN (Suspect Parameter Number) and FMI (Failure Mode Identifier) to get comprehensive diagnostics
- **AI-Powered Insights**: Uses IBM watsonx.ai Granite-3-8b-instruct model for intelligent analysis
- **Comprehensive Results**: Returns plain English explanations, root causes, step-by-step fix instructions, required tools, and parts to buy
- **REST API**: Simple JSON-based API for easy integration
- **IAM Authentication**: Automatic IBM Cloud IAM token management with caching
- **Error Handling**: Robust error handling with detailed error messages

## Project Structure

```
src/main/java/com/truckadvisor/dieselfaultadvisor/
├── controller/
│   └── FaultCodeController.java       # REST API endpoints
├── service/
│   ├── WatsonxService.java           # watsonx.ai integration
│   └── IamTokenService.java          # IBM Cloud IAM authentication
├── dto/
│   ├── FaultCodeRequest.java         # Request DTO
│   ├── FaultCodeAnalysis.java        # Response DTO
│   ├── WatsonxApiRequest.java        # watsonx.ai API request
│   ├── WatsonxApiResponse.java       # watsonx.ai API response
│   └── IamTokenResponse.java         # IAM token response
├── config/
│   └── WatsonxConfig.java            # Configuration properties
└── exception/
    ├── GlobalExceptionHandler.java   # Global error handling
    └── WatsonxApiException.java      # Custom exception
```

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- IBM Cloud account with watsonx.ai access
- IBM Cloud API key (not watsonx.ai specific - your general IBM Cloud API key)
- watsonx.ai Project ID
- Access to IBM Granite-3-8b-instruct model

## Authentication Flow

The application uses IBM Cloud IAM (Identity and Access Management) for authentication:

1. **API Key → IAM Token**: Your IBM Cloud API key is exchanged for an IAM access token
2. **Token Caching**: The IAM token is cached and automatically refreshed before expiration
3. **watsonx.ai API**: The IAM token is used as a Bearer token for watsonx.ai API calls

This is handled automatically by the `IamTokenService` - you only need to provide your IBM Cloud API key.

## Configuration

### Environment Variables

Set the following environment variables:

```bash
# Required: Your IBM Cloud API key (get from https://cloud.ibm.com/iam/apikeys)
export WATSONX_API_KEY="your-ibm-cloud-api-key"

# Required: Your watsonx.ai project ID (from project settings)
export WATSONX_PROJECT_ID="your-watsonx-project-id"
```

**Important**: Use your IBM Cloud API key, not a watsonx.ai-specific key. The application will automatically exchange it for an IAM token.

### Optional Configuration

You can override default settings via environment variables:

```bash
export WATSONX_URL="https://us-south.ml.cloud.ibm.com"
export WATSONX_MODEL_ID="ibm/granite-3-8b-instruct"
export WATSONX_MAX_TOKENS="1000"
export WATSONX_TEMPERATURE="0.7"
```

## Running the Application

### Using Maven

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Using Java

```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/dieselfaultadvisor-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Documentation

### Analyze Fault Code

**Endpoint:** `POST /api/analyze`

**Request Body:**
```json
{
  "spn": "157",
  "fmi": "3"
}
```

**Response:**
```json
{
  "faultCode": "SPN 157 FMI 3",
  "explanation": "Injector Metering Rail #1 Pressure - Voltage Above Normal. This indicates the fuel rail pressure sensor is reading a voltage higher than expected...",
  "rootCauses": [
    "Faulty fuel pressure sensor",
    "Wiring harness short to power",
    "ECM internal fault",
    "Connector corrosion or damage"
  ],
  "fixInstructions": [
    "Check fuel pressure sensor connector for corrosion or damage",
    "Test sensor voltage with multimeter (should be 0.5-4.5V)",
    "Inspect wiring harness for shorts to power or damage",
    "Check sensor resistance (typically 100-1000 ohms)",
    "Replace sensor if readings are out of specification"
  ],
  "toolsNeeded": [
    "Multimeter",
    "Fuel pressure gauge",
    "Socket set",
    "Wire stripper/crimper"
  ],
  "partsToBuy": [
    "Fuel rail pressure sensor (OEM recommended)",
    "Connector repair kit (if needed)",
    "Dielectric grease"
  ],
  "timestamp": "2026-05-03T01:38:00.000Z"
}
```

### Health Check

**Endpoint:** `GET /api/health`

**Response:**
```
Diesel Fault Advisor API is running
```

## Testing with cURL

```bash
# Analyze a fault code
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "spn": "157",
    "fmi": "3"
  }'

# Health check
curl http://localhost:8080/api/health
```

## Error Handling

The API returns structured error responses:

### Validation Error (400)
```json
{
  "timestamp": "2026-05-03T01:38:00.000Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "spn": "SPN (Suspect Parameter Number) is required",
    "fmi": "FMI (Failure Mode Identifier) is required"
  }
}
```

### AI Service Error (503)
```json
{
  "timestamp": "2026-05-03T01:38:00.000Z",
  "status": 503,
  "error": "AI Service Error",
  "message": "Failed to call watsonx.ai API: Connection timeout"
}
```

### Internal Server Error (500)
```json
{
  "timestamp": "2026-05-03T01:38:00.000Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later."
}
```

## Common Diesel Fault Codes

Here are some common SPN/FMI combinations you can test:

- **SPN 157 FMI 3**: Fuel Rail Pressure - Voltage Above Normal
- **SPN 94 FMI 1**: Fuel Delivery Pressure - Data Valid But Below Normal
- **SPN 110 FMI 3**: Engine Coolant Temperature - Voltage Above Normal
- **SPN 190 FMI 2**: Engine Speed - Data Erratic, Intermittent, or Incorrect
- **SPN 639 FMI 2**: J1939 Network - Data Erratic, Intermittent, or Incorrect

## Development

### Building for Production

```bash
mvn clean package -DskipTests
```

### Running Tests

```bash
mvn test
```

## Technologies Used

- **Spring Boot 3.5.14**: Application framework
- **Java 21**: Programming language
- **Lombok**: Reduce boilerplate code
- **Jackson**: JSON processing
- **Jakarta Validation**: Request validation
- **IBM watsonx.ai**: AI-powered analysis using Granite-3-8b-instruct model
- **IBM Cloud IAM**: Authentication and token management

## How IAM Authentication Works

The application implements a robust IAM authentication flow:

1. **Token Exchange**: On first request, `IamTokenService` exchanges your API key for an IAM access token
2. **Token Caching**: The token is cached in memory with its expiration time
3. **Automatic Refresh**: Tokens are automatically refreshed 5 minutes before expiration
4. **Thread Safety**: Token management is synchronized to prevent race conditions
5. **Error Handling**: Failed token exchanges are logged and throw `WatsonxApiException`

### Token Lifecycle

- **Initial Request**: API key → IAM token (valid for ~60 minutes)
- **Subsequent Requests**: Uses cached token (no API call needed)
- **Near Expiration**: Automatically refreshes token (5 min buffer)
- **On Error**: Clears cache and retries on next request

This ensures optimal performance while maintaining security.

## Security Notes

- **Never commit API keys**: All credentials should be in environment variables
- **Use environment variables**: Set `WATSONX_API_KEY` and `WATSONX_PROJECT_ID` via environment
- **Token Security**: IAM tokens are cached in memory only (not persisted)
- **Token Expiration**: Tokens automatically expire and refresh
- **CORS Configuration**: Update `@CrossOrigin` in `FaultCodeController` for production
- **Rate Limiting**: Consider implementing rate limiting for production use
- **HTTPS Only**: Always use HTTPS in production environments

## Troubleshooting

### 401 Unauthorized Error

If you get a 401 error:
1. Verify your IBM Cloud API key is correct: `echo $WATSONX_API_KEY`
2. Ensure the API key has access to watsonx.ai
3. Check the logs for IAM token exchange errors
4. Try creating a new API key at https://cloud.ibm.com/iam/apikeys

### Token Exchange Fails

If IAM token exchange fails:
1. Check your internet connection
2. Verify the IAM endpoint is accessible: `https://iam.cloud.ibm.com`
3. Ensure your API key is not expired or revoked
4. Check application logs for detailed error messages

### watsonx.ai API Errors

If the watsonx.ai API returns errors:
1. Verify your project ID is correct
2. Ensure your project has access to the Granite model
3. Check your watsonx.ai service instance is active
4. Review the model parameters in `application.properties`

## License

This project is for educational and demonstration purposes.

## Support

For issues or questions, please create an issue in the repository.