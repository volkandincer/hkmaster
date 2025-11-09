import Foundation
import React
import Masterpass
import UIKit
import WebKit

@objc(MasterpassModule)
class RCTMasterpassModule: RCTEventEmitter {
  
  override static func requiresMainQueueSetup() -> Bool {
    return false
  }
  
  override func supportedEvents() -> [String]! {
    return []
  }
  
  
  // MARK: - Initialize
  
  @objc func initialize(_ merchantId: NSNumber, terminalGroupId: String?, language: String?, url: String, cipherText: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Normalize URL - ensure trailing slash is present for SDK to append paths correctly
    let normalizedUrl = url.hasSuffix("/") ? url : url + "/"
    
    // Initialize SDK with normalized URL
    MasterPass.initialize(
      merchantId: merchantId.intValue,
      terminalGroupId: terminalGroupId,
      language: language ?? "en-US",
      url: normalizedUrl,
      cipherText: cipherText
    )
    
    // Get SDK version information from framework
    let mpTextBundle = Bundle(for: MPText.self)
    let sdkVersion = (mpTextBundle.infoDictionary?["CFBundleShortVersionString"] as? String) ?? "1.3.7"
    let sdkBuild = (mpTextBundle.infoDictionary?["CFBundleVersion"] as? String) ?? "Unknown"
    
    // Try to get SDK version from Masterpass framework
    var masterpassVersion: String? = nil
    if let masterpassBundle = Bundle(identifier: "com.masterpass.Masterpass") {
      masterpassVersion = masterpassBundle.infoDictionary?["CFBundleShortVersionString"] as? String
    }
    
    // Build response with SDK information
    var response: [String: Any] = [
      "success": true,
      "message": "SDK initialized successfully",
      "merchantId": merchantId.intValue,
      "language": language ?? "en-US",
      "url": normalizedUrl,
    ]
    
    // Handle optional values - use NSNull() to match Android null behavior
    if let terminalGroupId = terminalGroupId, !terminalGroupId.isEmpty {
      response["terminalGroupId"] = terminalGroupId
    } else {
      response["terminalGroupId"] = NSNull()
    }
    
    if let cipherText = cipherText, !cipherText.isEmpty {
      response["cipherText"] = cipherText
    } else {
      response["cipherText"] = NSNull()
    }
    
    // Add SDK version information if available
    if let version = masterpassVersion {
      response["sdkVersion"] = version
    } else {
      response["sdkVersion"] = sdkVersion
    }
    response["sdkBuild"] = sdkBuild
    
    // Add framework path to confirm SDK is loaded (iOS specific)
    let frameworkPath = mpTextBundle.bundlePath
    response["sdkFrameworkPath"] = frameworkPath
    
    resolver(response)
  }
  
  // MARK: - Add Card
  
