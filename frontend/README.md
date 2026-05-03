# Diesel Truck Fault Code Advisor - Frontend

A professional React application for diesel truck fault code diagnostics with bilingual support (English/Spanish) and voice features.

## Features

- 🌐 **Bilingual Support**: Full English and Spanish interface
- 🎤 **Voice Input**: Speak fault codes instead of typing
- 🔊 **Voice Greeting**: Friendly bilingual welcome message on app load
- 📱 **Responsive Design**: Works on desktop, tablet, and mobile
- 🎨 **Professional UI**: Clean, easy-to-use interface for truck drivers and mechanics
- ⚡ **Real-time Analysis**: Instant AI-powered diagnostics from watsonx.ai

## Prerequisites

- Node.js 16+ and npm
- Spring Boot backend running on http://localhost:8080

## Installation

```bash
cd frontend
npm install
```

## Running the Application

```bash
npm start
```

The app will open at http://localhost:3000

## Usage

### Text Input
1. Enter the SPN number (e.g., 157)
2. Enter the FMI number (e.g., 3)
3. Click "Analyze Fault Code"

### Voice Input
1. Click the microphone button
2. Say the fault code numbers (e.g., "one five seven three")
3. The app will extract the numbers automatically
4. Click "Analyze Fault Code"

### Language Toggle
- Click the language button in the top right to switch between English and Spanish
- All UI text and AI responses will be in the selected language

## Voice Features

### Greeting
When the app first loads, it plays a bilingual greeting:
- English: "Hi! Tell me your fault code or type it in, whatever is easier for you."
- Spanish: "¡Hola! Dime tu código de falla o escríbelo, lo que sea más fácil para ti."

### Voice Recognition
- Uses Web Speech API (works in Chrome, Edge, Safari)
- Automatically extracts numbers from speech
- Supports both English and Spanish voice input

## Browser Compatibility

- ✅ Chrome/Edge: Full support (voice input + text-to-speech)
- ✅ Safari: Full support (voice input + text-to-speech)
- ⚠️ Firefox: Text input only (no voice features)

## API Integration

The frontend connects to the Spring Boot backend at:
- **Endpoint**: POST http://localhost:8080/api/analyze
- **Request**: `{ "spn": "157", "fmi": "3", "language": "en" }`
- **Response**: Comprehensive fault analysis with explanations, causes, fixes, tools, and parts

## Building for Production

```bash
npm run build
```

The optimized production build will be in the `build/` directory.

## Troubleshooting

### Voice Input Not Working
- Ensure you're using Chrome, Edge, or Safari
- Check browser permissions for microphone access
- Voice input requires HTTPS in production

### CORS Errors
- Ensure the Spring Boot backend is running
- Check that CORS is configured to allow http://localhost:3000

### API Connection Issues
- Verify backend is running on port 8080
- Check browser console for error messages
- Ensure .env file is configured correctly in the backend

## Technology Stack

- React 18
- Web Speech API (voice recognition & text-to-speech)
- CSS3 with modern gradients and animations
- Fetch API for backend communication

## Accessibility

- Large, easy-to-read text
- High contrast colors
- Voice input for hands-free operation
- Simple, intuitive interface
- Bilingual support for Spanish speakers

## License

Made with ❤️ for truck drivers and mechanics
