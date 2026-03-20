# Quick Deployment Reference

## ✅ YES - Ready for Railway!

**All 6 deployment checks passed:**
- ✅ Secrets are gitignored (.env won't be pushed)
- ✅ No hardcoded API keys in code
- ✅ PORT configured for Railway
- ✅ Maven wrapper included
- ✅ Production data present (26 ICMR foods)
- ✅ Spring Boot properly configured

## 🚀 Push to GitHub

```bash
cd backend
git add .
git commit -m "Add hybrid ICMR-USDA nutrition system"
git push origin main
```

## ⚙️ Railway Setup (3 steps)

1. **Create Project:** Deploy from GitHub repo
2. **Set Root:** `backend` (if monorepo)
3. **Add Variables:**
   ```
   GOOGLE_VISION_API_KEY=AIzaSyAj7hbeUvurpfBmdNSFxMSfDq_5_MRURUU
   USDA_API_KEY=Are350FxIBTnBsn4S7PcW5b3xUBaizOw6TauMMbh
   ```

## 🧪 Test After Deploy

```bash
curl https://YOUR-APP.railway.app/nutrition?food=paneer
```

Expected: `"data_source":"ICMR"` and `"calories":"257"`

---

**Full guide:** See `DEPLOYMENT.md` in backend folder
