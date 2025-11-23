# Portrait AI Models Setup

## Огляд

Portrait AI функціонал використовує **покращену детекцію шкіри** для автоматичного знаходження обличчя:
- **Детекція шкіри**: Автоматично знаходить область обличчя за кольором шкіри (працює для різних тонів)
- **Адаптивний алгоритм**: Знаходить bounding box навколо всіх пікселів шкіри
- **Fallback**: Якщо детекція не спрацювала (< 5% пікселів), використовує центральну область
- **Без моделі**: Не потребує TFLite моделі, працює швидко та точно

**Примітка**: Тестували SSD MobileNet v1, але вона не спеціалізована на обличчях (тренована на COCO dataset). Skin detection показує кращі результати для Portrait AI.

---

## Необхідна модель

### Face Detection Model - MediaPipe Face Detector

**Використовуємо спеціалізовану модель для детекції обличь**

#### Завантажити модель:

**ВИКОРИСТАЙ ЦЮ МОДЕЛЬ - MediaPipe BlazeFace**

Пряме посилання для завантаження:
```bash
wget https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite -O face_detection.tflite
```

**АБО скачай вручну:**
1. Відкрий: https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite
2. Збережи як `face_detection.tflite`
3. Помісти в `app/src/main/assets/models/face_detection.tflite`

**Характеристики:**
- Input: 128x128x3 RGB (Float32, normalized 0-1)
- Output: [1, 896, 16] - 896 детекцій з 16 значеннями кожна
- Формат: [x, y, w, h, ...keypoints, score]

