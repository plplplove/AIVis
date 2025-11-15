# Налаштування моделі для обробки фону

## Яку модель використовувати

Для роботи функції обробки фону потрібна **модель сегментації** у форматі TensorFlow Lite (.tflite).

Рекомендовані варіанти:

### 1. MediaPipe Selfie Segmentation (Рекомендується) ⭐

**Чому саме ця модель:**
- Оптимізована для мобільних пристроїв
- Швидка обробка (real-time на більшості телефонів)
- Добра якість для портретів
- Компактний розмір (~1-2 MB)

**Де скачати:**
1. Офіційний репозиторій MediaPipe:
   ```
   https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
   ```

2. Або з GitHub MediaPipe Models:
   ```
   https://github.com/google/mediapipe/tree/master/mediapipe/models
   ```

**Альтернативна назва файлу:** `selfie_segmentation_landscape.tflite` або `selfie_multiclass_256x256.tflite`

### 2. DeepLab v3 (Альтернатива)

Якщо MediaPipe не підходить, можна використати DeepLab:
- Більш точна сегментація
- Працює з різними об'єктами, не тільки людьми
- Трохи повільніша
- Розмір: ~2-3 MB

**Де скачати:**
```
https://tfhub.dev/tensorflow/lite-model/deeplabv3/1/metadata/2
```

## Куди помістити модель

1. **Створіть папку** (якщо її ще немає):
   ```
   app/src/main/assets/models/
   ```

2. **Перейменуйте файл** на:
   ```
   selfie_segmentation.tflite
   ```

3. **Покладіть файл** у:
   ```
   app/src/main/assets/models/selfie_segmentation.tflite
   ```

## Структура папки assets

Після додавання моделі структура має виглядати так:

```
app/src/main/assets/
├── models/
│   ├── selfie_segmentation.tflite    ← НОВА МОДЕЛЬ
│   ├── style_predict_v2.tflite       (існуюча)
│   └── style_transfer_v2.tflite      (існуюча)
└── (інші файли assets)
```

## Тестування

Після додавання моделі:

1. **Синхронізуйте проект** у Android Studio
2. **Запустіть додаток**
3. **Відкрийте фото** в редакторі
4. **Виберіть інструмент "Background"**
5. **Спробуйте всі три опції:**
   - Remove Background (Прибрати фон)
   - Blur Background (Розмити фон)  
   - Replace Background (Замінити фон)

## Як працює модель

Модель аналізує зображення і створює **маску сегментації**:
- Значення 1.0 = передній план (людина/об'єкт)
- Значення 0.0 = фон

На основі цієї маски код виконує:
- **Remove:** Робить фон прозорим (alpha = 0)
- **Blur:** Розмиває тільки фонову частину
- **Replace:** Замінює фон на білий колір (або інше зображення)

## Розмір моделі

| Модель | Розмір | Швидкість | Якість |
|--------|--------|-----------|--------|
| MediaPipe Float16 | ~1 MB | Висока | Добра |
| MediaPipe Float32 | ~2 MB | Висока | Відмінна |
| DeepLab v3 | ~3 MB | Середня | Відмінна |

**Рекомендація:** Починайте з MediaPipe Float16 для балансу між розміром і якістю.

## Можливі помилки

### "Model file not found"
- Перевірте, що файл називається точно `selfie_segmentation.tflite`
- Перевірте, що файл знаходиться в `app/src/main/assets/models/`
- Rebuild проект (Build → Rebuild Project)

### "Model initialization failed"
- Модель може бути пошкоджена при завантаженні - перезавантажте
- Переконайтесь, що це TFLite модель сегментації, а не класифікації

### Низька якість сегментації
- Спробуйте модель з вищою точністю (Float32 замість Float16)
- Переконайтесь, що фото має достатню якість
- Модель працює краще з портретами на простому фоні

## Підтримка

Якщо виникли проблеми:
1. Перевірте логи в Android Studio (пошук по тегу "BackgroundSegmentation")
2. Переконайтесь, що TensorFlow Lite dependency встановлено (вже є в build.gradle)
3. Спробуйте альтернативну модель

## Корисні посилання

- [MediaPipe Models](https://developers.google.com/mediapipe/solutions/vision/image_segmenter)
- [TensorFlow Hub - Segmentation Models](https://tfhub.dev/s?module-type=image-segmentation)
- [DeepLab Models](https://github.com/tensorflow/models/tree/master/research/deeplab)
