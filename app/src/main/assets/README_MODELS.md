# AI Models for AIVis

## Directory Structure

```
app/src/main/assets/
├── models/
│   ├── style_predict.tflite    (Style prediction model)
│   └── style_transfer.tflite   (Style transfer model)
└── styles/
    ├── oil_painting.jpg
    ├── watercolor.jpg
    ├── cartoon.jpg
    ├── pencil_sketch.jpg
    ├── vangogh.jpg
    ├── pop_art.jpg
    └── impressionism.jpg
```

## Required Models

### Magenta Arbitrary Image Stylization

This app uses the **Magenta Arbitrary Image Stylization** model which consists of two parts:

1. **style_predict.tflite** (Style Prediction Network)
   - Input: Style image (256x256 RGB, normalized [0, 1])
   - Output: Style bottleneck vector (1x1x1x100)
   - Purpose: Extracts style features from reference images

2. **style_transfer.tflite** (Style Transfer Network)
   - Input 1: Content image (384x384 RGB, normalized [0, 1])
   - Input 2: Style bottleneck vector (1x1x1x100)
   - Output: Stylized image (384x384 RGB, normalized [0, 1])
   - Purpose: Applies extracted style to content image

## Model Sources

Download from **TensorFlow Hub**:
- https://tfhub.dev/google/magenta/arbitrary-image-stylization-v1-256/2

Or from **Magenta GitHub**:
- https://github.com/magenta/magenta/tree/main/magenta/models/arbitrary_image_stylization

## Style Reference Images

Place 7 style reference images in `assets/styles/`:
- **oil_painting.jpg** - Oil painting texture example
- **watercolor.jpg** - Watercolor painting example
- **cartoon.jpg** - Bright cartoon/anime illustration
- **pencil_sketch.jpg** - Pencil drawing example
- **vangogh.jpg** - Van Gogh painting (e.g., Starry Night)
- **pop_art.jpg** - Pop art style (Warhol-like)
- **impressionism.jpg** - Impressionist painting with soft brushstrokes

Image requirements:
- Format: JPG or PNG
- Any size (will be scaled to 256x256)
- High quality reference images produce better results

## How It Works

1. User selects a style (e.g., "Oil Painting")
2. App loads corresponding style image from `assets/styles/`
3. Style image → `style_predict.tflite` → style bottleneck vector (cached)
4. User's photo + style vector → `style_transfer.tflite` → stylized result

## Fallback Behavior

If models are missing, the app uses simple ColorMatrix filters as fallback:
- Oil Painting → Increased saturation
- Watercolor → Soft color blend
- Cartoon → High saturation
- Pencil Sketch → Grayscale
- Van Gogh → Blue/yellow tint
- Pop Art → Very high saturation
- Impressionism → Soft pastel colors

This allows testing without models.
