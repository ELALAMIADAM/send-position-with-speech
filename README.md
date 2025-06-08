# Location Sender with Emergency Detection

An Android application that allows users to share their location via SMS with an innovative emergency detection feature using volume button sequences.

## Features

### 📍 Location Services
- **Real-time GPS location tracking** using Google Play Services
- **Automatic location updates** on app startup
- **High-accuracy positioning** with fallback to network location
- **Location age verification** to ensure fresh coordinates
- **Google Maps integration** with clickable links

### 📱 SMS Integration  
- **One-tap location sharing** via SMS
- **Customizable phone numbers** with default emergency contact
- **Formatted location messages** with coordinates and Google Maps links
- **Automatic SMS sending** during emergencies

### 🚨 Emergency Volume Button Detection
- **Innovative emergency trigger** using volume button sequences
- **Configurable activation** (3 volume button presses by default)
- **Timeout protection** (3-second window for sequence completion)
- **Visual and haptic feedback** with vibration alerts
- **Automatic emergency SMS** with priority location detection

### 🔒 Security & Permissions
- **Runtime permission handling** for location and SMS
- **Privacy-conscious design** with user consent
- **Secure location data** transmission

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