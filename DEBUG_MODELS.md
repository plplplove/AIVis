# Debug AI Models - Діагностика

## Що я додав:

### Детальне логування в StyleTransferModel:

1. **Завантаження моделей:**
   - Підтвердження завантаження кожної моделі
   - Розміри input/output тензорів
   - Кількість входів/виходів

2. **Style Prediction:**
   - Розмір style image
   - Розмір input buffer
   - Розмір та перші значення output bottleneck

3. **Style Transfer:**
   - Розміри content bitmap
   - Розміри input buffers
   - Статус inference
   - Успіх або помилка

## Як переглянути логи:

### Варіант 1: Android Studio Logcat
1. Відкрий Android Studio
2. Знизу вкладка "Logcat"
3. Фільтр: `StyleTransfer`
4. Запусти додаток і вибери AI Style

### Варіант 2: Terminal (adb)
```bash
# Очисти попередні логи
adb logcat -c

# Дивись логи в реальному часі
adb logcat -s StyleTransfer:D

# Або всі логи з фільтром
adb logcat | grep StyleTransfer
```

## Що шукати в логах:

### ✅ Успішне завантаження:
```
StyleTransfer: Loading style_predict model...
StyleTransfer: style_predict model loaded successfully
StyleTransfer: Predict input shape: [1, 256, 256, 3]
StyleTransfer: Predict output shape: [1, 1, 1, 100]
StyleTransfer: Loading style_transfer model...
StyleTransfer: style_transfer model loaded successfully
StyleTransfer: Transfer input[0] shape: [1, 384, 384, 3]
StyleTransfer: Transfer input[1] shape: [1, 1, 1, 100]
StyleTransfer: Transfer output shape: [1, 384, 384, 3]
```

### ❌ Проблема з моделями:
```
StyleTransfer: Error initializing model
java.io.FileNotFoundException: models/style_predict.tflite
```
**Рішення**: Перевір що файли в правильній директорії

### ❌ Неправильні розміри:
```
StyleTransfer: Predict input shape: [1, 299, 299, 3]  ← Має бути 256!
```
**Рішення**: Неправильна модель, потрібна Magenta v1-256

### ❌ Помилка inference:
```
StyleTransfer: applyStyle: ERROR - falling back
java.lang.IllegalArgumentException: Cannot convert between a TensorFlowLite tensor with shape [1,1,1,100] and a Java object with shape [1,1,1,100]
```
**Рішення**: Неправильна структура output array

## Можливі проблеми:

### 1. Файли не знайдено
**Симптом**: `FileNotFoundException`
**Перевірка**:
```bash
# Перевір структуру APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E "models|styles"
```
**Має бути**:
```
assets/models/style_predict.tflite
assets/models/style_transfer.tflite
assets/styles/oil_painting.jpg
assets/styles/watercolor.jpg
...
```

### 2. Неправильна модель
**Симптом**: Розміри не [256, 256, 3] для predict або [384, 384, 3] для transfer
**Рішення**: 
- Переконайся що використовуєш Magenta **v1-256/2**
- Завантаж з: https://tfhub.dev/google/magenta/arbitrary-image-stylization-v1-256/2

### 3. Проблема з тензорами
**Симптом**: `IllegalArgumentException` під час run()
**Можливі причини**:
- Неправильна структура output array
- Неправильний ByteBuffer format
- Неправильна нормалізація [0, 1]

### 4. Out of Memory
**Симптом**: `OutOfMemoryError`
**Рішення**: 
- Зменши `contentImageSize` з 384 до 256
- Додай `android:largeHeap="true"` в AndroidManifest.xml

## Очікувані розміри:

### Style Predict Model:
- **Input**: `[1, 256, 256, 3]` - RGB image, normalized [0, 1]
- **Output**: `[1, 1, 1, 100]` - Style bottleneck vector

### Style Transfer Model:
- **Input 1**: `[1, 384, 384, 3]` - Content image, normalized [0, 1]
- **Input 2**: `[1, 1, 1, 100]` - Style bottleneck
- **Output**: `[1, 384, 384, 3]` - Stylized image, normalized [0, 1]

## Наступні кроки:

1. **Встанови APK** на пристрій
2. **Запусти adb logcat** в терміналі
3. **Відкрий додаток** та вибери зображення
4. **Натисни AI Styles** → Вибери будь-який стиль
5. **Скопіюй всі логи** та покажи мені

Тоді я точно скажу що не так! 🔍