**Варіант 2: TensorFlow Hub**
- **Джерело**: [TensorFlow Hub - Face Detection](https://tfhub.dev/tensorflow/lite-model/ssd_mobilenet_v1/1/metadata/2)
- **Формат**: TensorFlow Lite (.tflite)
- Завантажте модель та конвертуйте в TFLite формат якщо потрібно

**Варіант 3: Google ML Kit (Альтернатива)**
- **Джерело**: [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection)
- Можна використати вбудовану модель ML Kit, але BlazeFace рекомендується для кращої продуктивності

---

## Інструкції по встановленню

### Крок 1: Створіть директорію для моделей

```bash
mkdir -p app/src/main/assets/models
```

### Крок 2: Завантажте модель

**Використовуючи wget:**
```bash
cd app/src/main/assets/models
wget https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite -O face_detection.tflite
```

**Використовуючи curl:**
```bash
cd app/src/main/assets/models
curl -o face_detection.tflite https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite
```

**Або завантажте вручну:**
1. Відкрийте лінк у браузері
2. Збережіть файл як `face_detection.tflite`
3. Помістіть файл в `app/src/main/assets/models/`

### Крок 3: Перевірте структуру

Переконайтеся, що структура виглядає так:
```
AIVis/
├── app/
│   └── src/
│       └── main/
│           └── assets/
│               └── models/
│                   ├── face_detection.tflite  ← НОВА МОДЕЛЬ
│                   └── selfie_segmentation.tflite (для Background функцій)
```

### Крок 4: Rebuild проекту

```bash
./gradlew clean build
```

---

## Функціонал Portrait AI

### 1. Beauty Mode
- **Що робить**: Згладжування шкіри та корекція кольору
- **Алгоритм**: Bilateral filter для збереження країв при згладжуванні
- **Інтенсивність**: 0-100% (регулюється слайдером)
- **Працює без моделі**: ✅ Так (обробляє все зображення)

### 2. Eye Enhancement
- **Що робить**: Освітлення та підвищення контрасту в області очей
- **Область**: Верхні 40% обличчя
- **Інтенсивність**: 0-100% (до +30% яскравості)
- **Працює без моделі**: ✅ Так (використовує центральну область)

### 3. Face Blur
- **Що робить**: Розмиття області обличчя
- **Алгоритм**: Box blur з регульованим радіусом
- **Інтенсивність**: 0-100% (контролює радіус розмиття)
- **Працює без моделі**: ✅ Так (розмиває центральну область)

---

## Технічні деталі

### Вхідні параметри моделі
- **Input shape**: `[1, 128, 128, 3]` або `[1, 192, 192, 3]` (визначається автоматично)
- **Input type**: Float32
- **Normalization**: RGB values / 255.0 (0.0 - 1.0)

### Вихідні параметри моделі
- **Output**: Bounding boxes та confidence scores
- **Format**: `[x, y, width, height, confidence, ...]`
- **Threshold**: Confidence > 0.5

### Smart Fallback режим
Система використовує інтелектуальний fallback:
1. **Детекція шкіри**: Сканує зображення для знаходження пікселів кольору шкіри
2. **Bounding box**: Створює прямокутник навколо знайденої області
3. **Центральний fallback**: Якщо детекція не спрацювала, використовує верхню центральну область (35% ширини, 25% висоти)
4. Працює для різних тонів шкіри

---

## Перевірка роботи

### Логи для перевірки

Відкрийте Logcat в Android Studio та фільтруйте по тегу `PortraitProcessor`:

**Успішна робота з детекцією шкіри:**
```
D/PortraitProcessor: Loading face detection model from: models/face_detection.tflite
D/PortraitProcessor: ✅ Face detection model loaded successfully!
D/PortraitProcessor: BlazeFace output requires anchor decoding - using smart fallback instead
D/PortraitProcessor: Using smart fallback face detection
D/PortraitProcessor: Skin-based face region: Rect(120, 80 - 380, 420) (15234 skin pixels)
D/PortraitProcessor: Applying beauty mode with intensity: 0.5
D/PortraitProcessor: Beauty mode applied successfully
```

**Модель не знайдена (також працює):**
```
E/PortraitProcessor: ❌ MODEL FILE NOT FOUND: models/face_detection.tflite
W/PortraitProcessor: ⚠️ Will use algorithmic fallback instead
D/PortraitProcessor: Using smart fallback face detection
D/PortraitProcessor: Skin-based face region: Rect(...)
```

### Тестування функцій

1. Відкрийте зображення з обличчям в редакторі
2. Виберіть інструмент "Portrait AI" (іконка з обличчям)
3. Спробуйте кожну функцію:
   - **Beauty Mode**: Має згладити шкіру
   - **Eye Enhancement**: Має освітлити очі
   - **Face Blur**: Має розмити обличчя
4. Регулюйте інтенсивність слайдером для кожної функції

---

## Troubleshooting

### Проблема: "Model file not found"
**Рішення:**
1. Перевірте, що файл `face_detection.tflite` існує в `app/src/main/assets/models/`
2. Зробіть Clean & Rebuild проекту
3. Переконайтеся, що назва файлу точно `face_detection.tflite` (без додаткових розширень)

### Проблема: Функції не працюють
**Рішення:**
1. Перевірте логи на помилки
2. Переконайтеся, що зображення містить обличчя
3. Спробуйте збільшити інтенсивність слайдера
4. Функції працюють навіть без моделі (fallback режим)

### Проблема: Повільна обробка
**Рішення:**
1. Модель оптимізована для швидкості, але великі зображення можуть обробляться довше
2. Розгляньте можливість зменшення розміру зображення перед обробкою
3. Використовуйте XNNPACK delegation (вже увімкнено в коді)

---

## Альтернативні моделі (опціонально)

Якщо BlazeFace не підходить, можна спробувати:

### MTCNN Face Detection
- **Джерело**: [MTCNN TFLite](https://github.com/kby-ai/FaceLivenessDetection-Android)
- **Переваги**: Висока точність, детекція landmarks
- **Недоліки**: Повільніша за BlazeFace

### YuNet Face Detection
- **Джерело**: [OpenCV YuNet](https://github.com/opencv/opencv_zoo/tree/master/models/face_detection_yunet)
- **Переваги**: Дуже швидка
- **Недоліки**: Потрібна конвертація в TFLite

---

## Підсумок

✅ **Завантажте**: `blaze_face_short_range.tflite` з MediaPipe  
✅ **Перейменуйте**: на `face_detection.tflite`  
✅ **Помістіть**: в `app/src/main/assets/models/`  
✅ **Rebuild**: проект  
✅ **Тестуйте**: Portrait AI функції в додатку  

**Примітка**: Додаток працює навіть без моделі, використовуючи алгоритмічний fallback!
