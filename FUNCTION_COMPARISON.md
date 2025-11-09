# iOS ve Android Fonksiyon Karşılaştırması

## Android Fonksiyonları (24 adet)

1. `initialize`
2. `addCard`
3. `linkAccountToMerchant`
4. `accountAccess`
5. `removeCard`
6. `updateUserId`
7. `updateUserMsisdn`
8. `addUserId`
9. `verify`
10. `resendOtp`
11. `start3DValidation`
12. `payment`
13. `directPayment`
14. `registerAndPurchase`
15. `qrPayment`
16. `moneySend`
17. `digitalLoan`
18. `recurringOrderRegister`
19. `recurringOrderUpdate`
20. `recurringOrderDelete`
21. `completeRegistration` (Placeholder - SDK'da method bulunamadı)
22. `startLoanValidation` (Placeholder - SDK'da method bulunamadı)

## iOS Fonksiyonları (22 adet)

1. `initialize`
2. `addCard`
3. `linkAccountToMerchant`
4. `accountAccess`
5. `removeCard`
6. `updateUserId`
7. `updateUserMsisdn`
8. `addUserId`
9. `verify`
10. `resendOtp`
11. `start3DValidation`
12. `payment`
13. `directPayment`
14. `registerAndPurchase`
15. `qrPayment`
16. `moneySend`
17. `digitalLoan`
18. `recurringOrderRegister`
19. `recurringOrderUpdate`
20. `recurringOrderDelete`
21. `completeRegistration` (iOS: SDK çağrısı var, Android: Placeholder)
22. `startLoanValidation` (iOS: start3DValidation kullanıyor, Android: Placeholder)

## TypeScript Service Fonksiyonları (22 adet)

1. `initialize`
2. `addCard`
3. `linkAccountToMerchant`
4. `accountAccess`
5. `removeCard`
6. `updateUserId`
7. `updateUserMsisdn`
8. `addUserId`
9. `verify`
10. `resendOtp`
11. `start3DValidation`
12. `payment`
13. `directPayment`
14. `registerAndPurchase`
15. `qrPayment`
16. `moneySend`
17. `digitalLoan`
18. `recurringOrderRegister`
19. `recurringOrderUpdate`
20. `recurringOrderDelete`
21. `completeRegistration`
22. `startLoanValidation`

## Sonuç

✅ **Her iki platformda da aynı fonksiyonlar mevcut!**

- Android: 22 fonksiyon (2 placeholder dahil)
- iOS: 22 fonksiyon (2 placeholder dahil)
- TypeScript: 22 fonksiyon

**Not:** 
- `completeRegistration`: iOS'ta SDK'ya gerçek çağrı yapıyor, Android'de placeholder (SDK'da method bulunamadı)
- `startLoanValidation`: iOS'ta `start3DValidation` metodunu kullanıyor, Android'de placeholder

Detaylı karşılaştırma için `PLACEHOLDER_COMPARISON.md` dosyasına bakın.