  @objc func addCard(_ jToken: String, accountKey: String?, accountKeyType: String?, rrn: String?, userId: String?, card: NSDictionary?, cardAlias: String?, isMsisdnValidatedByMerchant: NSNumber?, authenticationMethod: String?, additionalParams: NSDictionary?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Extract card information from dictionary
    var cardNumber: String? = nil
    var expiryDate: String? = nil
    var cvv: String? = nil
    var cardHolderName: String? = nil
    
    if let cardDict = card {
      cardNumber = cardDict["cardNumber"] as? String
      expiryDate = cardDict["expiryDate"] as? String
      cvv = cardDict["cvv"] as? String
      cardHolderName = cardDict["cardHolderName"] as? String
    }
    
    // Validate required fields
    guard let cardNumber = cardNumber, !cardNumber.isEmpty else {
      rejecter("ERROR", "Card number is required", nil)
      return
    }
    
    guard let expiryDate = expiryDate, !expiryDate.isEmpty else {
      rejecter("ERROR", "Expiry date is required", nil)
      return
    }
    
    guard let cvv = cvv, !cvv.isEmpty else {
      rejecter("ERROR", "CVV is required", nil)
      return
    }
    
    // Convert accountKeyType to enum
    // AccountKeyType has .id and .msisdn cases
    let accountKeyTypeEnum: AccountKeyType?
    if let accountKeyTypeStr = accountKeyType {
      accountKeyTypeEnum = AccountKeyType(rawValue: accountKeyTypeStr.lowercased())
    } else {
      accountKeyTypeEnum = nil
    }
    
    // Convert authenticationMethod to AuthType enum if provided
    var authType: AuthType? = nil
    if let authMethod = authenticationMethod {
      authType = AuthType(rawValue: authMethod)
    }
    
    // Convert additionalParams from NSDictionary to [String: String]
    var additionalParamsDict: [String: String]? = nil
    if let params = additionalParams {
      additionalParamsDict = [:]
      for (key, value) in params {
        if let keyStr = key as? String, let valueStr = value as? String {
          additionalParamsDict?[keyStr] = valueStr
        }
      }
    }
    
    // Create MPText instances for card number and CVV
    // SDK requires MPText type to be set correctly (cardNo for card number, cvv for CVV)
    // TypeScript already validates and formats the values, so we just create MPText and set values
    let cardNoMPText = MPText()
    cardNoMPText.type = .cardNo
    cardNoMPText.text = cardNumber
    
    let cvvMPText = MPText()
    cvvMPText.type = .cvv
    cvvMPText.text = cvv
    
    // Create MPCheckboxStateProvider and set it to checked (required by SDK)
    // SDK requires terms and conditions checkbox to be selected
    let checkboxProvider = MPCheckboxStateProvider()
    checkboxProvider.checkStateAction(to: true)
    
    // Create MPCard with correct iOS SDK signature
    // Signature: init(_ cardNo: MPText, _ cardHolder: String?, _ cvv: MPText?, _ date: String, _ checkbox: MPCheckboxStateProvider? = nil)
    let mpCard = MPCard(
      cardNoMPText,
      cardHolderName,
      cvvMPText,
      expiryDate,
      checkboxProvider // checkbox is required - SDK validates terms and conditions
    )
    
    // Call SDK addCard method with completion handler
    // Signature: addCard(_:accountKey:accountKeyType:rrn:userId:card:cardAlias:isMsisdnValidatedByMerchant:authenticationMethod:_:_:)
    MasterPass.addCard(
      jToken,
      accountKey: accountKey ?? "",
      accountKeyType: accountKeyTypeEnum,
      rrn: rrn ?? "",
      userId: userId ?? "",
      card: mpCard,
      cardAlias: cardAlias ?? "",
      isMsisdnValidatedByMerchant: isMsisdnValidatedByMerchant?.boolValue ?? false,
      authenticationMethod: authType,
      additionalParamsDict,
      { (error: ServiceError?, result: MPResponse<GeneralResponseWith3D>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Add Card failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            // ExceptionResponse fields are non-optional
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - GeneralResponseWith3D
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            resultDict["token"] = resultObj.token
            resultDict["retrievalReferenceNumber"] = resultObj.retrievalReferenceNumber
            resultDict["responseCode"] = resultObj.responseCode
            resultDict["resultDescription"] = resultObj.resultDescription
            
            if let url3d = resultObj.url3d {
              resultDict["url3d"] = url3d.absoluteString
            } else {
              resultDict["url3d"] = NSNull()
            }
            
            if let url3dSuccess = resultObj.url3dSuccess {
              resultDict["url3dSuccess"] = url3dSuccess.absoluteString
            } else {
              resultDict["url3dSuccess"] = NSNull()
            }
            
            if let url3dFail = resultObj.url3dFail {
              resultDict["url3dFail"] = url3dFail.absoluteString
            } else {
              resultDict["url3dFail"] = NSNull()
            }
            
            resultDict["jToken"] = jToken
            if let cardAlias = cardAlias {
              resultDict["cardAlias"] = cardAlias
            } else {
              resultDict["cardAlias"] = NSNull()
            }
            
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Add Card failed: No response received", nil)
        }
      }
    )
  }
  
  // Helper method to convert SDK response to dictionary
  private func convertResponseToDictionary(_ response: Any) -> [String: Any]? {
    // This will be implemented based on actual SDK response structure
    // For now, return basic structure
    return ["response": "\(response)"]
  }
  
