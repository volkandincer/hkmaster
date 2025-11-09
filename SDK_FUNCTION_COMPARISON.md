# Masterpass SDK iOS vs Android Fonksiyon Karşılaştırması

Bu dokümanda her fonksiyon için iOS ve Android SDK'larındaki farklılıklar detaylı olarak incelenmiştir.

## Fonksiyon Listesi

1. initialize
2. addCard
3. linkAccountToMerchant
4. accountAccess
5. removeCard
6. updateUserId
7. updateUserMsisdn
8. addUserId
9. verify
10. resendOtp
11. start3DValidation
12. payment
13. directPayment
14. registerAndPurchase
15. qrPayment
16. moneySend
17. completeRegistration
18. digitalLoan
19. startLoanValidation
20. recurringOrderRegister
21. recurringOrderUpdate
22. recurringOrderDelete

## Genel Farklılıklar

### 1. Error Handling Pattern

- **iOS**: Completion handler pattern `(ServiceError?, MPResponse<T>?) -> Void`
- **Android**: Listener pattern with `onSuccess()` and `onFailed()` methods

### 2. Optional Parameters

- **iOS**: Parametreler genellikle optional (`String?`)
- **Android**: Parametreler required ama empty string fallback kullanılıyor

### 3. Response Types

- Bazı fonksiyonlarda response type'ları farklı:
  - iOS: `GeneralResponseWith3D` (addCard)
  - Android: `GeneralAccountResponse` (addCard)

### 4. Default Values

- **iOS**: language default "en-US"
- **Android**: language default "tr-TR"

### 5. Exception Handling

- **iOS**: Exception response içinde `response.exception` olarak gelir
- **Android**: Exception hem `onSuccess` içinde `response.exception` hem de `onFailed` callback'inde gelebilir
