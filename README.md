# Robot Controller Android App 🤖

A comprehensive Android application that serves as an intelligent robot controller, integrating voice recognition, AI processing, and Bluetooth communication to control an FPGA-based robotic system.

## 🎯 Project Overview

This 4th-year project combines mobile technology with robotics, creating a smart bot that can understand voice commands and respond with appropriate actions. The system consists of:
- **Mobile App**: Android controller with AI capabilities
- **FPGA Hardware**: Controls motors and grippers
- **Bluetooth Communication**: HC-05 module for wireless control

## ✨ Key Features

### 🗣️ Voice Recognition & AI
- **Wake Word Detection**: Uses Picovoice Porcupine for "Hey Robot" wake word
- **Speech Recognition**: Android's native speech-to-text conversion
- **AI Processing**: Google Gemini 1.5 Flash for command interpretation
- **Voice Response**: Text-to-speech for robot feedback

### 📱 Manual Controls
- **Touch Controls**: Forward, backward, left, right movement buttons
- **Hold-to-Move**: Continuous movement while button is pressed
- **Bluetooth Management**: Device selection and connection interface

### 🔗 Communication
- **Bluetooth Integration**: HC-05 module communication
- **Command Protocol**: Structured UART messages to FPGA
- **Real-time Control**: Immediate command transmission

### 🛡️ Smart Features
- **Obstacle Avoidance**: Computer vision integration (planned)
- **Permission Management**: Runtime permission handling
- **Error Recovery**: Robust error handling and user feedback

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Android App   │◄──►│   Bluetooth      │◄──►│   FPGA Board    │
│                 │    │   (HC-05)        │    │                 │
├─────────────────┤    └──────────────────┘    ├─────────────────┤
│ • Voice Recognition                          │ • Motor Control │
│ • AI Processing                              │ • Gripper Control│
│ • UI Controls                                │ • Sensor Data   │
│ • Bluetooth Mgmt                             │                 │
└─────────────────┘                            └─────────────────┘
```

## 📋 Command Protocol

The app sends structured commands to the FPGA via Bluetooth:

| Command | Description | UART Message |
|---------|-------------|--------------|
| Forward | Move forward | `MOV-FD-#` |
| Backward | Move backward | `MOV-BD-#` |
| Left | Turn left | `MOV-LD-#` |
| Right | Turn right | `MOV-RD-#` |
| Stop | Stop movement | `STOP-#` |

## 🛠️ Technical Stack

### Development
- **Language**: Kotlin
- **Min SDK**: 35 (Android 14+)
- **Target SDK**: 35
- **IDE**: Android Studio

### Dependencies
- **AI Integration**: Google Generative AI Client (0.9.0)
- **Voice Processing**: Picovoice Porcupine (3.0.3)
- **UI Framework**: Jetpack Compose + View Binding
- **Bluetooth**: Android Bluetooth Classic
- **Architecture**: MVVM with Lifecycle components

### Hardware Requirements
- **FPGA Board**: For motor and gripper control
- **HC-05 Module**: Bluetooth communication
- **Motors**: Robot movement system
- **Gripper**: Object manipulation
- **Android Device**: Min API 35 with microphone and Bluetooth

## 📱 App Structure

### Core Components

#### MainActivity.kt
- Main activity orchestrating all components
- Permission management and UI setup
- Voice command processing and mapping

#### BluetoothManager.kt
- Handles Bluetooth device discovery and connection
- Manages UART communication with FPGA
- Implements connection security and error handling

#### AiManager.kt
- Google Gemini API integration
- Natural language command interpretation
- Context-aware responses for robot interaction

#### Voice Recognition System
- **WakeWordDetector**: Porcupine-based wake word detection
- **SpeechRecognizerManager**: Android speech recognition wrapper
- **Command Mapping**: Voice-to-action translation

## 🚀 Getting Started

### Prerequisites
1. Android Studio Koala or newer
2. Android device with API 35+
3. FPGA board with HC-05 Bluetooth module
4. Google AI API key for Gemini

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/Atharva-Deshmukh-23662/RobotController-AndroidApp-.git
   cd RobotController-AndroidApp-
   ```

2. **Configure API Keys**
   - Create `local.properties` file in project root
   - Add your Gemini API key:
     ```properties
     GENERATIVEAI_API_KEY=your_api_key_here
     ```

3. **Update Wake Word Key**
   - Replace `YOUR_WAKEWORD_KEY` in MainActivity.kt with your Picovoice access key
   - Get free key from [Picovoice Console](https://console.picovoice.ai/)

4. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Hardware Setup
1. Connect HC-05 to FPGA UART pins
2. Pair HC-05 with Android device
3. Power up the robot system
4. Launch the app and connect via Bluetooth

## 📱 Usage Instructions

### First Launch
1. Grant microphone and Bluetooth permissions
2. Tap "Connect" to select HC-05 device
3. Wait for successful Bluetooth connection
4. Voice recognition will automatically initialize

### Voice Commands
1. Say the wake word: **"Hey Robot"**
2. Wait for listening confirmation
3. Give commands like:
   - "Move forward"
   - "Turn left"
   - "Go backward"
   - "Turn right"
   - "Stop"

### Manual Control
- Use on-screen buttons for direct control
- Hold buttons for continuous movement
- Release to stop movement

## 🤖 AI Integration

The app uses **Google Gemini 1.5 Flash** for intelligent command processing:

- **Natural Language Understanding**: Interprets conversational commands
- **Context Awareness**: Maintains conversation context for better responses
- **Command Mapping**: Translates AI responses to robot actions
- **Feedback Generation**: Provides intelligent responses to user queries

## 🔧 Future Enhancements

### Planned Features
- **Computer Vision**: OpenCV integration for obstacle detection
- **Advanced Navigation**: Path planning and autonomous movement
- **Sensor Integration**: Environmental awareness and feedback
- **Multi-Robot Support**: Control multiple robots simultaneously
- **Cloud Connectivity**: Remote monitoring and control

### Technical Improvements
- Enhanced error recovery mechanisms
- Improved voice recognition accuracy
- Battery optimization for mobile deployment
- Advanced security for Bluetooth communication

## 🔐 Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For voice recognition
- `BLUETOOTH_CONNECT`: For robot communication
- `BLUETOOTH_ADMIN`: For device management
- `INTERNET`: For AI API communication

## 🐛 Troubleshooting

### Common Issues

**Voice Recognition Not Working**
- Check microphone permissions
- Ensure device has internet connection
- Verify Gemini API key configuration

**Bluetooth Connection Failed**
- Verify HC-05 is paired with device
- Check Bluetooth permissions are granted
- Ensure FPGA system is powered on

**App Crashes on Voice Command**
- Update to latest Android version
- Clear app cache and data
- Reinstall the application

## 👥 Contributing

This is a 4th-year academic project. For suggestions or improvements:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is developed for academic purposes. Please respect the educational nature of this work.

## 🙏 Acknowledgments

- **Google**: For Gemini AI API and Android development tools
- **Picovoice**: For wake word detection technology
- **Arduino Community**: For FPGA and hardware integration resources
- **Android Developer Community**: For extensive documentation and support

## 📞 Contact

**Developer**: Atharva Deshmukh, Sumit Shelwane, Rajvardhan Deshmukh
**Project**: 4th Year Engineering Project  
**Institution**: TSSM Bhivarabai Sawant College of Engineering and Reaserch

---

*This robot controller represents the fusion of mobile technology, artificial intelligence, and robotics - creating an intelligent, voice-controlled robotic system for modern applications.*
