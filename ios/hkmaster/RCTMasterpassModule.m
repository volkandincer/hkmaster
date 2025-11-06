#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(MasterpassModule, RCTEventEmitter)

RCT_EXTERN_METHOD(initialize:(NSNumber *)merchantId
                  terminalGroupId:(NSString *)terminalGroupId
                  language:(NSString *)language
                  url:(NSString *)url
                  cipherText:(NSString *)cipherText
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(addCard:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  accountKeyType:(NSString *)accountKeyType
                  rrn:(NSString *)rrn
                  userId:(NSString *)userId
                  card:(NSDictionary *)card
                  cardAlias:(NSString *)cardAlias
                  isMsisdnValidatedByMerchant:(NSNumber *)isMsisdnValidatedByMerchant
                  authenticationMethod:(NSString *)authenticationMethod
                  additionalParams:(NSDictionary *)additionalParams
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(linkAccountToMerchant:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(accountAccess:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  accountKeyType:(NSString *)accountKeyType
                  userId:(NSString *)userId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(removeCard:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  cardAlias:(NSString *)cardAlias
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(updateUserId:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  currentUserId:(NSString *)currentUserId
                  newUserId:(NSString *)newUserId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(updateUserMsisdn:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  newMsisdn:(NSString *)newMsisdn
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(addUserId:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  currentUserId:(NSString *)currentUserId
                  newUserId:(NSString *)newUserId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(recurringOrderRegister:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  cardAlias:(NSString *)cardAlias
                  productId:(NSString *)productId
                  amountLimit:(NSString *)amountLimit
                  expireDate:(NSString *)expireDate
                  authenticationMethod:(NSString *)authenticationMethod
                  rrn:(NSString *)rrn
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(recurringOrderUpdate:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  cardAlias:(NSString *)cardAlias
                  productId:(NSString *)productId
                  amountLimit:(NSString *)amountLimit
                  expireDate:(NSString *)expireDate
                  rrn:(NSString *)rrn
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(recurringOrderDelete:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  accountChangedEventName:(NSString *)accountChangedEventName
                  cardAlias:(NSString *)cardAlias
                  productId:(NSString *)productId
                  authenticationMethod:(NSString *)authenticationMethod
                  rrn:(NSString *)rrn
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(verify:(NSString *)jToken
                  otp:(NSString *)otp
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(resendOtp:(NSString *)jToken
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(start3DValidation:(NSString *)jToken
                  returnURL:(NSString *)returnURL
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(payment:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(directPayment:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(registerAndPurchase:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(qrPayment:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(moneySend:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(completeRegistration:(NSString *)jToken
                  accountKey:(NSString *)accountKey
                  accountAlias:(NSString *)accountAlias
                  isMsisdnValidatedByMerchant:(NSNumber *)isMsisdnValidatedByMerchant
                  responseToken:(NSString *)responseToken
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(digitalLoan:(NSDictionary *)params
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(startLoanValidation:(NSString *)jToken
                  returnURL:(NSString *)returnURL
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end

