# Masterpass SDK Fonksiyon İnceleme Raporu

Bu dokümanda her fonksiyon için SDK imzaları, TypeScript parametreleri ve response mapping'leri kontrol edilmiştir.

## 1. initialize ✅

### Android SDK
- **Constructor**: `MasterPass(mId: Long, tGId: String, lan: String, verbose: Boolean, bUrl: String)`
- **Parametreler**: merchantId, terminalGroupId (required), language, url, verbose, merchantSecretKey (not used in SDK)

### iOS SDK
- **Method**: `MasterPass.initialize(merchantId: Int, terminalGroupId: String?, language: String?, url: String, cipherText: String?)`
- **Parametreler**: merchantId, terminalGroupId, language, url, cipherText

### TypeScript
- ✅ Parametreler doğru gönderiliyor
- ✅ Platform-specific parametreler doğru handle ediliyor

### Response
- ✅ Her iki platform da aynı yapıda response döndürüyor

---

## 2. addCard

### Android SDK
- **Method**: `addCard(jToken: String, accountKey: String, accountKeyType: AccountKeyType, rrn: String, card: MPCard, cardAlias: String, isMsisdnValidatedByMerchant: Boolean, userId: String, authenticationMethod: AuthType, listener: AddCardListener)`
- **Response**: `MPResponse<GeneralAccountResponse>`
  - GeneralAccountResponse: retrievalReferenceNumber, responseCode, description, token

### iOS SDK
- **Method**: `MasterPass.addCard(_:accountKey:accountKeyType:rrn:userId:card:cardAlias:isMsisdnValidatedByMerchant:authenticationMethod:_:_:)`
- **Response**: `MPResponse<GeneralResponseWith3D>`
  - GeneralResponseWith3D: token, retrievalReferenceNumber, responseCode, resultDescription, url3d, url3dSuccess, url3dFail

### TypeScript
- ✅ Tüm parametreler doğru gönderiliyor
- ✅ Card validation yapılıyor

### Response Mapping
- ✅ Android: GeneralAccountResponse alanları doğru map ediliyor
- ✅ iOS: GeneralResponseWith3D alanları doğru map ediliyor
- ✅ Platformlar arası uyum için eksik alanlar null olarak ekleniyor

---

## Kontrol Edilecek Fonksiyonlar

1. linkAccountToMerchant
2. accountAccess
3. removeCard
4. updateUserId
5. updateUserMsisdn
6. addUserId
7. verify
8. resendOtp
9. start3DValidation
10. payment
11. directPayment
12. registerAndPurchase
13. qrPayment
14. moneySend
15. digitalLoan
16. recurringOrderRegister
17. recurringOrderUpdate
18. recurringOrderDelete
19. completeRegistration
20. startLoanValidation

