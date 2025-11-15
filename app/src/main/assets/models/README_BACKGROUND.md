# Background Segmentation Model

## ⚠️ ВАЖЛИВО: DeepLab v3 НЕ ПРАЦЮВАТИМЕ!

Якщо ви завантажили DeepLab v3, вона НЕ підходить. Використовуйте MediaPipe!

## ✅ Що потрібно:

Файл з назвою **точно**:
```
selfie_segmentation.tflite
```

## 📥 Де скачати (MediaPipe):

**Варіант 1 - Пряме посилання (найпростіше):**
```
https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
```

Просто відкрийте це посилання у браузері - файл скачається автоматично.

**Варіант 2 - Альтернативне посилання:**
```
https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float32/latest/selfie_segmenter.tflite
```

## 📁 Куди покласти:

Після завантаження:
1. **Перейменуйте** файл на `selfie_segmentation.tflite` (якщо він називається інакше)
2. **Покладіть** у цю папку (де зараз цей README)
3. **Результат:** `app/src/main/assets/models/selfie_segmentation.tflite`

## 🔨 Після додавання:

1. В Android Studio: `Build → Rebuild Project`
2. Запустіть додаток
3. Спробуйте функцію Background

## 🔍 Перевірка:

У цій папці мають бути файли:
- ✅ `selfie_segmentation.tflite` (НОВИЙ, ~1-2 MB)
- ✅ `style_predict_v2.tflite` (існуючий)
- ✅ `style_transfer_v2.tflite` (існуючий)

## ❓ Детальні інструкції:

- `/BACKGROUND_MODEL_SETUP.md` - повна інструкція
- `/QUICK_FIX.md` - швидке виправлення
- `/DEEPLAB_V3_INSTRUCTIONS.md` - чому DeepLab не працює
