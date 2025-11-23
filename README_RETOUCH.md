# Portrait Retouching Pipeline

Model: ML Kit FaceDetector + heuristic skin smoothing (no external TFLite)

Model files:
- Uses built-in ML Kit FaceDetector binaries provided by Google Play Services (no local `.tflite` required).

Overview
Portrait AI is a lightweight retouch stack built directly in `PortraitProcessor`. It mixes ML Kit face detection with heuristic skin masking to apply three tool-specific passes: blemish removal + feathered smoothing, under-eye brightening, and skin-only blur, all tuned for subtlety and natural transitions.

How the model is executed
- ML Kit FaceDetector identifies the largest face and returns a bounding box. If the API fails, a fallback skin-tone heuristic scans the frame for skin-colored pixels.
- `isSkinTone()` filters RGB ratios to exclude hair/eyebrows while covering diverse skin tones. The resulting mask bounds editing operations.
- Each Portrait mode applies filters inside the bounding box with distance-based feathering so effects fade toward the cheeks/neck without hard edges.
- Blend factors depend on slider intensity (0–100%) scaled to `[0, 1]` and multiplied by `feather` distance to center.

Quality improvements implemented
- **Feathered smoothing**: Smoothing intensity decreases near edges to prevent visible rectangles and preserve detail.
- **Blemish-aware blending**: `removeBlemishes` blends only with surrounding pixels that are at least as bright as the blemish to avoid spreading dark spots.
- **Localized makeup**: `applySubtleMakeup` adds rosy cheeks and lip tint without affecting eyes or hair.
- **Dark circle focus**: Brightening and blur target the under-eye pockets, leaving eyes sharp.
- **Skin-only blur**: `blurSkinOnly` skips eyes/lips using precise exclusion zones, and uses stronger blending (70%) for a noticeable yet controlled softening.

Integration notes
- Face detection relies on `FaceDetection.getClient` configured for accurate mode and tracking. No manual `.tflite` management is required.
- The pipeline runs on `Dispatchers.IO` and uses `withContext` + coroutines for async image processing.
- Intensity slider values are clamped to `[0f, 1f]` before feeding smoothing/blur functions.
- Background state is updated only after each retouch pass completes; use `PortraitProcessor.applyBeautyMode`, `enhanceEyes`, and `blurFace` sequentially via the UI.

Tuning
- **Intensity scale**: 0–1 (mapped from slider). The slider controls smoothing radius, blend factors, and blush strength.
- **Feather factor**: Distance-based falloff ensures center gets strongest effect; edges taper to zero at the face periphery.
- **Blemish detection threshold**: `darknessDiff` > 20 triggers corrections; blend limited to 0.6 to avoid over-brightening.
- Confirm `Remove Dark Circles` focuses on bags without affecting the eyes themselves.
- For `Skin Blur`, verify blur strength increases without oversmoothing eyes or lips.
