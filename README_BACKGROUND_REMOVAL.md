# Background Removal Model

Model: MediaPipe Selfie Segmentation (Lite) (TFLite)

Model files:
- selfie_segmentation.tflite (semantic segmentation)

Overview
The background tool uses MediaPipe Selfie Segmentation to predict a soft per-pixel probability mask of the person (foreground) vs. the background. The mask is reprojected back to the original resolution, feathered, and reused across remove, blur, and replace operations so the pipeline never re-computes the same mask twice.

How the model is executed
- Model is loaded from `app/src/main/assets/models/selfie_segmentation.tflite` and run with the TensorFlow Lite Interpreter.
- Input images are resized to the model input (typically 257×257 or 320×320), converted to RGB float buffers normalized to `[0,1]`, and passed through the model.
- The single-channel float output is resized to the source resolution, blurred with a Gaussian kernel, and converted to an alpha mask for compositing.
- Mask values feed downstream logic: alpha compositing for background removal, blur radius control (5–50px), and soft edge preservation for PNG export.

Quality improvements implemented
- **Shared mask caching**: Once the mask is available, remove/blur/replace all reuse it instead of recomputing, reducing latency.
- **Feathered edges**: The float mask is blurred and thresholded with a soft falloff to avoid hard cuts around hair.
- **Adaptive blur**: The blur radius slider drives a separable Gaussian whose kernel size depends on the slider value (lower blur keeps detail, higher blur smooths backgrounds while preserving foreground edges).
- **Background replacement scaling**: Replacement assets are scaled to fit the canvas while preserving aspect ratio to avoid stretching.

Integration notes
- Store `selfie_segmentation.tflite` in `app/src/main/assets/models/`.
- Interpreter uses XNNPACK with thread count equal to the CPU; delegate support may be added later.
- The mask is reused for PNG export (alpha) and blur effect, so ensure mask computation finishes before downstream steps start.
- Background choices (NONE / REMOVE / BLUR / REPLACE) are stored in `EditorState` to keep undo/redo consistent.

Tuning
- **Blur radius**: 5 – 50 px (default 25 px).
- **Mask feather**: Gaussian sigma tuned to maintain hair detail while smoothing edges.
- **Threshold for alpha**: 0.3 – 0.6 (default 0.45) used when exporting PNG.
