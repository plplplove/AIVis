# Portrait Retouching Pipeline

**Model**: ML Kit FaceDetector + enhanced skin processing algorithms

**Model files**: Uses built-in ML Kit FaceDetector binaries provided by Google Play Services (no local `.tflite` required).

## Overview

Portrait AI is a professional-grade retouch system built in `PortraitProcessor`. It combines ML Kit face detection with advanced image processing algorithms to deliver three distinct enhancement modes:

1. **Beauty Mode** - Aggressive blemish removal, skin smoothing, eye brightening, and natural makeup application
2. **Eye Enhancement** - Dark circle removal and under-eye brightening
3. **Face Blur** - Selective skin blurring while preserving facial features

## How It Works

### Face Detection
- ML Kit FaceDetector identifies the largest face and returns precise bounding box
- Fallback to intelligent skin-tone heuristic if ML Kit fails
- `isSkinTone()` uses sophisticated RGB analysis to distinguish skin from hair/background
- Adaptive to diverse skin tones (light to dark)

### Processing Pipeline
Each mode applies effects within the detected face region using distance-based feathering for natural transitions. Blend factors scale with slider intensity (0-100%) and distance from face center.

## Beauty Mode - Enhanced Pipeline

### 4-Stage Processing:

**1. Aggressive Blemish Removal**
- **Dark spots** (acne, blackheads): threshold 15 (vs 20 before), blend up to 85% (vs 60%)
- **Light spots** (whiteheads, scars): NEW feature, blend up to 70%
- Larger blending radius (4px vs 3px) for smoother results
- Smart surrounding pixel averaging (excludes darker pixels to prevent spot spreading)

**2. Edge-Preserving Skin Smoothing**
- Bilateral filter approximation preserves facial structure
- Color-similarity threshold prevents blurring across edges
- Feathered application (stronger at center, fades at edges)
- Maintains skin texture while removing imperfections

**3. Eye Brightening (NEW)**
- Automatically detects eye regions (30% and 70% from left face edge)
- **Whites enhancement**: +15% brightness, +20% contrast for clearer, more alert eyes
- **Iris enhancement**: +20% contrast for more vivid eye colors
- Adaptive processing (different treatment for whites vs iris)

**4. Natural Makeup Application**
- **Blush**: 0.35 intensity (was 0.15), peachy-rosy tone on cheeks
  - +30 red, +5 green (natural warmth)
  - Distance-based falloff for soft edges
- **Lips**: 0.25 intensity (was 0.1)
  - Intelligent detection (checks if pixel is already lip-colored)
  - +25 red for actual lips, subtle tint for surrounding area
  - Natural gradient transitions

## Eye Enhancement Mode

**Target**: Under-eye area (below eyes, not on eyes)

**Effects**:
- Brightening: +20% luminance in dark circles
- Blur: Soft 50% blur to smooth texture
- Feathered blending for seamless integration

## Face Blur Mode

**Target**: Skin pixels only

**Exclusions**: Eyes, eyebrows, lips (precise region detection)

**Effect**: Strong 70% blur for bokeh-like skin softening while keeping features sharp

## Quality Features

### Feathering System
- Distance-based intensity falloff from face center
- Prevents harsh rectangular boundaries
- Natural transitions to neck/surrounding areas

### Intelligent Processing
- Skin-tone detection excludes non-skin pixels
- Blemish correction uses only lighter surrounding pixels
- Lip detection for targeted makeup application
- Separate white/iris processing for eyes

### Performance Optimizations
- Coroutine-based async processing (`Dispatchers.IO`)
- Efficient pixel-array operations
- Single-pass algorithms where possible

## Integration

### API Usage
```kotlin
val processor = PortraitProcessor(context)
processor.initialize()

// Beauty Mode (with all enhancements)
val result = processor.applyBeautyMode(bitmap, intensity = 0.5f)

// Eye Enhancement
val result = processor.enhanceEyes(bitmap, intensity = 0.5f)

// Face Blur
val result = processor.blurFace(bitmap, intensity = 0.5f)

processor.release()
```

### Intensity Guidelines
- **0-30%**: Subtle, natural look (everyday photos)
- **30-60%**: Noticeable enhancement (social media)
- **60-100%**: Professional/magazine-style (maximum effect)

## Technical Parameters

### Blemish Removal
- Dark spot threshold: `darknessDiff > 15`
- Light spot threshold: `lightnessDiff > 15`
- Max blend: 85% for dark, 70% for light
- Blending radius: 4 pixels

### Makeup Intensities
- Blush: 35% of slider value
- Lips: 25% of slider value
- Eye brightening: 60% of slider value

### Feather Formula
```kotlin
val distance = sqrt(dx² + dy²)
val feather = 1 - distance.coerceIn(0f, 1f)
val finalIntensity = baseIntensity * feather
```

## Results Comparison

| Feature | Before | After | Improvement |
|---------|--------|-------|-------------|
| Blemish removal | 60% max | 85% max | +42% stronger |
| Light spot removal | None | 70% | NEW feature |
| Blush visibility | 15% | 35% | +133% |
| Lip enhancement | 10% | 25% | +150% |
| Eye brightening | None | 15-20% | NEW feature |

## Architecture

- **PortraitProcessor**: Core processing algorithms
- **ProcessPortraitUseCase**: Use case layer (coordinates processor)
- **PortraitPanel**: UI component with intensity sliders
- **PhotoEditorScreen**: Integration with main editor

Clean Architecture compliance with clear separation of concerns.
