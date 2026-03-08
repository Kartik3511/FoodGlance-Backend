# FoodGlance Backend

Spring Boot REST API for the FoodGlance AI food calorie tracker app.

---

## What It Does

1. Accepts a food photo from the Android app
2. Sends it to **Google Cloud Vision API** to identify the food
3. Queries **USDA FoodData Central API** for nutrition data
4. Returns food name + calories, protein, carbs, fat back to the app

---

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/detect-food` | Upload food image → get nutrition |
| `GET` | `/nutrition?food={name}` | Get nutrition for a known food name |
| `GET` | `/foods/search?q={query}` | Search food suggestions (top 5) |
| `GET` | `/health` | Health check → `{ "status": "UP" }` |
| `GET` | `/version` | Deployment version → `FoodGlance Backend v1.0` |

### POST `/detect-food`

- **Body:** `multipart/form-data` with field `image`
- **Max size:** 5MB (returns HTTP 413 if exceeded)
- **Rate limit:** 10 requests/minute per IP (returns HTTP 429 if exceeded)

**Response:**
```json
{
  "foodName": "Banana",
  "calories": "89",
  "protein": "1",
  "carbs": "23",
  "fat": "0",
  "confidence": "98%",
  "originalImageSizeKB": 2400,
  "compressedImageSizeKB": 320,
  "compressionRatio": "86%",
  "compressionTimeMs": 45,
  "visionTimeMs": 820,
  "nutritionTimeMs": 310,
  "totalTimeMs": 1175
}
```

### GET `/foods/search?q=banana`

**Response:**
```json
[
  { "food_name": "Banana", "calories": "89", "protein": "1", "carbs": "23", "fat": "0" },
  ...
]
```

---

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Google Cloud Vision API** — food recognition from image
- **USDA FoodData Central API** — free nutrition database
- **Bucket4j** — rate limiting (10 req/min per IP)
- **Spring Cache** — in-memory caching for repeated food queries

---

## Project Structure

```
src/main/java/com/kartik/foodglance/
├── FoodGlanceApplication.java      # Entry point, cache enabled
├── controller/
│   └── FoodController.java         # All REST endpoints
├── service/
│   ├── VisionService.java          # Google Vision API calls
│   ├── NutritionService.java       # USDA API calls + caching
│   └── ImageCompressionService.java # Resize + JPEG compress before Vision
├── filter/
│   └── RateLimitFilter.java        # Bucket4j 10 req/min per IP
└── model/
    ├── FoodResponse.java           # Main API response
    ├── FoodSearchResult.java       # Search result item
    ├── CompressionResult.java      # Compression metadata
    ├── NutritionData.java          # Internal nutrition holder
    ├── VisionResult.java           # Internal vision result holder
    └── ErrorResponse.java          # Error JSON response
```

---

## Local Setup

### 1. Clone and enter the backend folder
```bash
git clone https://github.com/Kartik3511/FoodGlance-Backend.git
cd FoodGlance-Backend
```

### 2. Create a `.env` file
```env
GOOGLE_VISION_API_KEY=your_google_vision_key_here
USDA_API_KEY=your_usda_key_here
```

> Get a free USDA key at: https://fdc.nal.usda.gov/api-guide.html
> Google Vision key from: https://console.cloud.google.com

### 3. Run the app
```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`

---

## Deployment (Railway)

The app is deployed at:
```
https://foodglance-backend-production.up.railway.app
```

**Environment variables set on Railway:**
- `GOOGLE_VISION_API_KEY`
- `USDA_API_KEY`
- `PORT` (set automatically by Railway)

`application.properties` is committed with `${VAR}` placeholders only — no secrets in code.

---

## Image Compression

Before sending to Vision API, images are:
- Resized to max **800px width** (aspect ratio preserved)
- JPEG compressed to **70% quality**
- This reduces Vision API costs and speeds up detection

---

## Caching

Nutrition lookups are cached in memory using Spring Cache:
- `"nutrition"` cache — for `getNutrition()` calls
- `"food-search"` cache — for `searchFoods()` calls

Same food queried twice → second call returns instantly, no USDA API hit.
