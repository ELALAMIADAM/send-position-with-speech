# Emergency Location Sender App

This Android app can detect your location and send it via SMS to a predefined phone number. It includes an improved emergency detection feature that works even when the screen is off.

## Features

### 🔹 Basic Location Sharing
- Get current GPS location
- Send location via SMS to any phone number
- Automatic location updates with high accuracy

### 🚨 Emergency Detection (NEW & IMPROVED)
- **Trigger**: Hold Volume Up + Power button simultaneously for 1 second
- **Works when screen is OFF** - Critical improvement for emergencies
- **Background service** - Always listening when emergency mode is enabled
- **Haptic feedback** - Vibration confirmation when emergency is triggered
- **Automatic SMS** - Sends location immediately without user interaction

### 📱 SMS Auto-Response (NEW FEATURE)
- **Automatic location sharing** when receiving trigger messages
- **Smart message detection** - Responds to phrases like "give me your location"
- **Works 24/7** - No need to manually check and respond to messages
- **Instant response** - Sends location automatically within seconds
- **Multiple trigger phrases** - "send location", "where are you", etc.

## Setup Instructions

### 1. Install and Configure
1. Install the APK on your Android device
2. Grant all requested permissions:
   - Location (Fine & Coarse)
   - SMS
   - Notifications
   - Accessibility Service (for screen-off detection)

### 2. Enable Emergency Mode
1. Open the app
2. Enter the emergency contact phone number
3. Tap "Enable Emergency Mode"
4. **IMPORTANT**: Enable Accessibility Service when prompted:
   - Go to Settings > Accessibility
   - Find "Emergency Detection Service"
   - Turn it ON

### 3. Enable SMS Auto-Response (Optional)
1. Tap "Enable SMS Auto-Response" in the app
2. Grant SMS permissions when prompted:
   - Receive SMS
   - Read SMS
   - Send SMS
3. The app will now automatically respond to location requests

### 4. Test Emergency Detection
1. With emergency mode enabled, hold Volume Up + Power button together
2. Hold for 1 second until you feel vibration
3. Emergency SMS should be sent automatically

## How Emergency Detection Works

### Technical Implementation
- **Accessibility Service**: Globally detects key combinations even when screen is off
- **Background Service**: Handles location retrieval and SMS sending
- **Wake Locks**: Ensures emergency detection works when device is sleeping
- **Simultaneous Detection**: Detects Volume Up + Power button pressed within 500ms of each other
- **Hold Duration**: Requires 1-second hold to prevent accidental triggers

### Emergency Message Format
```
🚨 EMERGENCY ALERT 🚨
I need help! My location:
Latitude: XX.XXXXXX
Longitude: XX.XXXXXX

Google Maps: https://maps.google.com/?q=XX.XXXXXX,XX.XXXXXX

Sent automatically by emergency detection.
```

### SMS Auto-Response Triggers
The app automatically responds to messages containing:
- "give me your location"
- "send location" 
- "where are you"
- "share location"
- "your location"
- "location please"
- "send me location"

### Auto-Response Message Format
```
📍 My current location:
Latitude: XX.XXXXXX
Longitude: XX.XXXXXX
Accuracy: XX.Xm

🗺️ Google Maps: https://maps.google.com/?q=XX.XXXXXX,XX.XXXXXX

⏰ Sent automatically in response to your request
```

## Permissions Required

- **Location**: To get GPS coordinates
- **SMS**: To send emergency messages
- **Receive SMS**: To detect incoming location requests
- **Read SMS**: To analyze message content
- **Wake Lock**: To work when screen is off
- **Foreground Service**: For background emergency detection
- **Accessibility Service**: For global key detection
- **Notifications**: For foreground service notifications

## Important Notes

⚠️ **The accessibility service MUST be enabled for emergency detection to work when the screen is off.**

🔋 **The app uses minimal battery** - emergency detection runs efficiently in the background

📱 **Compatible with Android 8.0+** (API level 26+)

🚨 **This is an emergency safety tool** - test it thoroughly and inform your emergency contacts

## Troubleshooting

