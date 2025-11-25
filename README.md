# AIVis - AI Vision Photo Editor

An offline Android photo editing application powered by AI, using local TensorFlow Lite and ML Kit models for on-device image processing.

## Overview

AIVis is a diploma project (2025) that brings professional AI-powered photo editing capabilities entirely offline. The application runs all ML models locally on the device, ensuring privacy and enabling editing without internet connectivity.

## Technologies Used

### Core Technologies
- **Language**: Kotlin 1.9.x
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture
- **Build System**: Gradle 8.x with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 14+)

### AI/ML Stack
- **TensorFlow Lite 2.14.0**: On-device ML inference
- **TensorFlow Lite Support 0.4.4**: Image preprocessing utilities
- **TensorFlow Lite GPU 2.14.0**: GPU acceleration for models
- **ML Kit Face Detection 16.1.6**: Google's face detection API

### Architecture Components
- **Room Database**: Local photo storage and metadata
- **DataStore Preferences**: User settings persistence
- **Coroutines**: Asynchronous operations
- **StateFlow**: Reactive state management
- **ViewModel**: UI state management with lifecycle awareness

### Additional Libraries
- **Coil 2.5.0**: Image loading and caching
- **Material 3**: Modern Android UI components
- **Coroutines Play Services 1.7.3**: ML Kit integration

## Project Structure

```
app/
├── src/main/
│   ├── java/com/ai/vis/
│   │   ├── ai/                      # AI model processors
│   │   │   ├── processor/
│   │   │   │   ├── BackgroundProcessor.kt    # Background removal/blur/replace
│   │   │   │   └── PortraitProcessor.kt      # Portrait retouching (Beauty Mode, eye enhancement)
│   │   │   └── style/
│   │   │       └── StyleTransferProcessor.kt # AI style transfer (8 styles)
│   │   ├── data/                    # Data layer
│   │   │   ├── AppDatabase.kt       # Room database
│   │   │   ├── EditedPhoto.kt       # Photo entity
│   │   │   ├── EditedPhotoDao.kt    # DAO interface
│   │   │   ├── PhotoRepository.kt   # Data repository
│   │   │   ├── AppSettings.kt       # Settings model
│   │   │   └── SettingsDataStore.kt # Settings storage
│   │   ├── domain/                  # Domain layer
│   │   │   ├── model/
│   │   │   │   ├── AIStyle.kt       # AI style enum
│   │   │   │   └── EditorUiState.kt # Editor state model
│   │   │   └── usecase/             # Business logic
│   │   │       ├── ApplyAdjustmentsUseCase.kt
│   │   │       ├── ApplyAIStyleUseCase.kt
│   │   │       ├── ApplyCropRotateUseCase.kt
│   │   │       ├── ApplyFilterUseCase.kt
│   │   │       ├── ClearAllDataUseCase.kt
│   │   │       ├── GetSettingsUseCase.kt
│   │   │       ├── ProcessBackgroundUseCase.kt
│   │   │       ├── ProcessPortraitUseCase.kt
│   │   │       ├── UpdateLanguageUseCase.kt
│   │   │       └── UpdateThemeUseCase.kt
│   │   ├── ui/                      # UI layer
│   │   │   ├── components/          # Reusable components
│   │   │   ├── screens/             # Application screens
│   │   │   │   ├── MainScreen.kt
│   │   │   │   ├── PhotoEditorScreen.kt
│   │   │   │   ├── PhotoGalleryScreen.kt
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SplashScreen.kt
│   │   │   └── theme/               # Material 3 theming
│   │   ├── utils/                   # Utility classes
│   │   │   ├── ImageProcessor.kt    # Image manipulation utilities
│   │   │   ├── LocaleHelper.kt      # Language switching
│   │   │   └── PhotoSaver.kt        # Photo save operations
│   │   ├── viewmodel/               # ViewModels
│   │   │   ├── PhotoEditorViewModel.kt
│   │   │   ├── PhotoEditorViewModelFactory.kt
│   │   │   ├── PhotoGalleryViewModel.kt
│   │   │   ├── SettingsViewModel.kt
│   │   │   └── SettingsViewModelFactory.kt
│   │   └── MainActivity.kt
│   ├── res/
│   │   ├── values/                  # English strings
│   │   ├── values-pl/               # Polish strings
│   │   ├── drawable/                # Icons and graphics
│   │   ├── font/                    # 15 custom fonts
│   │   └── ml/                      # TensorFlow Lite models
│   │       ├── isnet.tflite         # Background removal model
│   │       ├── face_landmarks.tflite # Facial landmarks detection
│   │       └── style_*.tflite       # Style transfer models
│   └── assets/                      # Additional resources
└── build.gradle.kts
```

