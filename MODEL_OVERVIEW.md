Model: Magenta Arbitrary Image Stylization (TFLite)

Model files:
- style_predict_v2.tflite (style prediction)
- style_transfer_v2.tflite (style transfer)

Overview
The app uses the Magenta "arbitrary image stylization" two-stage model.
First the style image is passed into the style prediction network to produce
a 100-dimensional style bottleneck vector. Then the content image and the
style bottleneck are passed to the style transfer network which outputs a
stylized RGB image (values normalized in [0,1]). The project supports v2
(fp16) models as primary and v1 as fallback.

How the model is executed
- Models are loaded from assets/models/ and run with TensorFlow Lite Interpreter.
- Content images are resized to the model input size (typically 256/384) and
  converted to float buffers in RGB order (0..1). The transfer model is run
  with two inputs: content buffer and style bottleneck buffer.
- The raw float output is converted to an ARGB_8888 Bitmap and returned.

Quality improvements implemented
- Avoid forced upscaling: the pipeline no longer blindly rescales every image
  to the maximum model input; this reduces blurring and prevents amplifying
  compression artifacts.
- Edge-preserving sharpening (unsharp mask): replaced the naive 3x3 kernel
  convolution with an unsharp mask approach (downscale-blur-upscale and add
  the high-frequency residual). This gives more natural sharpening and fewer
  halos.
- Tiled inference with overlap and feathered blending: large images are split
  into overlapping tiles, each tile is processed at model input size, then the
  outputs are blended with triangular weights to avoid seams and limit memory.
- Fast edge-preserving denoise (pre and post): a lightweight downscale/upsample
  blur combined with a Sobel-based edge mask reduces noise in flat areas while
  preserving edges. This reduces noise amplification from sharpening and upscaling.
- Moderate post-processing: contrast and saturation boosts were tuned down to
  avoid plastic/oversaturated look while preserving detail.

Integration notes
- Model files should be placed in `app/src/main/assets/models/`.
- Interpreter options use XNNPACK and thread count equal to the device CPU.
- For performance and better quality consider using GPU/NNAPI delegates and
  a separate TFLite super-resolution model after stylization.

Tuning
- Unsharp amount: 0.12 - 0.4 (default 0.25)
- Contrast: 0.05 - 0.15 (default 0.12)
- Saturation: 0.05 - 0.12 (default 0.08)
- Tile overlap: 16 - 64 px (default 25% of tile size)

