# TODO: Android start3DValidation Implementation

## Durum
- ✅ iOS: Tam implementasyon var (`MPWebView` ile)
- ⏳ Android: Placeholder implementasyon (SDK component'leri mevcut ama tam implementasyon yapılmadı)

## Bulgular

### Android SDK'da Mevcut Component'ler:
1. ✅ `Transaction3DListener` - 3D Secure validation için listener
2. ✅ `MPWebView` - WebView component
3. ✅ `MPWebView.loadUrl(Transaction3DListener)` - 3D Secure URL yükleme metodu
4. ✅ `ValidateTransaction3DResult` - 3D validation sonucu

### Eksik:
- ❌ `MasterPass` class'ında `start3DValidation` metodu yok
- ⚠️ 3D Secure URL payment response'tan gelmeli (`paymentRequest` veya `directPayment`)

## Implementasyon Planı

### Adım 1: Payment Akışını Implement Et
- `paymentRequest` veya `directPayment` metodunu implement et
- Response'tan 3D Secure URL'i al

### Adım 2: start3DValidation Implementasyonu
```kotlin
@ReactMethod
fun start3DValidation(jToken: String, returnURL: String?, threeDSecureURL: String?, promise: Promise) {
  try {
    val activity = reactApplicationContext.currentActivity
    if (activity == null) {
      promise.reject("ERROR", "Activity not available", null)
      return
    }
    
    // Create MPWebView instance
    val webView = MPWebView(activity)
    
    // Set Transaction3DListener
    webView.callback = object : Transaction3DListener {
      override fun onSuccess(result: ValidateTransaction3DResult) {
        // Handle success
        val responseMap = Arguments.createMap()
        responseMap.putInt("statusCode", 200)
        responseMap.putString("message", "3D Validation successful")
        responseMap.putString("token", result.token)
        
        val resultMap = Arguments.createMap()
        resultMap.putString("token", result.token)
        resultMap.putString("status", "success")
        responseMap.putMap("result", resultMap)
        
        promise.resolve(responseMap)
      }
      
      override fun onServiceResponse(response: ServiceResponse) {
        // Handle service response
      }
      
      override fun onServiceError(error: ServiceError) {
        // Handle error
      }
      
      override fun onInternalError(message: String) {
        promise.reject("ERROR", "Internal error: $message", null)
      }
    }
    
    // Load 3D Secure URL
    if (threeDSecureURL != null && threeDSecureURL.isNotEmpty()) {
      // MPWebView.loadUrl(Transaction3DListener) - URL'i yükle
      webView.loadUrl(webView.callback)
    } else {
      promise.reject("ERROR", "3D Secure URL is required from payment response", null)
    }
  } catch (e: Exception) {
    promise.reject("ERROR", e.message ?: "Unknown error", e)
  }
}
```

### Adım 3: TypeScript Service Güncellemesi
```typescript
async start3DValidation(
  jToken: string,
  returnURL?: string,
  threeDSecureURL?: string, // Payment response'tan gelen URL
): Promise<MasterpassResponse> {
  // ...
}
```

## Notlar
- 3D Secure URL genellikle `paymentRequest` veya `directPayment` response'undan gelir
- `returnURL` payment sonrası dönüş URL'i (bizim gönderdiğimiz)
- `threeDSecureURL` 3D Secure doğrulama URL'i (payment response'tan gelen)
- Bridge başka bir projede kullanılacak, bu yüzden implementasyon şimdilik TODO olarak bırakıldı

## İlgili Dosyalar
- `android/app/src/main/java/com/hkmaster/MasterpassModule.kt` (satır 1474-1503)
- `services/MasterpassService.ts` (satır 406-440)
- `screens/MasterpassTestScreen.tsx` (satır 346-369)

## Tarih
- Oluşturulma: 2025-01-09
- Son Güncelleme: 2025-01-09

