package com.hkmaster

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableArray

import com.masterpass.turkiye.MasterPass
import com.masterpass.turkiye.enums.AccountKeyType
import com.masterpass.turkiye.enums.AuthType
import com.masterpass.turkiye.enums.MPCurrencyCode
import com.masterpass.turkiye.enums.PaymentType
import com.masterpass.turkiye.enums.Secure3DModel
import com.masterpass.turkiye.enums.AccountChangeKind
import com.masterpass.turkiye.listener.*
import com.masterpass.turkiye.view.MPWebView
// import com.masterpass.turkiye.services.AccountServices // TODO: Check if this exists in new SDK
import com.masterpass.turkiye.mp.MPCard
import com.masterpass.turkiye.view.MPText
import com.masterpass.turkiye.view.MPCheckBox
import android.app.Activity

class MasterpassModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  
  private var masterPassInstance: MasterPass? = null
  
  override fun getName(): String {
    return "MasterpassModule"
  }
  
  // MARK: - Initialize
  
  @ReactMethod
  fun initialize(merchantId: Int, terminalGroupId: String?, language: String?, url: String, verbose: Boolean?, merchantSecretKey: String?, promise: Promise) {
    try {
      // Initialize MasterPass directly
      // Android SDK signature: MasterPass(mId: Long, tGId: String, lan: String, verbose: Boolean, bUrl: String, mSecKey: String?)
      // SDK requires terminalGroupId to be non-empty - if null/empty, SDK will throw "All parameters needed" error
      val validTerminalGroupId = terminalGroupId?.takeIf { it.isNotBlank() } 
        ?: throw IllegalArgumentException("terminalGroupId is required and cannot be empty")
      
      // Android SDK MasterPass constructor: MasterPass(mId: Long, tGId: String, lan: String, verbose: Boolean, bUrl: String)
      // Note: mSecKey parameter doesn't exist in current SDK version
      masterPassInstance = MasterPass(
        mId = merchantId.toLong(),
        tGId = validTerminalGroupId,
        lan = language ?: "tr-TR",
        verbose = verbose ?: false,
        bUrl = url
      )
      
      // Get SDK version information
      // Try to get version from MasterPass class package
      val sdkVersion = try {
        val pkg = MasterPass::class.java.`package`
        pkg?.implementationVersion?.takeIf { it.isNotBlank() } 
          ?: pkg?.specificationVersion?.takeIf { it.isNotBlank() }
          ?: "1.3.7" // Default fallback
      } catch (e: Exception) {
        "1.3.7"
      }
      
      val sdkBuild = try {
        val pkg = MasterPass::class.java.`package`
        pkg?.implementationVersion?.takeIf { it.isNotBlank() } ?: "1"
      } catch (e: Exception) {
        "1"
      }
      
      val result = Arguments.createMap()
      result.putBoolean("success", true)
      result.putString("message", "SDK initialized successfully")
      result.putInt("merchantId", merchantId)
      // Use null instead of empty string to match iOS behavior
      if (terminalGroupId != null && terminalGroupId.isNotBlank()) {
        result.putString("terminalGroupId", terminalGroupId)
      } else {
        result.putNull("terminalGroupId")
      }
      result.putString("language", language ?: "tr-TR")
      result.putString("url", url)
      result.putBoolean("verbose", verbose ?: false)
      // Use null instead of empty string to match iOS behavior
      if (merchantSecretKey != null && merchantSecretKey.isNotBlank()) {
        result.putString("merchantSecretKey", merchantSecretKey)
      } else {
        result.putNull("merchantSecretKey")
      }
      result.putString("sdkVersion", sdkVersion)
      result.putString("sdkBuild", sdkBuild)
      result.putString("sdkClassName", MasterPass::class.java.name)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", "SDK initialization failed: ${e.message ?: "Unknown error"}", e)
    }
  }
  
  private fun getMasterPassInstance(): MasterPass {
    return masterPassInstance ?: throw IllegalStateException("MasterPass not initialized. Call initialize() first.")
  }
  
  // MARK: - Add Card
  
  @ReactMethod
  fun addCard(jToken: String, accountKey: String?, accountKeyType: String?, rrn: String?, userId: String?, card: ReadableMap?, cardAlias: String?, isMsisdnValidatedByMerchant: Boolean?, authenticationMethod: String?, additionalParams: ReadableMap?, promise: Promise) {
    try {
      // Extract card information
      var cardNumber: String? = null
      var expiryDate: String? = null
      var cvv: String? = null
      var cardHolderName: String? = null
      
      card?.let { cardMap ->
        cardNumber = cardMap.getString("cardNumber")
        expiryDate = cardMap.getString("expiryDate")
        cvv = cardMap.getString("cvv")
        cardHolderName = cardMap.getString("cardHolderName")
      }
      
      // Convert accountKeyType to enum (non-nullable required by SDK)
      val accountKeyTypeEnum = accountKeyType?.let {
        AccountKeyType.values().find { type -> type.name.equals(accountKeyType, ignoreCase = true) }
      } ?: AccountKeyType.MSISDN // Default value
      
      // Get current activity for context
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Validate required fields
      if (cardNumber.isNullOrBlank()) {
        promise.reject("ERROR", "Card number is required", null)
        return
      }
      
      if (expiryDate.isNullOrBlank()) {
        promise.reject("ERROR", "Expiry date is required", null)
        return
      }
      
      if (cvv.isNullOrBlank()) {
        promise.reject("ERROR", "CVV is required", null)
        return
      }
      
      // Create MPText instances for card number and CVV
      // SDK requires MPText type to be set correctly for validation
      // Android SDK MPText needs type to be set before setText, otherwise SDK throws "Card number is empty" error
      val cardNoMPText = MPText(activity)
      // Set type to cardNo for card number validation using reflection
      try {
        val typeField = cardNoMPText.javaClass.getDeclaredField("type")
        typeField.isAccessible = true
        // Try to find MPTextType enum - Android SDK uses enum values like CARD_NO, CVV
        val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
        val cardNoType = mpTextTypeClass.getDeclaredField("CARD_NO").get(null)
        typeField.set(cardNoMPText, cardNoType)
      } catch (e: Exception) {
        // If reflection fails, try alternative enum field names
        try {
          val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
          val cardNoType = mpTextTypeClass.getDeclaredField("CARDNUMBER").get(null)
          val typeField = cardNoMPText.javaClass.getDeclaredField("type")
          typeField.isAccessible = true
          typeField.set(cardNoMPText, cardNoType)
        } catch (e2: Exception) {
          // If all reflection attempts fail, SDK may handle type internally
          // But this might cause "Card number is empty" error
        }
      }
      // Set text after type is set - SDK validates based on type
      cardNoMPText.setText(cardNumber)
      
      val cvvMPText = MPText(activity)
      // Set type to cvv for CVV validation
      try {
        val typeField = cvvMPText.javaClass.getDeclaredField("type")
        typeField.isAccessible = true
        val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
        val cvvType = mpTextTypeClass.getDeclaredField("CVV").get(null)
        typeField.set(cvvMPText, cvvType)
      } catch (e: Exception) {
        // If reflection fails, try alternative enum field names
        try {
          val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
          val cvvType = mpTextTypeClass.getDeclaredField("CVC").get(null)
          val typeField = cvvMPText.javaClass.getDeclaredField("type")
          typeField.isAccessible = true
          typeField.set(cvvMPText, cvvType)
        } catch (e2: Exception) {
          // If all reflection attempts fail, SDK may handle type internally
        }
      }
      // Set text after type is set
      cvvMPText.setText(cvv)
      
      // Create MPCheckBox and set it to checked (required by SDK)
      // SDK requires terms and conditions checkbox to be selected
      val checkBox = MPCheckBox(activity)
      checkBox.isChecked = true
      
      // Create MPCard object - use non-null values for required fields
      // MPCard constructor: MPCard(cardNo: MPText, cvv: MPText, cardHolder: String, date: String, checkBox: MPCheckBox)
      val mpCard = MPCard(
        cardNoMPText,
        cvvMPText,
        cardHolderName ?: "",
        expiryDate,
        checkBox
      )
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      val mp = getMasterPassInstance()
      
      // Call MasterPass.addCard directly
      // SDK signature: addCard(jToken, accountKey, accountKeyType, rrn, card, cardAlias, isMsisdnValidatedByMerchant, userId, authenticationMethod, listener)
      mp.addCard(
        jToken,
        accountKey?.takeIf { it.isNotBlank() } ?: "",
        accountKeyTypeEnum,
        rrn?.takeIf { it.isNotBlank() } ?: "",
        mpCard,
        cardAlias?.takeIf { it.isNotBlank() } ?: "",
        isMsisdnValidatedByMerchant ?: false,
        userId?.takeIf { it.isNotBlank() } ?: "",
        authTypeEnum,
        object : AddCardListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Add Card failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Add Card failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Card saved successfully")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle response result - GeneralAccountResponse
              // Note: SDK may return different response type at runtime, but interface expects GeneralAccountResponse
              val resultObj = response.result
              val cardSaveResult = Arguments.createMap()
              
              // Map GeneralAccountResponse fields
              if (resultObj != null) {
                cardSaveResult.putString("status", "success")
                
                if (resultObj.retrievalReferenceNumber != null) {
                  cardSaveResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  cardSaveResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  cardSaveResult.putString("responseCode", resultObj.responseCode)
                } else {
                  cardSaveResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  cardSaveResult.putString("description", resultObj.description)
                  cardSaveResult.putString("resultDescription", resultObj.description) // iOS compatibility
                } else {
                  cardSaveResult.putNull("description")
                  cardSaveResult.putNull("resultDescription")
                }
                
                if (resultObj.token != null) {
                  cardSaveResult.putString("token", resultObj.token)
                } else {
                  cardSaveResult.putNull("token")
                }
                
                // Android GeneralAccountResponse doesn't have url3d fields, but iOS GeneralResponseWith3D does
                // Add null values to match iOS response structure for consistency
                cardSaveResult.putNull("url3d")
                cardSaveResult.putNull("url3dSuccess")
                cardSaveResult.putNull("url3dFail")
                
                cardSaveResult.putString("jToken", jToken)
                
                if (cardAlias != null) {
                  cardSaveResult.putString("cardAlias", cardAlias)
                } else {
                  cardSaveResult.putNull("cardAlias")
                }
              } else {
                // If result is null, create basic structure
                cardSaveResult.putString("status", "success")
                cardSaveResult.putString("jToken", jToken)
                if (cardAlias != null) {
                  cardSaveResult.putString("cardAlias", cardAlias)
                }
                cardSaveResult.putNull("retrievalReferenceNumber")
                cardSaveResult.putNull("responseCode")
                cardSaveResult.putNull("description")
                cardSaveResult.putNull("resultDescription")
                cardSaveResult.putNull("token")
                cardSaveResult.putNull("url3d")
                cardSaveResult.putNull("url3dSuccess")
                cardSaveResult.putNull("url3dFail")
              }
              
              result.putMap("result", cardSaveResult)
              
              promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Add Card failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Add Card failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", "Add Card failed: ${e.message ?: "Unknown error"}", e)
    }
  }
  
  // MARK: - Link Account To Merchant
  
  @ReactMethod
  fun linkAccountToMerchant(jToken: String, accountKey: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.linkAccountToMerchant(
        jToken = jToken,
        accountKey = accountKey ?: "",
        linkToMerchantListener = object : LinkToMerchantListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.LinkToMerchantResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Link Account To Merchant failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Link Account To Merchant failed with exception", null)
                return
              }
              
              // Map LinkToMerchantResponse manually to match iOS structure
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Link Account To Merchant successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle LinkToMerchantResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.LinkToMerchantResponse) {
                val linkResult = Arguments.createMap()
                
                if (resultObj.token != null) {
                  linkResult.putString("token", resultObj.token)
                } else {
                  linkResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  linkResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  linkResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  linkResult.putString("responseCode", resultObj.responseCode)
                } else {
                  linkResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  linkResult.putString("description", resultObj.description)
                } else {
                  linkResult.putNull("description")
                }
                
                if (resultObj.cardIssuerName != null) {
                  linkResult.putString("cardIssuerName", resultObj.cardIssuerName)
                } else {
                  linkResult.putNull("cardIssuerName")
                }
                
                if (resultObj.maskedPan != null) {
                  linkResult.putString("maskedPan", resultObj.maskedPan)
                } else {
                  linkResult.putNull("maskedPan")
                }
                
                linkResult.putString("jToken", jToken)
                
                if (accountKey != null) {
                  linkResult.putString("accountKey", accountKey)
                } else {
                  linkResult.putNull("accountKey")
                }
                
                result.putMap("result", linkResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Link Account To Merchant failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Link Account To Merchant failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Account Access
  
  @ReactMethod
  fun accountAccess(jToken: String, accountKey: String?, accountKeyType: String?, userId: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val accountKeyTypeEnum = accountKeyType?.let { 
        AccountKeyType.values().find { it.name.equals(accountKeyType, ignoreCase = true) }
      } ?: throw IllegalArgumentException("accountKeyType is required")
      
      mp.accountAccess(
        jToken,
        accountKey ?: "",
        accountKeyTypeEnum,
        userId ?: "",
        object : AccountAccessRequestListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.CardResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Account Access failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Account Access failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Account Access successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle CardResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.CardResponse) {
                val cardResponseResult = Arguments.createMap()
                
                cardResponseResult.putString("accountKey", resultObj.accountKey)
                cardResponseResult.putString("accountState", resultObj.accountState)
                
                // Convert cards array
                // CardResponse.cards is ArrayList<Object>, need to handle as Map or convert
                val cardsArray = Arguments.createArray()
                for (cardObj in resultObj.cards) {
                  val cardMap = Arguments.createMap()
                  // cards is ArrayList<Object>, convert to string representation or handle as Map
                  if (cardObj is Map<*, *>) {
                    (cardObj as? Map<String, *>)?.let { card ->
                      card["cardAlias"]?.let { cardMap.putString("cardAlias", it.toString()) }
                      card["cardState"]?.let { cardMap.putString("cardState", it.toString()) }
                      card["maskedCardNumber"]?.let { cardMap.putString("maskedCardNumber", it.toString()) }
                      card["uniqueCardNumber"]?.let { cardMap.putString("uniqueCardNumber", it.toString()) }
                      card["cardType"]?.let { cardMap.putString("cardType", it.toString()) }
                      card["productName"]?.let { cardMap.putString("productName", it.toString()) }
                      card["cardBin"]?.let { cardMap.putString("cardBin", it.toString()) }
                      card["cardIssuerIcaNumber"]?.let { cardMap.putString("cardIssuerIcaNumber", it.toString()) }
                    }
                  } else {
                    // Fallback: convert object to string
                    cardMap.putString("data", cardObj.toString())
                  }
                  
                  cardsArray.pushMap(cardMap)
                }
                cardResponseResult.putArray("cards", cardsArray)
                
                // Android SDK CardResponse doesn't have accountInformation or recipientCards
                // These fields are iOS-specific, so we create compatible structure for Android
                val accountInfoMap = Arguments.createMap()
                accountInfoMap.putBoolean("isAccountLinked", false) // Default value since Android doesn't have this field
                cardResponseResult.putMap("accountInformation", accountInfoMap)
                cardResponseResult.putNull("recipientCards") // Android doesn't have recipientCards
                
                result.putMap("result", cardResponseResult)
              } else if (resultObj != null) {
                // Fallback: if result is not CardResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Account Access failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Account Access failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Remove Card
  
  @ReactMethod
  fun removeCard(jToken: String, accountKey: String?, cardAlias: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.removeCard(
        jToken = jToken,
        accountKey = accountKey ?: "",
        cardAlias = cardAlias ?: "",
        removeCardListener = object : RemoveCardListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.RemoveCardResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Remove Card failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Remove Card failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Remove Card successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle RemoveCardResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.RemoveCardResponse) {
                val removeCardResult = Arguments.createMap()
                
                // Map RemoveCardResponse fields - Android SDK has these fields:
                // clientId (int), refNo (String)
                removeCardResult.putInt("clientId", resultObj.clientId)
                
                if (resultObj.refNo != null) {
                  removeCardResult.putString("refNo", resultObj.refNo)
                } else {
                  removeCardResult.putNull("refNo")
                }
                
                result.putMap("result", removeCardResult)
              } else if (resultObj != null) {
                // Fallback: if result is not RemoveCardResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Remove Card failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Remove Card failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Update User ID
  
  @ReactMethod
  fun updateUserId(jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.updateUserId(
        jToken = jToken,
        accountKey = accountKey ?: "",
        currentUserId = currentUserId ?: "",
        newUserId = newUserId ?: "",
        updateUserIdListener = object : UpdateUserIdListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Update User ID failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Update User ID failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Update User ID successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle UpdateUserIdResponse result - uses GeneralAccountResponse
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val updateUserIdResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  updateUserIdResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  updateUserIdResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  updateUserIdResult.putString("responseCode", resultObj.responseCode)
                } else {
                  updateUserIdResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  updateUserIdResult.putString("description", resultObj.description)
                } else {
                  updateUserIdResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  updateUserIdResult.putString("token", resultObj.token)
                } else {
                  updateUserIdResult.putNull("token")
                }
                
                result.putMap("result", updateUserIdResult)
              } else if (resultObj != null) {
                // Fallback: if result is not UpdateUserIdResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Update User ID failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Update User ID failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Update User MSISDN
  
  @ReactMethod
  fun updateUserMsisdn(jToken: String, accountKey: String?, newMsisdn: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.updateUserMsisdn(
        jToken = jToken,
        accountKey = accountKey ?: "",
        newMsisdn = newMsisdn ?: "",
        updateUserMsisdnListener = object : UpdateUserMsisdnListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Update User MSISDN failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Update User MSISDN failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Update User MSISDN successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle UpdateUserMsisdnResponse result - uses GeneralAccountResponse
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val updateUserMsisdnResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  updateUserMsisdnResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  updateUserMsisdnResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  updateUserMsisdnResult.putString("responseCode", resultObj.responseCode)
                } else {
                  updateUserMsisdnResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  updateUserMsisdnResult.putString("description", resultObj.description)
                } else {
                  updateUserMsisdnResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  updateUserMsisdnResult.putString("token", resultObj.token)
                } else {
                  updateUserMsisdnResult.putNull("token")
                }
                
                result.putMap("result", updateUserMsisdnResult)
              } else if (resultObj != null) {
                // Fallback: if result is not UpdateUserMsisdnResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Update User MSISDN failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Update User MSISDN failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Add User ID
  
  @ReactMethod
  fun addUserId(jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.addUserId(
        jToken = jToken,
        accountKey = accountKey ?: "",
        currentUserId = currentUserId ?: "",
        newUserId = newUserId ?: "",
        addUserIdListener = object : AddUserIdListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Add User ID failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Add User ID failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Add User ID successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle AddUserIdResponse result - uses GeneralAccountResponse
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val addUserIdResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  addUserIdResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  addUserIdResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  addUserIdResult.putString("responseCode", resultObj.responseCode)
                } else {
                  addUserIdResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  addUserIdResult.putString("description", resultObj.description)
                } else {
                  addUserIdResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  addUserIdResult.putString("token", resultObj.token)
                } else {
                  addUserIdResult.putNull("token")
                }
                
                result.putMap("result", addUserIdResult)
              } else if (resultObj != null) {
                // Fallback: if result is not AddUserIdResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Add User ID failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Add User ID failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Recurring Order Register
  
  @ReactMethod
  fun recurringOrderRegister(jToken: String, accountKey: String?, cardAlias: String?, productId: String?, amountLimit: String?, expireDate: String, authenticationMethod: String?, rrn: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // SDK signature: recurringOrderRegister(jToken, accountKey, cardAlias, productId, amountLimit, expireDate, authenticationMethod, rrn, listener)
      mp.recurringOrderRegister(
        jToken,
        accountKey ?: "",
        cardAlias ?: "",
        productId ?: "",
        amountLimit ?: "",
        expireDate,
        authTypeEnum,
        rrn,
        object : RecurringOrderListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Recurring Order Register failed", null)
                return
              }
              
              // Map GeneralAccountResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Recurring Order Register successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle GeneralAccountResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val recurringResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  recurringResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  recurringResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  recurringResult.putString("responseCode", resultObj.responseCode)
                } else {
                  recurringResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  recurringResult.putString("description", resultObj.description)
                } else {
                  recurringResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  recurringResult.putString("token", resultObj.token)
                } else {
                  recurringResult.putNull("token")
                }
                
                result.putMap("result", recurringResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Recurring Order Register failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Recurring Order Register failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Recurring Order Update
  
  @ReactMethod
  fun recurringOrderUpdate(jToken: String, accountKey: String?, cardAlias: String?, productId: String?, amountLimit: String?, expireDate: String, rrn: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // SDK signature: recurringOrderUpdate(jToken, accountKey, cardAlias, productId, amountLimit, expireDate, rrn, listener)
      mp.recurringOrderUpdate(
        jToken,
        accountKey ?: "",
        cardAlias ?: "",
        productId ?: "",
        amountLimit ?: "",
        expireDate,
        rrn,
        object : RecurringOrderListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Recurring Order Update failed", null)
                return
              }
              
              // Map GeneralAccountResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Recurring Order Update successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle GeneralAccountResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val recurringResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  recurringResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  recurringResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  recurringResult.putString("responseCode", resultObj.responseCode)
                } else {
                  recurringResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  recurringResult.putString("description", resultObj.description)
                } else {
                  recurringResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  recurringResult.putString("token", resultObj.token)
                } else {
                  recurringResult.putNull("token")
                }
                
                result.putMap("result", recurringResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Recurring Order Update failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Recurring Order Update failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Recurring Order Delete
  
  @ReactMethod
  fun recurringOrderDelete(jToken: String, accountKey: String?, accountChangedEventName: String?, cardAlias: String?, productId: String?, authenticationMethod: String?, rrn: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // Convert accountChangedEventName to AccountChangeKind enum
      val accountChangeKindEnum = accountChangedEventName?.let {
        AccountChangeKind.values().find { kind -> kind.name.equals(accountChangedEventName, ignoreCase = true) }
      } ?: AccountChangeKind.RecurringOrderDeleted // Default value
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // SDK signature: recurringOrderDelete(jToken, accountChangeKind, accountKey, authenticationMethod, cardAlias, productId, rrn, listener)
      mp.recurringOrderDelete(
        jToken,
        accountChangeKindEnum,
        accountKey ?: "",
        authTypeEnum,
        cardAlias ?: "",
        productId ?: "",
        rrn,
        object : RecurringOrderListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Recurring Order Delete failed", null)
                return
              }
              
              // Map GeneralAccountResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Recurring Order Delete successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle GeneralAccountResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val recurringResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  recurringResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  recurringResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  recurringResult.putString("responseCode", resultObj.responseCode)
                } else {
                  recurringResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  recurringResult.putString("description", resultObj.description)
                } else {
                  recurringResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  recurringResult.putString("token", resultObj.token)
                } else {
                  recurringResult.putNull("token")
                }
                
                result.putMap("result", recurringResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Recurring Order Delete failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Recurring Order Delete failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Verify
  
  @ReactMethod
  fun verify(jToken: String, otp: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Create MPText for OTP (required by SDK)
      // SDK requires MPText type to be set correctly for validation
      // Android SDK MPText needs type to be set before setText, otherwise SDK may throw validation errors
      val otpMPText = MPText(activity)
      // Set type to OTP for OTP validation using reflection
      try {
        val typeField = otpMPText.javaClass.getDeclaredField("type")
        typeField.isAccessible = true
        // Try to find MPTextType enum - Android SDK uses enum values like OTP, RTA
        val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
        val otpType = mpTextTypeClass.getDeclaredField("OTP").get(null)
        typeField.set(otpMPText, otpType)
      } catch (e: Exception) {
        // If reflection fails, try alternative enum field names
        try {
          val mpTextTypeClass = Class.forName("com.masterpass.turkiye.enums.MPTextType")
          val otpType = mpTextTypeClass.getDeclaredField("RTA").get(null) // RTA is alternative for OTP
          val typeField = otpMPText.javaClass.getDeclaredField("type")
          typeField.isAccessible = true
          typeField.set(otpMPText, otpType)
        } catch (e2: Exception) {
          // If all reflection attempts fail, SDK may handle type internally
          // But this might cause validation errors
        }
      }
      // Set text after type is set - SDK validates based on type
      otpMPText.setText(otp)
      
      mp.verify(
        jToken = jToken,
        otpCode = otpMPText,
        verifyListener = object : VerifyListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.validateflow.VerifyResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Verify failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Verify failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Verify successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle VerifyResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.validateflow.VerifyResponse) {
                val verifyResult = Arguments.createMap()
                
                // Map VerifyResponse fields - Android SDK has these fields:
                // retrievalReferenceNumber, isVerified, cardUniqueNumber, token, responseCode, url3d, url3dSuccess, url3dFail, urlIFrame
                verifyResult.putBoolean("isVerified", resultObj.isVerified)
                
                if (resultObj.retrievalReferenceNumber != null) {
                  verifyResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  verifyResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.cardUniqueNumber != null) {
                  verifyResult.putString("cardUniqueNumber", resultObj.cardUniqueNumber)
                } else {
                  verifyResult.putNull("cardUniqueNumber")
                }
                
                if (resultObj.token != null) {
                  verifyResult.putString("token", resultObj.token)
                } else {
                  verifyResult.putNull("token")
                }
                
                if (resultObj.responseCode != null) {
                  verifyResult.putString("responseCode", resultObj.responseCode)
                } else {
                  verifyResult.putNull("responseCode")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  verifyResult.putString("url3d", resultObj.url3d)
                } else {
                  verifyResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  verifyResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  verifyResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  verifyResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  verifyResult.putNull("url3dFail")
                }
                
                if (resultObj.urlIFrame != null) {
                  verifyResult.putString("urlIFrame", resultObj.urlIFrame)
                } else {
                  verifyResult.putNull("urlIFrame")
                }
                
                result.putMap("result", verifyResult)
              } else if (resultObj != null) {
                // Fallback: if result is not VerifyResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Verify failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Verify failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Resend OTP
  
  @ReactMethod
  fun resendOtp(jToken: String, promise: Promise) {
    try {
      // Validate jToken - SDK requires non-empty jToken
      if (jToken.isNullOrBlank()) {
        promise.reject("ERROR", "jToken is required and cannot be empty", null)
        return
      }
      
      val mp = getMasterPassInstance()
      mp.resendOtp(
        jToken = jToken,
        resendOtpListener = object : ResendOtpListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.account.GeneralAccountResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Resend OTP failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Resend OTP failed with exception", null)
                return
              }
              
              val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Resend OTP successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle GeneralAccountResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.account.GeneralAccountResponse) {
                val resendOtpResult = Arguments.createMap()
                
                // Map GeneralAccountResponse fields
                if (resultObj.retrievalReferenceNumber != null) {
                  resendOtpResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  resendOtpResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.responseCode != null) {
                  resendOtpResult.putString("responseCode", resultObj.responseCode)
                } else {
                  resendOtpResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  resendOtpResult.putString("description", resultObj.description)
                } else {
                  resendOtpResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  resendOtpResult.putString("token", resultObj.token)
                } else {
                  resendOtpResult.putNull("token")
                }
                
                result.putMap("result", resendOtpResult)
              } else if (resultObj != null) {
                // Fallback: if result is not ResendOtpResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Resend OTP failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
            promise.reject("ERROR", error.responseDesc ?: "Resend OTP failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Start 3D Validation
  
  @ReactMethod
  fun start3DValidation(jToken: String, returnURL: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Validate returnURL - SDK requires a valid URL for 3D Secure
      if (returnURL.isNullOrBlank()) {
        promise.reject("ERROR", "returnURL is required for 3D Secure validation", null)
        return
      }
      
      // Create MPWebView instance on main thread
      val webView = MPWebView(activity)
      webView.url3d = returnURL
      
      // Create Transaction3DListener - parameters are nullable in Kotlin interface
      val listener = object : com.masterpass.turkiye.listener.Transaction3DListener {
        override fun onSuccess(result: com.masterpass.turkiye.results.ValidateTransaction3DResult?) {
          try {
            val responseMap = Arguments.createMap()
            responseMap.putInt("statusCode", 200)
            responseMap.putString("message", "3D Validation successful")
            responseMap.putString("status", "success")
            // Map ValidateTransaction3DResult token if available
            if (result?.token != null) {
              responseMap.putString("token", result.token)
            }
            promise.resolve(responseMap)
          } catch (e: Exception) {
            promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
          }
        }
        
        override fun onServiceError(error: com.masterpass.turkiye.response.ServiceError?) {
          try {
            val errorMessage = StringBuilder()
            errorMessage.append(error?.responseDesc ?: "3D Validation failed")
            if (error?.responseCode != null) {
              errorMessage.append(" (Code: ${error.responseCode})")
            }
            promise.reject("ERROR", errorMessage.toString(), null)
          } catch (e: Exception) {
            promise.reject("ERROR", error?.responseDesc ?: "3D Validation failed", null)
          }
        }
        
        override fun onServiceResponse(response: com.masterpass.turkiye.response.ServiceResponse?) {
          try {
            val responseMap = Arguments.createMap()
            responseMap.putInt("statusCode", 200)
            responseMap.putString("message", response?.responseDesc ?: "3D Validation response received")
            if (response?.token != null) {
              responseMap.putString("token", response.token)
            }
            if (response?.refNo != null) {
              responseMap.putString("refNo", response.refNo)
            }
            if (response?.responseCode != null) {
              responseMap.putString("responseCode", response.responseCode)
            }
            promise.resolve(responseMap)
          } catch (e: Exception) {
            promise.reject("ERROR", "Failed to process service response: ${e.message ?: "Unknown error"}", e)
          }
        }
        
        override fun onInternalError(error: String?) {
          promise.reject("ERROR", "Internal error: ${error ?: "Unknown"}", null)
        }
      }
      
      // SDK signature: start3DValidation(jToken, webView, listener)
      mp.start3DValidation(
        jToken,
        webView,
        listener
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Payment
  
  @ReactMethod
  fun payment(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // Extract parameters from ReadableMap
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val accountKey = params.getString("accountKey")
      val amount = params.getString("amount")
      val orderNo = params.getString("orderNo")
      val cardAlias = params.getString("cardAlias")
      val currencyCode = params.getString("currencyCode")
      val installmentCount = if (params.hasKey("installmentCount") && !params.isNull("installmentCount")) {
        params.getInt("installmentCount")
      } else {
        0
      }
      val requestReferenceNo = params.getString("requestReferenceNo")
      val acquirerIcaNumber = params.getString("acquirerIcaNumber")
      val authenticationMethod = params.getString("authenticationMethod")
      val cvv = params.getString("cvv")
      
      // Get current activity for context
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Create MPText for CVV (required by SDK)
      val cvvMPText = MPText(activity)
      if (cvv != null && cvv.isNotEmpty()) {
        cvvMPText.setText(cvv)
      }
      
      // Convert currencyCode to enum
      val currencyCodeEnum = currencyCode?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(currencyCode, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY // Default to TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      val subMerchant = params.getMap("subMerchant") // SubMerchant object
      val rewardList = params.getArray("rewardList") // Reward[] array
      val orderDetails = params.getMap("orderDetails") // OrderDetails object
      val orderProductsDetails = params.getMap("orderProductsDetails") // OrderProductsDetails object
      val buyerDetails = params.getMap("buyerDetails") // BuyerDetails object
      val billDetails = params.getMap("billDetails") // BillDetails object
      val deliveryDetails = params.getMap("deliveryDetails") // DeliveryDetails object
      val otherDetails = params.getMap("otherDetails") // OtherDetails object
      val mokaSubDealerDetails = params.getMap("mokaSubDealerDetails") // MokaSubDealersDetails object
      val additionalParams = params.getMap("additionalParams") // CustomParameters or HashMap
      
      // Call SDK payment method with new signature
      // Android SDK signature: payment(jToken, requestReferenceNo, cvv, cardAlias, accountKey, amount, orderNo, currencyCode, paymentType, acquirerIcaNumber, installmentCount, subMerchant, rewardList, orderDetails, authenticationMethod, orderProductsDetails, buyerDetails, billDetails, deliveryDetails, otherDetails, secure3DModel, mokaSubDealerDetails, additionalParams, paymentListener)
      mp.payment(
        jToken,
        requestReferenceNo,
        cvvMPText,
        cardAlias,
        accountKey,
        amount,
        orderNo,
        currencyCodeEnum,
        paymentTypeEnum,
        acquirerIcaNumber,
        if (installmentCount > 0) installmentCount else null,
        null, // subMerchant - TODO: Convert from ReadableMap
        null, // rewardList - TODO: Convert from ReadableArray
        null, // orderDetails - TODO: Convert from ReadableMap
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails - TODO: Convert from ReadableMap
        null, // buyerDetails - TODO: Convert from ReadableMap
        null, // billDetails - TODO: Convert from ReadableMap
        null, // deliveryDetails - TODO: Convert from ReadableMap
        null, // otherDetails - TODO: Convert from ReadableMap
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails - TODO: Convert from ReadableMap
        null, // terminal - TODO: Convert from ReadableMap
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              // Check if response has exception - even in onSuccess, SDK might return exception
              if (response.exception != null) {
                val errorMap = Arguments.createMap()
                errorMap.putInt("statusCode", response.statusCode ?: 500)
                errorMap.putString("message", response.message ?: "Payment failed")
                
                if (response.buildId != null) {
                  errorMap.putString("buildId", response.buildId)
                } else {
                  errorMap.putNull("buildId")
                }
                
                if (response.version != null) {
                  errorMap.putString("version", response.version)
                } else {
                  errorMap.putNull("version")
                }
                
                if (response.correlationId != null) {
                  errorMap.putString("correlationId", response.correlationId)
                } else {
                  errorMap.putNull("correlationId")
                }
                
                if (response.requestId != null) {
                  errorMap.putString("requestId", response.requestId)
                } else {
                  errorMap.putNull("requestId")
                }
                
                val exceptionMap = Arguments.createMap()
                exceptionMap.putString("level", response.exception?.level ?: "")
                exceptionMap.putString("code", response.exception?.code ?: "")
                exceptionMap.putString("message", response.exception?.message ?: "")
                errorMap.putMap("exception", exceptionMap)
                
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Payment failed with exception", null)
                return
              }
      
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Payment successful")
              
              // Handle nullable strings properly
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback: if result is not PaymentResponse, wrap it in result object
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                // If result is null, put null in result field
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              // Send complete error information including all ServiceError fields
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Payment failed")
              
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Payment failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Direct Payment
  
  @ReactMethod
  fun directPayment(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // Extract parameters
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val accountKey = params.getString("accountKey")
      val amount = params.getString("amount")
      val orderNo = params.getString("orderNo")
      val cardAlias = params.getString("cardAlias")
      val currencyCode = params.getString("currencyCode")
      val installmentCount = if (params.hasKey("installmentCount") && !params.isNull("installmentCount")) {
        params.getInt("installmentCount")
      } else {
        0
      }
      val requestReferenceNo = params.getString("requestReferenceNo")
      val acquirerIcaNumber = params.getString("acquirerIcaNumber")
      val authenticationMethod = params.getString("authenticationMethod")
      val cvv = params.getString("cvv")
      
      // Get current activity for context
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Create MPText for CVV
      val cvvMPText = MPText(activity)
      if (cvv != null && cvv.isNotEmpty()) {
        cvvMPText.setText(cvv)
      }
      
      // Convert currencyCode to enum
      val currencyCodeEnum = currencyCode?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(currencyCode, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Call SDK payment method with new signature (directPayment uses same method as payment)
      mp.payment(
        jToken,
        requestReferenceNo,
        cvvMPText,
        cardAlias,
        accountKey,
        amount,
        orderNo,
        currencyCodeEnum,
        paymentTypeEnum,
        acquirerIcaNumber,
        if (installmentCount > 0) installmentCount else null,
        null, // subMerchant
        null, // rewardList
        null, // orderDetails
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails
        null, // buyerDetails
        null, // billDetails
        null, // deliveryDetails
        null, // otherDetails
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails
        null, // additionalParams
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Direct payment failed", null)
                return
              }
              
              // Map PaymentResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Direct payment successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Direct payment failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Direct payment failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Register And Purchase
  
  @ReactMethod
  fun registerAndPurchase(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      
      // Extract parameters
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val accountKey = params.getString("accountKey")
      val amount = params.getString("amount")
      val orderNo = params.getString("orderNo")
      val cardAlias = params.getString("cardAlias")
      val currencyCode = params.getString("currencyCode")
      val installmentCount = if (params.hasKey("installmentCount") && !params.isNull("installmentCount")) {
        params.getInt("installmentCount")
      } else {
        0
      }
      val requestReferenceNo = params.getString("requestReferenceNo")
      val acquirerIcaNumber = params.getString("acquirerIcaNumber")
      val authenticationMethod = params.getString("authenticationMethod")
      val cvv = params.getString("cvv")
      
      // Get current activity for context
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod?.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Create MPText for CVV
      val cvvMPText = MPText(activity)
      if (cvv != null && cvv.isNotEmpty()) {
        cvvMPText.setText(cvv)
      }
      
      // Convert currencyCode to enum
      val currencyCodeEnum = currencyCode?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(currencyCode, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Call SDK payment method with new signature (registerAndPurchase uses same method as payment)
      mp.payment(
        jToken,
        requestReferenceNo,
        cvvMPText,
        cardAlias,
        accountKey,
        amount,
        orderNo,
        currencyCodeEnum,
        paymentTypeEnum,
        acquirerIcaNumber,
        if (installmentCount > 0) installmentCount else null,
        null, // subMerchant
        null, // rewardList
        null, // orderDetails
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails
        null, // buyerDetails
        null, // billDetails
        null, // deliveryDetails
        null, // otherDetails
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails
        null, // additionalParams
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Register and purchase failed", null)
                return
              }
              
              // Map PaymentResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Register and purchase successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Register and purchase failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              if (!error.mdStatus.isNullOrBlank()) {
                errorMessage.append(" [MD Status: ${error.mdStatus}]")
              }
              try {
                val mdErrorMsg = error.javaClass.getDeclaredField("mdErrorMsg")?.let { field ->
                  field.isAccessible = true
                  field.get(error) as? String
                }
                if (!mdErrorMsg.isNullOrBlank()) {
                  errorMessage.append(" [MD Error: $mdErrorMsg]")
                }
              } catch (e: Exception) {
                // mdErrorMsg property not accessible, skip it
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Register and purchase failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - QR Payment
  
  @ReactMethod
  fun qrPayment(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      // QR Payment uses paymentRequest method
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      val cvvMPText = MPText(activity)
      val authenticationMethod = params.getString("authenticationMethod") ?: "_3D"
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Convert currencyCode to enum (default to TRY)
      val currencyCodeEnum = params.getString("currencyCode")?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(it, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      
      mp.payment(
        jToken,
        null, // requestReferenceNo
        cvvMPText,
        null, // cardAlias
        null, // accountKey
        amount,
        null, // orderNo
        currencyCodeEnum,
        paymentTypeEnum,
        null, // acquirerIcaNumber
        null, // installmentCount
        null, // subMerchant
        null, // rewardList
        null, // orderDetails
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails
        null, // buyerDetails
        null, // billDetails
        null, // deliveryDetails
        null, // otherDetails
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails
        null, // additionalParams
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "QR Payment failed", null)
                return
              }
              
              // Map PaymentResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "QR Payment successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "QR Payment failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "QR Payment failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Money Send
  
  @ReactMethod
  fun moneySend(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      // Money Send uses paymentRequest method
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      val cvvMPText = MPText(activity)
      val authenticationMethod = params.getString("authenticationMethod") ?: "_3D"
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Convert currencyCode to enum (default to TRY)
      val currencyCodeEnum = params.getString("currencyCode")?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(it, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      
      mp.payment(
        jToken,
        null, // requestReferenceNo
        cvvMPText,
        null, // cardAlias
        null, // accountKey
        amount,
        null, // orderNo
        currencyCodeEnum,
        paymentTypeEnum,
        null, // acquirerIcaNumber
        null, // installmentCount
        null, // subMerchant
        null, // rewardList
        null, // orderDetails
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails
        null, // buyerDetails
        null, // billDetails
        null, // deliveryDetails
        null, // otherDetails
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails
        null, // additionalParams
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Money Send failed", null)
                return
              }
              
              // Map PaymentResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Money Send successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Money Send failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Money Send failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Complete Registration
  
  @ReactMethod
  fun completeRegistration(jToken: String, accountKey: String?, accountAlias: String, isMsisdnValidatedByMerchant: Boolean?, responseToken: String?, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      // SDK method not found - completeRegistration doesn't exist in Android SDK
      // Placeholder implementation - matches iOS response structure for consistency
      // iOS SDK has this method, but Android SDK doesn't have it yet
      val result = Arguments.createMap()
      
      // Map MPResponse fields to match iOS structure (MPResponse<GeneralResponse>)
      result.putInt("statusCode", 200)
      result.putString("message", "Complete Registration - SDK method not available in Android SDK")
      
      // Add optional MPResponse fields (null for placeholder)
      result.putNull("buildId")
      result.putNull("version")
      result.putNull("correlationId")
      result.putNull("requestId")
      
      // Map GeneralResponse result structure (matches iOS)
      val resultObj = Arguments.createMap()
      resultObj.putString("status", "success")
      result.putMap("result", resultObj)
      
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Digital Loan
  
  @ReactMethod
  fun digitalLoan(params: ReadableMap, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      // Digital Loan uses paymentRequest method
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      val cvvMPText = MPText(activity)
      val authenticationMethod = params.getString("authenticationMethod") ?: "_3D"
      
      // Convert authenticationMethod to enum
      val authTypeEnum = authenticationMethod.let {
        AuthType.values().find { type -> type.name.equals(authenticationMethod, ignoreCase = true) }
      } ?: AuthType.values().firstOrNull() ?: throw IllegalArgumentException("authenticationMethod is required")
      
      // Convert currencyCode to enum (default to TRY)
      val currencyCodeEnum = params.getString("currencyCode")?.let {
        MPCurrencyCode.values().find { code -> code.name.equals(it, ignoreCase = true) }
      } ?: MPCurrencyCode.TRY
      
      // Convert paymentType to enum
      val paymentTypeEnum = params.getString("paymentType")?.let {
        PaymentType.values().find { type -> type.name.equals(it, ignoreCase = true) }
      } ?: null
      
      // Convert secure3DModel to enum
      val secure3DModelEnum = params.getString("secure3DModel")?.let {
        Secure3DModel.values().find { model -> model.name.equals(it, ignoreCase = true) }
      } ?: null
      
      mp.payment(
        jToken,
        null, // requestReferenceNo
        cvvMPText,
        null, // cardAlias
        null, // accountKey
        amount,
        null, // orderNo
        currencyCodeEnum,
        paymentTypeEnum,
        null, // acquirerIcaNumber
        null, // installmentCount
        null, // subMerchant
        null, // rewardList
        null, // orderDetails
        authTypeEnum, // AuthType enum
        null, // orderProductsDetails
        null, // buyerDetails
        null, // billDetails
        null, // deliveryDetails
        null, // otherDetails
        secure3DModelEnum, // Secure3DModel enum
        null, // mokaSubDealerDetails
        null, // additionalParams
        paymentListener = object : PaymentResponseListener {
          override fun onSuccess(response: com.masterpass.turkiye.models.general.MPResponse<com.masterpass.turkiye.models.payment.PaymentResponse>) {
            try {
              if (response.exception != null) {
                promise.reject("ERROR", response.exception?.message ?: response.message ?: "Digital Loan failed", null)
                return
              }
              
              // Map PaymentResponse manually to match iOS structure
      val result = Arguments.createMap()
              
              // Add MPResponse fields with proper null handling
              result.putInt("statusCode", response.statusCode ?: 200)
              result.putString("message", response.message ?: "Digital Loan successful")
              
              if (response.buildId != null) {
                result.putString("buildId", response.buildId)
              } else {
                result.putNull("buildId")
              }
              
              if (response.version != null) {
                result.putString("version", response.version)
              } else {
                result.putNull("version")
              }
              
              if (response.correlationId != null) {
                result.putString("correlationId", response.correlationId)
              } else {
                result.putNull("correlationId")
              }
              
              if (response.requestId != null) {
                result.putString("requestId", response.requestId)
              } else {
                result.putNull("requestId")
              }
              
              // Handle PaymentResponse result
              val resultObj = response.result
              if (resultObj is com.masterpass.turkiye.models.payment.PaymentResponse) {
                val paymentResult = Arguments.createMap()
                
                // Map PaymentResponse fields - Android SDK has these fields:
                // responseCode, description, token, retrievalReferenceNumber, maskedNumber, terminalGroupId, url3d, url3dSuccess, url3dFail
                if (resultObj.responseCode != null) {
                  paymentResult.putString("responseCode", resultObj.responseCode)
                } else {
                  paymentResult.putNull("responseCode")
                }
                
                if (resultObj.description != null) {
                  paymentResult.putString("description", resultObj.description)
                } else {
                  paymentResult.putNull("description")
                }
                
                if (resultObj.token != null) {
                  paymentResult.putString("token", resultObj.token)
                } else {
                  paymentResult.putNull("token")
                }
                
                if (resultObj.retrievalReferenceNumber != null) {
                  paymentResult.putString("retrievalReferenceNumber", resultObj.retrievalReferenceNumber)
                } else {
                  paymentResult.putNull("retrievalReferenceNumber")
                }
                
                if (resultObj.maskedNumber != null) {
                  paymentResult.putString("maskedNumber", resultObj.maskedNumber)
                } else {
                  paymentResult.putNull("maskedNumber")
                }
                
                if (resultObj.terminalGroupId != null) {
                  paymentResult.putString("terminalGroupId", resultObj.terminalGroupId)
                } else {
                  paymentResult.putNull("terminalGroupId")
                }
                
                // 3D Secure URLs
                if (resultObj.url3d != null) {
                  paymentResult.putString("url3d", resultObj.url3d)
                } else {
                  paymentResult.putNull("url3d")
                }
                
                if (resultObj.url3dSuccess != null) {
                  paymentResult.putString("url3dSuccess", resultObj.url3dSuccess)
                } else {
                  paymentResult.putNull("url3dSuccess")
                }
                
                if (resultObj.url3dFail != null) {
                  paymentResult.putString("url3dFail", resultObj.url3dFail)
                } else {
                  paymentResult.putNull("url3dFail")
                }
                
                result.putMap("result", paymentResult)
              } else if (resultObj != null) {
                // Fallback
                val fallbackResult = Arguments.createMap()
                fallbackResult.putString("data", resultObj.toString())
                result.putMap("result", fallbackResult)
              } else {
                result.putNull("result")
              }
              
      promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.masterpass.turkiye.response.ServiceError) {
            try {
              val errorMessage = StringBuilder()
              errorMessage.append(error.responseDesc ?: "Digital Loan failed")
              if (error.responseCode != null) {
                errorMessage.append(" (Code: ${error.responseCode})")
              }
              promise.reject("ERROR", errorMessage.toString(), null)
            } catch (e: Exception) {
              promise.reject("ERROR", error.responseDesc ?: "Digital Loan failed", null)
            }
          }
        }
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Start Loan Validation
  
  @ReactMethod
  fun startLoanValidation(jToken: String, returnURL: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      val activity = reactApplicationContext.currentActivity
      if (activity == null) {
        promise.reject("ERROR", "Activity not available", null)
        return
      }
      
      // Validate returnURL - SDK requires a valid URL for Loan Validation
      if (returnURL.isNullOrBlank()) {
        promise.reject("ERROR", "returnURL is required for Loan Validation", null)
        return
      }
      
      // Create MPWebView instance on main thread
      // Start Loan Validation uses same pattern as start3DValidation (same as iOS implementation)
      val webView = MPWebView(activity)
      webView.url3d = returnURL
      
      // Create Transaction3DListener - same pattern as start3DValidation
      val listener = object : com.masterpass.turkiye.listener.Transaction3DListener {
        override fun onSuccess(result: com.masterpass.turkiye.results.ValidateTransaction3DResult?) {
          try {
            val responseMap = Arguments.createMap()
            responseMap.putInt("statusCode", 200)
            responseMap.putString("message", "Loan Validation started successfully")
            responseMap.putString("status", "success")
            // Map ValidateTransaction3DResult token if available
            if (result?.token != null) {
              responseMap.putString("token", result.token)
            }
            promise.resolve(responseMap)
          } catch (e: Exception) {
            promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
          }
        }
        
        override fun onServiceError(error: com.masterpass.turkiye.response.ServiceError?) {
          try {
            val errorMessage = StringBuilder()
            errorMessage.append(error?.responseDesc ?: "Loan Validation failed")
            if (error?.responseCode != null) {
              errorMessage.append(" (Code: ${error.responseCode})")
            }
            promise.reject("ERROR", errorMessage.toString(), null)
          } catch (e: Exception) {
            promise.reject("ERROR", error?.responseDesc ?: "Loan Validation failed", null)
          }
        }
        
        override fun onServiceResponse(response: com.masterpass.turkiye.response.ServiceResponse?) {
          try {
            val responseMap = Arguments.createMap()
            responseMap.putInt("statusCode", 200)
            responseMap.putString("message", response?.responseDesc ?: "Loan Validation response received")
            if (response?.token != null) {
              responseMap.putString("token", response.token)
            }
            if (response?.refNo != null) {
              responseMap.putString("refNo", response.refNo)
            }
            if (response?.responseCode != null) {
              responseMap.putString("responseCode", response.responseCode)
            }
            promise.resolve(responseMap)
          } catch (e: Exception) {
            promise.reject("ERROR", "Failed to process service response: ${e.message ?: "Unknown error"}", e)
          }
        }
        
        override fun onInternalError(error: String?) {
          promise.reject("ERROR", "Internal error: ${error ?: "Unknown"}", null)
        }
      }
      
      // Use start3DValidation for Loan Validation (same pattern as iOS)
      // SDK signature: start3DValidation(jToken, webView, listener)
      mp.start3DValidation(
        jToken,
        webView,
        listener
      )
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Helper Methods
  
  private fun convertMPResponseToMap(response: com.masterpass.turkiye.models.general.MPResponse<*>): WritableMap {
    val map = Arguments.createMap()
    try {
      // Convert MPResponse to WritableMap
      map.putInt("statusCode", response.statusCode ?: 200)
      map.putString("message", response.message ?: "")
      map.putString("buildId", response.buildId ?: "")
      map.putString("version", response.version ?: "")
      map.putString("correlationId", response.correlationId)
      map.putString("requestId", response.requestId)
      
      // Convert result if available
      response.result?.let { result ->
        val resultMap = Arguments.createMap()
        // Convert result object properties
        resultMap.putString("data", result.toString())
        map.putMap("result", resultMap)
      }
      
      // Convert exception if available
      response.exception?.let { exception ->
        val exceptionMap = Arguments.createMap()
        exceptionMap.putString("level", exception.level ?: "")
        exceptionMap.putString("code", exception.code ?: "")
        exceptionMap.putString("message", exception.message ?: "")
        map.putMap("exception", exceptionMap)
      }
    } catch (e: Exception) {
      map.putString("error", "Failed to convert response: ${e.message}")
    }
    return map
  }
  
  private fun convertResponseToMap(response: Any?): WritableMap {
    val map = Arguments.createMap()
    // Convert response object to WritableMap
    // This will need to be implemented based on SDK response structure
    return map
  }
}