## Features

### 1. Photo Editor
The main photo editing workspace with 9 powerful tools:

#### Crop & Rotate
- Free crop or fixed aspect ratios (1:1, 4:3, 16:9)
- Rotate left/right (90-degree increments)
- Flip horizontal/vertical
- Straighten with angle adjustment

#### Adjust
- Brightness: Lighten or darken the image
- Contrast: Increase or decrease color contrast
- Saturation: Adjust color intensity
- Sharpness: Enhance or soften edges
- Temperature: Add warmth (orange) or coolness (blue)
- Tint: Adjust green/magenta color balance
- Real-time preview with sliders

#### Filters
8 preset filters with adjustable intensity:
- Original (no filter)
- Black & White
- Sepia (vintage brown tone)
- Vintage (faded retro look)
- Cool (blue tones)
- Warm (orange tones)
- Grayscale (pure B&W)
- Invert (negative colors)

#### Stickers
- 40+ emoji stickers
- Transform: Scale, rotate, move
- Adjust opacity
- Multiple stickers per image

#### Portrait (AI-Powered)
Three AI retouching options powered by ML Kit Face Detection:
- **Beauty Mode**: Advanced skin smoothing and blemish removal with makeup enhancement (brightens skin, removes imperfections, adds subtle makeup effect)
- **Eye Enhancement**: Brightens eyes and removes dark circles using facial landmarks
- **Skin Blur**: Professional skin smoothing while preserving facial features
- Adjustable intensity for each effect
- Real-time face detection and processing

#### AI Styles
8 artistic style transfer effects using TensorFlow Lite:
- Original
- Oil Painting
- Watercolor
- Cartoon
- Pencil Sketch
- Van Gogh
- Pop Art
- Impressionism

#### AI Background (TensorFlow Lite)
Advanced background manipulation using the ISNet segmentation model:
- **Remove Background**: Transparent background with alpha channel
- **Blur Background**: Gaussian blur (adjustable radius: 1-50px)
- **Replace Background**: Choose color or upload custom image
- Precise person segmentation
- GPU-accelerated processing

#### Text
Professional text overlay with 15 custom fonts:
- Font selection (Abite, American Captain, Bear Days, Cool Vetica, Cute Notes, JMH Typewriter, Karina, Keep On Truckin, Komicax, Lemon Milk, Porkys, Riffic Free, Rossten, Sweety Rasty, Varsity Team)
- Size, color, opacity adjustments
- Text style: Bold, Italic, Underline, Strikethrough
- Letter spacing control
- Shadow effects (radius, offset X/Y)
- Transform: Move, scale, rotate
- Multiple text items per image

#### Draw
Freehand drawing and shapes:
- Brush tool with adjustable size and opacity
- Softness control for smooth edges
- Eraser mode
- Color picker
- Shapes: Line, Arrow, Rectangle, Rounded Rectangle, Circle, Star
- Fill or stroke style for shapes
- Multiple drawing layers

### 2. Photo Gallery
- Grid view of all edited photos
- Photo metadata: Filename, size, dimensions, date
- Full-screen image viewer
- Multi-select mode for batch operations
- Delete photos with confirmation
- Re-edit previous work
- Export to device gallery
- Share photos

### 3. Settings
- **Theme**: Dark mode / Light mode toggle (persistent)
- **Language**: English / Polish (Polski) with full app translation
- **Clear Cache**: Remove all photos and reset app data
- **About**: App version and project information

### 4. Universal Features
- **Undo/Redo**: 20-level history stack for all operations
- **Save Options**: 
  - Gallery only (device photo library)
  - App only (private app storage)
  - Both locations
- **Real-time Preview**: All adjustments show instant previews
- **State Persistence**: Maintains edits across tool switches
- **Optimized Performance**: GPU acceleration for AI operations

## Application Screens

### 1. Splash Screen
- App logo and branding
- 2-second auto-dismiss
- Smooth fade transition to main screen

### 2. Main Screen (Home)
Two tabs with bottom navigation:
- **Editor Tab**: 
  - Large "Start Editing" button
  - Choose photo source: Gallery or Camera
  - Direct access to photo editing
- **My Gallery Tab**:
  - Grid of edited photos with thumbnails
  - Empty state with helpful hint
  - Tap to view full image
  - Long press for multi-select

### 3. Photo Editor Screen
- Top bar: Back, Undo/Redo buttons, Save/Apply
- Center: Image canvas with zoom and pan
- Bottom: Tool selector with 9 tools
- Tool panels: Context-sensitive controls per tool
- Floating overlays: Crop guide, text items, stickers, drawings