### Emergency detection not working?
1. Check if accessibility service is enabled
2. Ensure emergency mode is ON in the app
3. Try holding both buttons firmly for a full second
4. Check if the app has all required permissions

### SMS not sending?
1. Verify SMS permission is granted
2. Check if the phone number is correct (include country code)
3. Ensure you have network connectivity
4. Some carriers may block automated SMS

### Location not accurate?
1. Ensure GPS is enabled in device settings
2. Grant precise location permission
3. Test outdoors for better GPS signal
4. Make sure location services are enabled for the app

## Privacy & Security

- No data is stored or transmitted except the emergency SMS
- Location data is only accessed when needed
- No internet connection required (except for Google Maps link)
- All processing happens locally on your device

## Screenshots

*Add screenshots of your app here*

## Installation

### Prerequisites
- Android device running Android 6.0+ (API level 23+)
- GPS/Location services enabled
- SMS permissions granted

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/localisation_sender_with_speech.git
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Build and run on your device or emulator

### APK Installation
Download the latest APK from the [Releases](../../releases) section.

## Usage

### Basic Location Sharing
1. Launch the app
2. Grant location and SMS permissions when prompted
3. The app will automatically detect your current location
4. Enter a phone number or use the default emergency contact
5. Tap "Send Location SMS" to share your coordinates

### Emergency Mode
1. Tap "Enable Emergency Mode" to activate volume button detection
2. In an emergency, quickly press any volume button 3 times within 3 seconds
3. The app will automatically:
   - Get your current location
   - Send an emergency SMS to the configured number
   - Provide visual and haptic feedback

### Emergency SMS Format
```
🚨 EMERGENCY ALERT 🚨
I need help! My location:
Latitude: [your latitude]
Longitude: [your longitude]

Google Maps: https://maps.google.com/?q=[lat],[lng]

Sent automatically by volume button emergency detection.
```

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | High-accuracy GPS location tracking |
| `ACCESS_COARSE_LOCATION` | Network-based location as fallback |
| `SEND_SMS` | Sending location via text message |

## Configuration

### Default Emergency Contact
Update the default phone number in `MainActivity.java`:
```java
private static final String DEFAULT_PHONE_NUMBER = "+33780542575";
```

### Emergency Sequence Settings
Customize the emergency detection in `MainActivity.java`:
```java
private static final int EMERGENCY_SEQUENCE_REQUIRED = 3; // Number of presses
private static final long EMERGENCY_TIMEOUT_MS = 3000; // Timeout in milliseconds
```

## Technical Details

### Architecture
- **Language**: Java
- **Min SDK**: Android 6.0 (API 23)
- **Target SDK**: Android 14 (API 34)
- **Location Services**: Google Play Services Location API
- **SMS**: Android SmsManager

### Key Components
- `FusedLocationProviderClient` for location services
- `SmsManager` for SMS functionality
- Volume button event handling with `onKeyDown`/`onKeyUp`
- Handler-based timeout management for emergency sequences

### Dependencies
- Google Play Services Location
- AndroidX libraries
- Material Design Components

## Privacy & Security

- **No data collection**: Location data is only used locally and sent via SMS
- **User consent**: All permissions require explicit user approval
- **Secure transmission**: Location data is sent via encrypted SMS
- **No server communication**: All operations are performed locally on device

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Troubleshooting

### Location Issues
- Ensure GPS is enabled in device settings
- Grant location permissions to the app
- Try moving to an area with better GPS signal

### SMS Issues
- Verify SMS permissions are granted
- Check if the phone number format is correct
- Ensure sufficient SMS credit/plan

### Emergency Mode Issues
- Make sure emergency mode is enabled (button should be red)
- Press volume buttons quickly within the 3-second window
- Check that vibration is enabled for haptic feedback

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

If you encounter any issues or have questions:
- Create an [Issue](../../issues) on GitHub
- Check the [Troubleshooting](#troubleshooting) section

## Acknowledgments

- Google Play Services for location APIs
- Android SMS framework
- Material Design guidelines

---

**⚠️ Emergency Use Disclaimer**: This app is designed as a supplementary emergency tool. Always contact emergency services (911, 112, etc.) as your primary emergency response method.