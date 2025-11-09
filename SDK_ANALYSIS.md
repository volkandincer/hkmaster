# Masterpass SDK iOS vs Android Analysis

## Fonksiyon Listesi ve Karşılaştırma

Bu dokümanda iOS ve Android SDK'larındaki tüm fonksiyonlar, parametreleri, dönüş değerleri ve error handling karşılaştırılacaktır.

## 1. initialize

### iOS SDK

- **Signature**: `MasterPass.initialize(merchantId: Int, terminalGroupId: String?, language: String?, url: String)`
- **Parametreler**:
  - merchantId: Int (required)
  - terminalGroupId: String? (optional)
  - language: String? (optional, default: "en-US")
  - url: String (required)
- **Dönüş Değeri**: Void (no return)
- **Error Handling**: SDK throws exceptions

### Android SDK

- **Signature**: `MasterPass(mId: Long, tGId: String, lan: String, verbose: Boolean, bUrl: String, mSecKey: String?)`
- **Parametreler**:
  - mId: Long (required)
  - tGId: String (required - cannot be empty)
  - lan: String (optional, default: "tr-TR")
  - verbose: Boolean (required)
  - bUrl: String (required)
  - mSecKey: String? (optional)
- **Dönüş Değeri**: MasterPass instance
- **Error Handling**: SDK throws "All parameters needed" if terminalGroupId is empty

### Farklılıklar

1. iOS: terminalGroupId optional, Android: required (cannot be empty)
2. Android: verbose ve merchantSecretKey parametreleri var, iOS: yok
3. iOS: language default "en-US", Android: "tr-TR"
4. iOS: cipherText parametresi var (deprecated?), Android: yok

---

## 2. addCard

### iOS SDK

- **Signature**: `MasterPass.addCard(jToken: String, accountKey: String?, accountKeyType: AccountKeyType?, rrn: String?, card: MPCard, cardAlias: String?, isMsisdnValidatedByMerchant: Bool?, userId: String?, authenticationMethod: AuthType?, completion: @escaping (ServiceError?, MPResponse<CardSaveResponse>?) -> Void)`
- **Parametreler**:
  - jToken: String (required)
  - accountKey: String? (optional)
  - accountKeyType: AccountKeyType? (optional)
  - rrn: String? (optional)
  - card: MPCard (required)
  - cardAlias: String? (optional)
  - isMsisdnValidatedByMerchant: Bool? (optional)
  - userId: String? (optional)
  - authenticationMethod: AuthType? (optional)
- **Dönüş Değeri**: MPResponse<CardSaveResponse> via completion handler
- **Error Handling**: ServiceError in completion handler

### Android SDK

- **Signature**: `MasterPass.addCard(jToken: String, accountKey: String, accountKeyType: AccountKeyType, rrn: String, card: MPCard, cardAlias: String, isMsisdnValidatedByMerchant: Boolean, userId: String, authenticationMethod: AuthType, listener: AddCardListener)`
- **Parametreler**:
  - jToken: String (required)
  - accountKey: String (required - empty string if null)
  - accountKeyType: AccountKeyType (required)
  - rrn: String (required - empty string if null)
  - card: MPCard (required)
  - cardAlias: String (required - empty string if null)
  - isMsisdnValidatedByMerchant: Boolean (required)
  - userId: String (required - empty string if null)
  - authenticationMethod: AuthType (required)
- **Dönüş Değeri**: MPResponse<GeneralAccountResponse> via listener
- **Error Handling**: ServiceError in listener.onFailed()

### Farklılıklar

1. iOS: Parametreler optional, Android: required (empty string fallback)
2. iOS: CardSaveResponse, Android: GeneralAccountResponse
3. iOS: completion handler, Android: listener pattern

---

## 3. accountAccess

### iOS SDK

- **Signature**: `MasterPass.accountAccess(jToken: String, accountKey: String?, accountKeyType: AccountKeyType?, userId: String?, completion: @escaping (ServiceError?, MPResponse<CardResponse>?) -> Void)`
- **Dönüş Değeri**: MPResponse<CardResponse>
- **Error Handling**: ServiceError in completion handler

### Android SDK

- **Signature**: `MasterPass.accountAccess(jToken: String, accountKey: String, accountKeyType: AccountKeyType, userId: String, listener: AccountAccessRequestListener)`
- **Dönüş Değeri**: MPResponse<CardResponse>
- **Error Handling**: ServiceError in listener.onFailed()

### Farklılıklar

1. iOS: Parametreler optional, Android: required (empty string fallback)
2. Response yapısı aynı ama CardResponse.cards farklı (iOS: Array, Android: ArrayList<Object>)

---

## 4. payment

### iOS SDK

- **Signature**: `MasterPass.payment(jToken: String, requestReferenceNo: String?, cvv: MPText, cardAlias: String?, accountKey: String?, amount: String?, orderNo: String?, currencyCode: MPCurrencyCode?, paymentType: PaymentType?, acquirerIcaNumber: String?, installmentCount: Int?, subMerchant: SubMerchant?, rewardList: [Reward]?, orderDetails: OrderDetails?, authenticationMethod: AuthType?, orderProductsDetails: OrderProductsDetails?, buyerDetails: BuyerDetails?, billDetails: BillDetails?, deliveryDetails: DeliveryDetails?, otherDetails: OtherDetails?, secure3DModel: Secure3DModel?, mokaSubDealerDetails: MokaSubDealersDetails?, customParameters: CustomParameters?, additionalParams: [String: String]?, completion: @escaping (ServiceError?, MPResponse<PaymentResponse>?) -> Void)`
- **Dönüş Değeri**: MPResponse<PaymentResponse>
- **Error Handling**: ServiceError in completion handler

### Android SDK

- **Signature**: `MasterPass.payment(jToken: String, requestReferenceNo: String?, cvv: MPText, cardAlias: String?, accountKey: String?, amount: String?, orderNo: String?, currencyCode: MPCurrencyCode?, paymentType: PaymentType?, acquirerIcaNumber: String?, installmentCount: Int?, subMerchant: SubMerchant?, rewardList: ArrayList<Reward>?, orderDetails: OrderDetails?, authenticationMethod: AuthType?, orderProductsDetails: OrderProductsDetails?, buyerDetails: BuyerDetails?, billDetails: BillDetails?, deliveryDetails: DeliveryDetails?, otherDetails: OtherDetails?, secure3DModel: Secure3DModel?, mokaSubDealerDetails: MokaSubDealerDetails?, terminal: Terminal?, listener: PaymentResponseListener)`
- **Dönüş Değeri**: MPResponse<PaymentResponse>
- **Error Handling**: ServiceError in listener.onFailed()

### Farklılıklar

1. iOS: customParameters ve additionalParams var, Android: terminal parametresi var
2. iOS: completion handler, Android: listener pattern
3. Parametre sırası ve optionality farklı olabilir

---

## Genel Farklılıklar

1. **Error Handling Pattern**:

   - iOS: Completion handler with ServiceError? and Response?
   - Android: Listener pattern with onSuccess() and onFailed()

2. **Optional Parameters**:

   - iOS: Genellikle optional parametreler kabul eder
   - Android: Required parametreler empty string olarak gönderilir

3. **Response Types**:

   - Bazı fonksiyonlarda response type'ları farklı (CardSaveResponse vs GeneralAccountResponse)

4. **Enum Handling**:

   - Her iki platformda enum'lar var ama default değerler farklı olabilir

5. **Default Values**:
   - iOS: language default "en-US"
   - Android: language default "tr-TR"