### 4. Full Image Viewer
- Large image preview
- Action buttons:
  - Export to gallery
  - Delete photo
  - Re-edit (reopens in editor)
  - Share via system share sheet
- Photo metadata display
- Edit filename dialog

### 5. Settings Screen
- Theme toggle switch (Dark/Light)
- Language selector (English/Polish)
- Clear cache button with warning dialog
- About section (version, description, diploma info)

## Navigation Flow

```
MainActivity
    ├── SplashScreen (auto-dismiss after 2s)
    └── MainScreen
            ├── Editor Tab
            │   ├── Choose Image Source BottomSheet
            │   │   ├── Gallery → (system gallery picker)
            │   │   └── Camera → (system camera)
            │   └── → PhotoEditorScreen
            │           └── Back → MainScreen
            │
            ├── Gallery Tab (PhotoGalleryScreen)
            │   ├── Tap photo → FullImageViewerScreen
            │   │   ├── Re-Edit → PhotoEditorScreen
            │   │   └── Back → PhotoGalleryScreen
            │   └── Long press → Multi-select mode
            │
            └── Settings Button → SettingsScreen
                    └── Back → MainScreen
```

## Setup Instructions

### Prerequisites
- **Android Studio**: Koala or later (2024.x+)
- **JDK**: Version 11 or higher
- **Gradle**: 8.0+ (included in project)
- **Android SDK**: API 26-36
- **Physical Device or Emulator**: API 26+ with at least 2GB RAM

### Installation Steps

1. Clone the repository:
```bash
git clone https://github.com/plplplove/AIVis.git
cd AIVis
```

2. Open the project in Android Studio:
   - File → Open → Select AIVis folder
   - Wait for Gradle sync to complete

3. Verify ML models are present:
   - Check `app/src/main/res/ml/` for .tflite files
   - Models should include: isnet.tflite, face_landmarks.tflite, style_*.tflite

4. Configure local.properties (if needed):
```properties
sdk.dir=/path/to/Android/Sdk
```

5. Sync Gradle dependencies:
   - Tools → Gradle → Sync Project with Gradle Files

6. Build the project:
   - Build → Make Project (Ctrl+F9 / Cmd+F9)

7. Run on device/emulator:
   - Select target device from dropdown
   - Run → Run 'app' (Shift+F10 / Ctrl+R)

### Building APK

#### Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release APK (unsigned)
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

#### Signed Release APK
1. Generate keystore:
```bash
keytool -genkey -v -keystore aivis-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias aivis
```

2. Update `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../aivis-release-key.jks")
            storePassword = "your_password"
            keyAlias = "aivis"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

3. Build signed APK:
```bash
./gradlew assembleRelease
```

### Running Tests
```bash
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumentation tests
```

## Performance Considerations

- **AI Processing**: Runs on device GPU when available, falls back to CPU
- **Memory**: Background removal and style transfer require 100-300MB RAM
- **Processing Time**: 
  - Filters: <100ms
  - Adjustments: <200ms
  - Portrait AI: 500-2000ms (depends on image resolution)
  - Background AI: 1000-4000ms
  - Style Transfer: 2000-5000ms
- **Recommendations**: Use devices with 2GB+ RAM, Android 8.0+

## Supported Languages

- English (default)
- Polish (Polski) - Complete translation of all UI strings and messages

## Permissions Required

- **READ_MEDIA_IMAGES** (API 33+) / **READ_EXTERNAL_STORAGE** (API 26-32): Load photos from gallery
- **CAMERA**: Take photos with device camera
- **WRITE_EXTERNAL_STORAGE** (API 26-28): Save photos to gallery

All permissions are requested at runtime with appropriate rationale dialogs.

## Known Limitations

- Style transfer models work best on images under 2048x2048 pixels
- Background removal requires clear person-background separation
- Portrait AI requires frontal face visibility
- Maximum undo/redo stack: 20 operations
- Supported image formats: JPEG, PNG, WebP

## License

This project is a diploma project for educational purposes (2025).

## Credits

**Diploma Project 2025**  
AIVis - AI Vision Photo Editor

### Models Used
- ISNet: Background segmentation (Apache 2.0)
- ML Kit Face Detection: Google (Terms of Service)
- Style Transfer: Custom TensorFlow Lite models

### Libraries
All third-party libraries are used under their respective licenses:
- TensorFlow Lite: Apache 2.0
- ML Kit: Google Terms of Service
- Jetpack Compose: Apache 2.0
- Room, Coroutines, Material 3: Apache 2.0
- Coil: Apache 2.0

