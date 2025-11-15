# Інструкції для DeepLab v3

## ⚠️ ВАЖЛИВО: DeepLab v3 має інший формат!

DeepLab v3 з TensorFlow Hub має інший формат входу/виходу, ніж очікує код. 

### Що скачати з TFHub:

На сторінці https://tfhub.dev/tensorflow/lite-model/deeplabv3/1/metadata/2 є:

1. **Default** - це сама модель (.tflite файл) - ЦЕ ПОТРІБНО СКАЧАТИ
2. **Metadata** - це JSON файл з описом - це НЕ потрібно

**Скачайте файл із секції "Default"**

### Проблема з DeepLab v3:

DeepLab v3 повертає **класифікацію для кожного пікселя** (20+ класів об'єктів), а не бінарну маску людина/фон.

Формат виходу: `[1, 257, 257, 21]` - де 21 це кількість класів

### ✅ РЕКОМЕНДАЦІЯ: Використовуйте MediaPipe замість DeepLab!

**Чому MediaPipe краща для цього випадку:**
- Спеціально розроблена для селфі сегментації
- Повертає просту маску людина/фон
- Швидша на мобільних пристроях
- Менший розмір файлу

## Як завантажити MediaPipe (РЕКОМЕНДОВАНО):

### Варіант 1: Пряме посилання
```
https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
```

### Варіант 2: З GitHub
1. Перейдіть на https://github.com/google-ai-edge/mediapipe/tree/master/mediapipe/modules/selfie_segmentation
2. Знайдіть файл `selfie_segmentation.tflite` або `selfie_segmentation_landscape.tflite`
3. Натисніть "Download" або "Raw" щоб скачати

### Варіант 3: MediaPipe Models
```
https://developers.google.com/mediapipe/solutions/vision/image_segmenter#models
```

## Після завантаження:

1. **Перейменуйте файл** на точно:
   ```
   selfie_segmentation.tflite
   ```

2. **Покладіть у папку:**
   ```
   app/src/main/assets/models/selfie_segmentation.tflite
   ```

3. **Rebuild проект:**
   - У Android Studio: `Build → Rebuild Project`

4. **Запустіть додаток і спробуйте:**
   - Відкрийте фото
   - Виберіть інструмент "Background"
   - Тапніть на будь-яку з трьох опцій
   - Має з'явитися індикатор завантаження (крутиться колесо)
   - Через 2-5 секунд має з'явитися результат

## Як перевірити логи:

1. Відкрийте **Logcat** у Android Studio
2. Знайдіть фільтр і введіть: `BackgroundSegmentation`
3. Запустіть обробку фону
4. Ви побачите:
   - ✅ "Model loaded successfully!" - якщо модель завантажилась
   - ❌ "MODEL FILE NOT FOUND" - якщо файл не знайдено
   - Інформацію про розміри входу/виходу моделі

## Якщо все одно хочете використати DeepLab v3:

Потрібно модифікувати `BackgroundSegmentationModel.kt`:
1. Змінити обробку виходу моделі (21 клас → 1 маска)
2. Додати логіку вибору класу "людина" (зазвичай клас 15)
3. Налаштувати розмір входу (257x257 замість 256x256)

Це складніше і повільніше. **Рекомендую MediaPipe!**

## Розміри моделей:

| Модель | Розмір | Швидкість | Для селфі |
|--------|--------|-----------|-----------|
| MediaPipe Float16 | ~1 MB | ⚡⚡⚡ Дуже швидка | ✅ Так |
| MediaPipe Float32 | ~2 MB | ⚡⚡⚡ Дуже швидка | ✅ Так |
| DeepLab v3 | ~2.7 MB | ⚡⚡ Швидка | ❌ Для всіх об'єктів |

## Перевірка, що модель на місці:

Відкрийте папку в провіднику:
```
app/src/main/assets/models/
```

Там мають бути файли:
- ✅ `selfie_segmentation.tflite` - НОВИЙ ФАЙЛ
- ✅ `style_predict_v2.tflite` - існуючий
- ✅ `style_transfer_v2.tflite` - існуючий

## Що робити після додавання моделі:

1. **Clean Build:**
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. **Перезапустіть додаток**

3. **Спробуйте обробку:**
   - Тапніть на картинку → має з'явитися крутиться колесо
   - Зачекайте 2-5 секунд
   - Має з'явитися Toast повідомлення

## Якщо нічого не відбувається:

1. Перевірте Logcat (фільтр: `PhotoEditor` або `BackgroundSegmentation`)
2. Переконайтесь, що файл називається точно `selfie_segmentation.tflite`
3. Переконайтесь, що він у папці `app/src/main/assets/models/`
4. Зробіть Rebuild Project
5. Надішліть мені логи з Logcat, якщо проблема залишилась