  // MARK: - Link Account To Merchant
  
  @objc func linkAccountToMerchant(_ jToken: String, accountKey: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK linkAccountToMerchant method with completion handler
    // Signature: linkAccountToMerchant(_:_:_:)
    MasterPass.linkAccountToMerchant(
      jToken,
      accountKey ?? "",
      { (error: ServiceError?, result: MPResponse<LinkToMerchantResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Link Account To Merchant failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            // ExceptionResponse fields are non-optional
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - LinkToMerchantResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            resultDict["token"] = resultObj.token
            resultDict["retrievalReferenceNumber"] = resultObj.retrievalReferenceNumber
            resultDict["responseCode"] = resultObj.responseCode
            resultDict["description"] = resultObj.description
            resultDict["cardIssuerName"] = resultObj.cardIssuerName
            resultDict["maskedPan"] = resultObj.maskedPan
            resultDict["jToken"] = jToken
            if let accountKey = accountKey {
              resultDict["accountKey"] = accountKey
            } else {
              resultDict["accountKey"] = NSNull()
            }
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Link Account To Merchant failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Account Access
  
  @objc func accountAccess(_ jToken: String, accountKey: String?, accountKeyType: String?, userId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Convert accountKeyType to enum
    let accountKeyTypeEnum: AccountKeyType?
    if let accountKeyTypeStr = accountKeyType {
      accountKeyTypeEnum = AccountKeyType(rawValue: accountKeyTypeStr.lowercased())
    } else {
      accountKeyTypeEnum = nil
    }
    
    // Call SDK accountAccess method with completion handler
    MasterPass.accountAccess(
      jToken,
      accountKey: accountKey ?? "",
      accountKeyType: accountKeyTypeEnum,
      userId: userId ?? "",
      { (error: ServiceError?, result: MPResponse<CardResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Account Access failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - CardResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            resultDict["accountKey"] = resultObj.accountKey
            resultDict["accountState"] = resultObj.accountState
            
            // Convert cards array
            var cardsArray: [[String: Any]] = []
            for card in resultObj.cards {
              var cardDict: [String: Any] = [:]
              cardDict["cardAlias"] = card.cardAlias
              cardDict["cardState"] = card.cardState
              cardDict["maskedCardNumber"] = card.maskedCardNumber
              cardDict["uniqueCardNumber"] = card.uniqueCardNumber
              cardDict["cardType"] = card.cardType
              
              if let productName = card.productName {
                cardDict["productName"] = productName
              } else {
                cardDict["productName"] = NSNull()
              }
              
              cardDict["cardBin"] = card.cardBin
              
              if let cardIssuerIcaNumber = card.cardIssuerIcaNumber {
                cardDict["cardIssuerIcaNumber"] = cardIssuerIcaNumber
              } else {
                cardDict["cardIssuerIcaNumber"] = NSNull()
              }
              
              cardDict["cardValidationType"] = card.cardValidationType
              cardDict["isDefaultCard"] = card.isDefaultCard
              cardDict["expireSoon"] = card.expireSoon
              cardDict["isExpired"] = card.isExpired
              cardDict["isMasterpassMember"] = card.isMasterpassMember
              
              cardsArray.append(cardDict)
            }
            resultDict["cards"] = cardsArray
            
            // Convert accountInformation
            var accountInfoDict: [String: Any] = [:]
            accountInfoDict["isAccountLinked"] = resultObj.accountInformation.isAccountLinked
            resultDict["accountInformation"] = accountInfoDict
            
            // Convert recipientCards array (optional)
            if let recipientCards = resultObj.recipientCards {
              var recipientCardsArray: [[String: Any]] = []
              for recipientCard in recipientCards {
                var recipientCardDict: [String: Any] = [:]
                if let alias = recipientCard.alias {
                  recipientCardDict["alias"] = alias
                } else {
                  recipientCardDict["alias"] = NSNull()
                }
                if let encryptedNumber = recipientCard.encryptedNumber {
                  recipientCardDict["encryptedNumber"] = encryptedNumber
                } else {
                  recipientCardDict["encryptedNumber"] = NSNull()
                }
                if let maskedNumber = recipientCard.maskedNumber {
                  recipientCardDict["maskedNumber"] = maskedNumber
                } else {
                  recipientCardDict["maskedNumber"] = NSNull()
                }
                if let uniqueNumber = recipientCard.uniqueNumber {
                  recipientCardDict["uniqueNumber"] = uniqueNumber
                } else {
                  recipientCardDict["uniqueNumber"] = NSNull()
                }
                if let issuerIca = recipientCard.issuerIca {
                  recipientCardDict["issuerIca"] = issuerIca
                } else {
                  recipientCardDict["issuerIca"] = NSNull()
                }
                if let productName = recipientCard.productName {
                  recipientCardDict["productName"] = productName
                } else {
                  recipientCardDict["productName"] = NSNull()
                }
                recipientCardsArray.append(recipientCardDict)
              }
              resultDict["recipientCards"] = recipientCardsArray
            } else {
              resultDict["recipientCards"] = NSNull()
            }
            
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Account Access failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Remove Card
  
  @objc func removeCard(_ jToken: String, accountKey: String?, cardAlias: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK removeCard method with completion handler
    // iOS SDK signature: removeCard(_:accountKey:cardAlias:_:)
    MasterPass.removeCard(
      jToken,
      accountKey ?? "",
      cardAlias ?? "",
      { (error: ServiceError?, result: MPResponse<RemoveCardResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Remove Card failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - RemoveCardResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            // RemoveCardResponse typically contains success confirmation
            // Map any fields from RemoveCardResponse if available
            resultDict["status"] = "success"
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Remove Card failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Update User ID
  
  @objc func updateUserId(_ jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK updateUserId method with completion handler
    // iOS SDK signature: updateUserId(_:accountKey:currentUserId:newUserId:_:)
    MasterPass.updateUserId(
      jToken,
      accountKey ?? "",
      currentUserId ?? "",
      newUserId ?? "",
      { (error: ServiceError?, result: MPResponse<GeneralResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Update User ID failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - GeneralResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            // GeneralResponse typically contains success confirmation
            resultDict["status"] = "success"
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Update User ID failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Update User MSISDN
  
  @objc func updateUserMsisdn(_ jToken: String, accountKey: String?, newMsisdn: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK updateUserMsisdn method with completion handler
    // iOS SDK signature: updateUserMsisdn(_:accountKey:newMsisdn:_:)
    MasterPass.updateUserMsisdn(
      jToken,
      accountKey ?? "",
      newMsisdn ?? "",
      { (error: ServiceError?, result: MPResponse<GeneralResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Update User MSISDN failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - GeneralResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            // GeneralResponse typically contains success confirmation
            resultDict["status"] = "success"
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Update User MSISDN failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Add User ID
  
  @objc func addUserId(_ jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK addUserId method with completion handler
    // iOS SDK signature: addUserId(_:accountKey:currentUserId:newUserId:_:)
    MasterPass.addUserId(
      jToken,
      accountKey ?? "",
      currentUserId ?? "",
      newUserId ?? "",
      { (error: ServiceError?, result: MPResponse<GeneralResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Add User ID failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - GeneralResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            // GeneralResponse typically contains success confirmation
            resultDict["status"] = "success"
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Add User ID failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Recurring Order Register
  
  @objc func recurringOrderRegister(_ jToken: String, accountKey: String?, cardAlias: String?, productId: String?, amountLimit: String?, expireDate: String, authenticationMethod: String?, rrn: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Recurring Order Register - Not implemented yet"])
  }
  
  // MARK: - Recurring Order Update
  
  @objc func recurringOrderUpdate(_ jToken: String, accountKey: String?, cardAlias: String?, productId: String?, amountLimit: String?, expireDate: String, rrn: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Recurring Order Update - Not implemented yet"])
  }
  
  // MARK: - Recurring Order Delete
  
  @objc func recurringOrderDelete(_ jToken: String, accountKey: String?, accountChangedEventName: String?, cardAlias: String?, productId: String?, authenticationMethod: String?, rrn: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Recurring Order Delete - Not implemented yet"])
  }
  
  // MARK: - Verify
  
  @objc func verify(_ jToken: String, otp: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Create MPText instance for OTP on main thread
    // MPText requires type to be set to .otp or .rta
    // MPText is a UIView subclass, so it must be created on main thread
    DispatchQueue.main.async {
      let otpMPText = MPText()
      otpMPText.type = .otp
      otpMPText.text = otp
      
      // Call SDK verify method with completion handler
      // iOS SDK signature: verify(_:otp:_:)
      MasterPass.verify(
        jToken,
        otp: otpMPText,
        { (error: ServiceError?, result: MPResponse<VerifyResponse>?) in
          if let error = error {
            // Handle error
            var errorMessage = error.responseDesc ?? "Verify failed"
            if let responseCode = error.responseCode {
              errorMessage += " (Code: \(responseCode))"
            }
            if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
              errorMessage += " [MD Status: \(mdStatus)]"
            }
            if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
              errorMessage += " [MD Error: \(mdErrorMsg)]"
            }
            rejecter("ERROR", errorMessage, nil)
          } else if let response = result {
            // Handle success response
            var responseDict: [String: Any] = [:]
            
            // MPResponse fields - buildId, statusCode, message are non-optional
            responseDict["statusCode"] = response.statusCode
            responseDict["message"] = response.message
            responseDict["buildId"] = response.buildId
            
            // Optional fields
            if let version = response.version {
              responseDict["version"] = version
            } else {
              responseDict["version"] = NSNull()
            }
            
            if let correlationId = response.correlationId {
              responseDict["correlationId"] = correlationId
            } else {
              responseDict["correlationId"] = NSNull()
            }
            
            if let requestId = response.requestId {
              responseDict["requestId"] = requestId
            } else {
              responseDict["requestId"] = NSNull()
            }
            
            // Check for exception
            if let exception = response.exception {
              var exceptionDict: [String: Any] = [:]
              exceptionDict["level"] = exception.level
              exceptionDict["code"] = exception.code
              exceptionDict["message"] = exception.message
              responseDict["exception"] = exceptionDict
              rejecter("ERROR", exception.message, nil)
              return
            }
            
            // Handle result - VerifyResponse
            if let resultObj = response.result {
              var resultDict: [String: Any] = [:]
              // VerifyResponse typically contains success confirmation
              resultDict["status"] = "success"
              responseDict["result"] = resultDict
            } else {
              responseDict["result"] = NSNull()
            }
            
            resolver(responseDict)
          } else {
            rejecter("ERROR", "Verify failed: No response received", nil)
          }
        }
      )
    }
  }
  
  // MARK: - Resend OTP
  
  @objc func resendOtp(_ jToken: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Call SDK resendOtp method with completion handler
    // iOS SDK signature: resendOtp(_:_:)
    MasterPass.resendOtp(
      jToken,
      { (error: ServiceError?, result: MPResponse<GeneralResponse>?) in
        if let error = error {
          // Handle error
          var errorMessage = error.responseDesc ?? "Resend OTP failed"
          if let responseCode = error.responseCode {
            errorMessage += " (Code: \(responseCode))"
          }
          if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
            errorMessage += " [MD Status: \(mdStatus)]"
          }
          if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
            errorMessage += " [MD Error: \(mdErrorMsg)]"
          }
          rejecter("ERROR", errorMessage, nil)
        } else if let response = result {
          // Handle success response
          var responseDict: [String: Any] = [:]
          
          // MPResponse fields - buildId, statusCode, message are non-optional
          responseDict["statusCode"] = response.statusCode
          responseDict["message"] = response.message
          responseDict["buildId"] = response.buildId
          
          // Optional fields
          if let version = response.version {
            responseDict["version"] = version
          } else {
            responseDict["version"] = NSNull()
          }
          
          if let correlationId = response.correlationId {
            responseDict["correlationId"] = correlationId
          } else {
            responseDict["correlationId"] = NSNull()
          }
          
          if let requestId = response.requestId {
            responseDict["requestId"] = requestId
          } else {
            responseDict["requestId"] = NSNull()
          }
          
          // Check for exception
          if let exception = response.exception {
            var exceptionDict: [String: Any] = [:]
            exceptionDict["level"] = exception.level
            exceptionDict["code"] = exception.code
            exceptionDict["message"] = exception.message
            responseDict["exception"] = exceptionDict
            rejecter("ERROR", exception.message, nil)
            return
          }
          
          // Handle result - GeneralResponse
          if let resultObj = response.result {
            var resultDict: [String: Any] = [:]
            // GeneralResponse typically contains success confirmation
            resultDict["status"] = "success"
            responseDict["result"] = resultDict
          } else {
            responseDict["result"] = NSNull()
          }
          
          resolver(responseDict)
        } else {
          rejecter("ERROR", "Resend OTP failed: No response received", nil)
        }
      }
    )
  }
  
  // MARK: - Start 3D Validation
  
  @objc func start3DValidation(_ jToken: String, returnURL: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Validate returnURL - SDK requires a valid URL, not empty string
    // Even though documentation says it's optional, SDK throws error if empty
    let validReturnURL = returnURL?.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let returnURLValue = validReturnURL, !returnURLValue.isEmpty else {
      rejecter("ERROR", "returnURL is required for 3D Validation", nil)
      return
    }
    
    // Validate URL format
    guard URL(string: returnURLValue) != nil else {
      rejecter("ERROR", "returnURL must be a valid URL format", nil)
      return
    }
    
    // Create MPWebView for 3D Secure validation on main thread
    // iOS SDK requires MPWebView (not WKWebView) for 3D Secure flow
    // MPWebView is a UIView subclass, so it must be created on main thread
    DispatchQueue.main.async {
      let webView = MPWebView()
      
      // Call SDK start3DValidation method with completion handler
      // iOS SDK signature: start3DValidation(_:returnURL:webView:_:)
      // Note: SDK signature differs from documentation - actual signature uses returnURL before webView
      // Completion handler returns Result<Status3D?, MPError>
      // Note: SDK may require 3D Secure URL from payment response, not just returnURL
      MasterPass.start3DValidation(
        jToken,
        returnURL: returnURLValue,
        webView: webView,
        { (result: Result<Status3D?, MPError>) in
          switch result {
          case .success(let status3D):
            // Handle success
            var responseDict: [String: Any] = [:]
            responseDict["statusCode"] = 200
            responseDict["message"] = "Start 3D Validation successful"
            
            if let status3D = status3D {
              var statusDict: [String: Any] = [:]
              // Map Status3D fields if available
              statusDict["status"] = "success"
              responseDict["result"] = statusDict
            } else {
              responseDict["result"] = NSNull()
            }
            
            resolver(responseDict)
          case .failure(let error):
            // Handle error - SDK may return "No URL" if 3D Secure URL is missing
            // This typically means the 3D Secure URL needs to come from payment response
            var errorMessage = error.localizedDescription
            if errorMessage.contains("No URL") || errorMessage.contains("no URL") {
              errorMessage = "3D Secure URL is required. This URL typically comes from payment response (paymentRequest or directPayment). returnURL alone is not sufficient."
            }
            rejecter("ERROR", errorMessage, nil)
          }
        }
      )
    }
  }
  
  // MARK: - Payment
  
  @objc func payment(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    // Extract required parameters from NSDictionary
    guard let jToken = params["jToken"] as? String else {
      rejecter("ERROR", "jToken is required", nil)
      return
    }
    
    guard let accountKey = params["accountKey"] as? String else {
      rejecter("ERROR", "accountKey is required", nil)
      return
    }
    
    guard let cardAlias = params["cardAlias"] as? String else {
      rejecter("ERROR", "cardAlias is required", nil)
      return
    }
    
    guard let amount = params["amount"] as? String else {
      rejecter("ERROR", "amount is required", nil)
      return
    }
    
    guard let orderNo = params["orderNo"] as? String else {
      rejecter("ERROR", "orderNo is required", nil)
      return
    }
    
    guard let rrn = params["rrn"] as? String else {
      rejecter("ERROR", "rrn is required", nil)
      return
    }
    
    guard let cvv = params["cvv"] as? String else {
      rejecter("ERROR", "cvv is required", nil)
      return
    }
    
    guard let currencyCodeStr = params["currencyCode"] as? String else {
      rejecter("ERROR", "currencyCode is required", nil)
      return
    }
    
    guard let paymentTypeStr = params["paymentType"] as? String else {
      rejecter("ERROR", "paymentType is required", nil)
      return
    }
    
    guard let authenticationMethod = params["authenticationMethod"] as? String else {
      rejecter("ERROR", "authenticationMethod is required", nil)
      return
    }
    
    // Optional parameters
    let acquirerIcaNumber = params["acquirerIcaNumber"] as? String
    let installmentCount = params["installmentCount"] as? Int ?? 0
    let secure3DModelStr = params["secure3DModel"] as? String
    
    // Convert currencyCode to enum
    var currencyCodeEnum: MPCurrencyCode = .TRY // Default value
    if let found = MPCurrencyCode.allCases.first(where: { $0.rawValue.lowercased() == currencyCodeStr.lowercased() }) {
      currencyCodeEnum = found
    }
    
    // Convert paymentType to enum
    // PaymentType enum needs to be checked from SDK
    // Try to find matching enum value, or use first available value as default
    var paymentTypeEnum: PaymentType
    if let found = PaymentType.allCases.first(where: { $0.rawValue.lowercased() == paymentTypeStr.lowercased() }) {
      paymentTypeEnum = found
    } else {
      // Use first available value as default
      paymentTypeEnum = PaymentType.allCases.first ?? PaymentType.allCases[0]
    }
    
    // Convert authenticationMethod to enum
    var authTypeEnum: AuthType
    if let found = AuthType.allCases.first(where: { $0.rawValue.lowercased() == authenticationMethod.lowercased() }) {
      authTypeEnum = found
    } else {
      // Use first available value as default
      authTypeEnum = AuthType.allCases.first ?? AuthType.allCases[0]
    }
    
    // Convert secure3DModel to SecurityType enum if provided
    var secure3DModelEnum: SecurityType? = nil
    if let secure3D = secure3DModelStr {
      // SecurityType enum needs to be checked from SDK
      // For now, set to nil
    }
    
    // Define completion handler outside of DispatchQueue closure to avoid syntax issues
    let completionHandler: (ServiceError?, MPResponse<PaymentResponse>?) -> Void = { (error: ServiceError?, response: MPResponse<PaymentResponse>?) in
      if let error = error {
        // Handle error
        var errorMessage = error.responseDesc ?? "Payment failed"
        if let responseCode = error.responseCode {
          errorMessage += " (Code: \(responseCode))"
        }
        if let mdStatus = error.mdStatus, !mdStatus.isEmpty {
          errorMessage += " [MD Status: \(mdStatus)]"
        }
        if let mdErrorMsg = error.mdErrorMsg, !mdErrorMsg.isEmpty {
          errorMessage += " [MD Error: \(mdErrorMsg)]"
        }
        rejecter("ERROR", errorMessage, nil)
      } else if let response = response {
        // Handle success response
        var responseDict: [String: Any] = [:]
        
        // MPResponse fields - buildId, statusCode, message are non-optional
        responseDict["statusCode"] = response.statusCode
        responseDict["message"] = response.message
        responseDict["buildId"] = response.buildId
        
        // Optional fields
        if let version = response.version {
          responseDict["version"] = version
        } else {
          responseDict["version"] = NSNull()
        }
        
        if let correlationId = response.correlationId {
          responseDict["correlationId"] = correlationId
        } else {
          responseDict["correlationId"] = NSNull()
        }
        
        if let requestId = response.requestId {
          responseDict["requestId"] = requestId
        } else {
          responseDict["requestId"] = NSNull()
        }
        
        // Check for exception
        if let exception = response.exception {
          var exceptionDict: [String: Any] = [:]
          exceptionDict["level"] = exception.level
          exceptionDict["code"] = exception.code
          exceptionDict["message"] = exception.message
          responseDict["exception"] = exceptionDict
          rejecter("ERROR", exception.message, nil)
          return
        }
        
        // Handle result - PaymentResponse
        if let resultObj = response.result {
          var resultDict: [String: Any] = [:]
          // PaymentResponse may contain transaction details, 3D Secure URL, etc.
          resultDict["status"] = "success"
          responseDict["result"] = resultDict
        } else {
          responseDict["result"] = NSNull()
        }
        
        resolver(responseDict)
      } else {
        rejecter("ERROR", "Payment failed: No response received", nil)
      }
    }
    
    // Create MPText for CVV on main thread (required by SDK)
    // MPText is a UIView subclass, so it must be created on main thread
    let workItem = DispatchWorkItem {
      let cvvMPText = MPText()
      cvvMPText.type = .cvv
      cvvMPText.text = cvv
      
      // Call SDK payment method with completion handler
      // iOS SDK actual signature: payment(_ jToken: String, _ rrn: String?, _ cvv: MPText?, _ cardAlias: String?, _ accountKey: String?, _ amount: String?, _ orderNo: String?, _ currencyCode: MPCurrencyCode?, _ paymentType: PaymentType?, _ acquirerIcaNumber: String?, _ installmentCount: Int?, _ subMerchant: SubMerchant?, _ rewardList: [Reward]?, _ orderDetails: OrderDetails?, _ authenticationMethod: AuthType?, _ orderProductDetails: OrderProductDetails?, _ buyerDetails: BuyerDetails?, _ billDetails: BillDetails?, _ deliveryDetails: DeliveryDetails?, _ otherDetails: OtherDetails?, _ secure3DModel: SecurityType?, _ terminal: Terminal?, _ mokaSubDealerDetails: MokaSubDealerDetails?, _ customParameters: CustomParameters?, _ additionalParams: [String : String]?, _ completion: @escaping (ServiceError?, MPResponse<PaymentResponse>?) -> Void)
      // All parameters are positional (no labels) and in specific order
      MasterPass.payment(
        jToken,
        rrn,
        cvvMPText,
        cardAlias,
        accountKey,
        amount,
        orderNo,
        currencyCodeEnum,
        paymentTypeEnum,
        acquirerIcaNumber,
        installmentCount > 0 ? installmentCount : nil,
        nil, // subMerchant
        nil, // rewardList
        nil, // orderDetails
        authTypeEnum,
        nil, // orderProductDetails (note: singular, not plural)
        nil, // buyerDetails
        nil, // billDetails
        nil, // deliveryDetails
        nil, // otherDetails
        secure3DModelEnum,
        nil, // terminal
        nil, // mokaSubDealerDetails
        nil, // customParameters
        nil, // additionalParams
        completionHandler
      )
    }
    DispatchQueue.main.async(execute: workItem)
  }
  
  // MARK: - Direct Payment
  
  @objc func directPayment(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Direct Payment - Not implemented yet"])
  }
  
  // MARK: - Register And Purchase
  
  @objc func registerAndPurchase(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Register And Purchase - Not implemented yet"])
  }
  
  // MARK: - QR Payment
  
  @objc func qrPayment(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "QR Payment - Not implemented yet"])
  }
  
  // MARK: - Money Send
  
  @objc func moneySend(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Money Send - Not implemented yet"])
  }
  
  // MARK: - Complete Registration
  
  @objc func completeRegistration(_ jToken: String, accountKey: String?, accountAlias: String, isMsisdnValidatedByMerchant: NSNumber?, responseToken: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Complete Registration - Not implemented yet"])
  }
  
  // MARK: - Digital Loan
  
  @objc func digitalLoan(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Digital Loan - Not implemented yet"])
  }
  
  // MARK: - Start Loan Validation
  
  @objc func startLoanValidation(_ jToken: String, returnURL: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Start Loan Validation - Not implemented yet"])
  }
}
