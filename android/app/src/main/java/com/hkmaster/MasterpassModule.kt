package com.hkmaster

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableArray

import com.paycore.masterpass.MasterPass
import com.paycore.masterpass.enums.AccountKeyType
import com.paycore.masterpass.enums.AuthType
import com.paycore.masterpass.enums.MPCurrencyCode
import com.paycore.masterpass.listener.*
import com.paycore.masterpass.services.AccountServices
import com.paycore.masterpass.mp.MPCard
import com.paycore.masterpass.view.MPText
import com.paycore.masterpass.view.MPCheckBox
import android.app.Activity

class MasterpassModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  
  private var masterPassInstance: MasterPass? = null
  
  override fun getName(): String {
    return "MasterpassModule"
  }
  
  // MARK: - Initialize
  
  @ReactMethod
  fun initialize(merchantId: Int, terminalGroupId: String?, language: String?, url: String, cipherText: String?, promise: Promise) {
    try {
      // Create MasterPass instance
      masterPassInstance = MasterPass(
        mId = merchantId.toLong(),
        tGId = terminalGroupId ?: "",
        lan = language ?: "en-US",
        verbose = false,
        bUrl = url
      )
      
      // Set cipherText if provided (if SDK supports it via Companion)
      cipherText?.let {
        // Handle cipherText if SDK supports it
      }
      
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
      result.putString("language", language ?: "en-US")
      result.putString("url", url)
      // Use null instead of empty string to match iOS behavior
      if (cipherText != null && cipherText.isNotBlank()) {
        result.putString("cipherText", cipherText)
      } else {
        result.putNull("cipherText")
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
      val cardNoMPText = MPText(activity)
      cardNoMPText.setText(cardNumber)
      
      val cvvMPText = MPText(activity)
      cvvMPText.setText(cvv)
      
      // Create MPCheckBox and set it to checked (required by SDK)
      // SDK requires terms and conditions checkbox to be selected
      val checkBox = MPCheckBox(activity)
      checkBox.isChecked = true
      
      // Create MPCard object - use non-null values for required fields
      val mpCard = MPCard(
        cardNo = cardNoMPText,
        cvv = cvvMPText,
        cardHolder = cardHolderName ?: "",
        cardAlias = cardAlias ?: "",
        date = expiryDate,
        checkBox = checkBox
      )
      
      // Call AccountServices.saveCard with correct parameter format
      // Note: SDK uses saveCard method, but we format parameters according to addCard signature
      AccountServices.Companion.saveCard(
        jToken = jToken,
        accountKey = accountKey?.takeIf { it.isNotBlank() } ?: "",
        accountKeyType = accountKeyTypeEnum,
        rrn = rrn?.takeIf { it.isNotBlank() } ?: "",
        card = mpCard,
        saveCardListener = object : SaveCardListener {
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.CardSaveResponse>) {
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
              
              // Wrap CardSaveResponse in a result object to match TypeScript interface
              val resultObj = response.result
              if (resultObj is com.paycore.masterpass.models.account.CardSaveResponse) {
                val cardSaveResult = Arguments.createMap()
                
                // Handle nullable fields properly
                if (resultObj.token != null) {
                  cardSaveResult.putString("token", resultObj.token)
                } else {
                  cardSaveResult.putNull("token")
                }
                
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
                
                if (resultObj.resultDescription != null) {
                  cardSaveResult.putString("resultDescription", resultObj.resultDescription)
                } else {
                  cardSaveResult.putNull("resultDescription")
                }
                
                cardSaveResult.putString("jToken", jToken)
                
                if (cardAlias != null) {
                  cardSaveResult.putString("cardAlias", cardAlias)
                } else {
                  cardSaveResult.putNull("cardAlias")
                }
                
                result.putMap("result", cardSaveResult)
              } else if (resultObj != null) {
                // Fallback: if result is not CardSaveResponse, wrap it in result object
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.LinkToMerchantResponse>) {
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
              
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
            } catch (e: Exception) {
              promise.reject("ERROR", "Failed to process response: ${e.message ?: "Unknown error"}", e)
            }
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
      
      mp.getCard(
        jToken = jToken,
        accountKey = accountKey ?: "",
        accountKeyType = accountKeyTypeEnum,
        merchantUserId = userId ?: "",
        cardRequestListener = object : CardRequestListener {
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.CardResponse>) {
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
              if (resultObj is com.paycore.masterpass.models.account.CardResponse) {
                val cardResponseResult = Arguments.createMap()
                
                cardResponseResult.putString("accountKey", resultObj.accountKey)
                cardResponseResult.putString("accountState", resultObj.accountState)
                
                // Convert cards array
                val cardsArray = Arguments.createArray()
                for (card in resultObj.cards) {
                  val cardMap = Arguments.createMap()
                  cardMap.putString("cardAlias", card.cardAlias)
                  cardMap.putString("cardState", card.cardState)
                  cardMap.putString("maskedCardNumber", card.maskedCardNumber)
                  cardMap.putString("uniqueCardNumber", card.uniqueCardNumber)
                  cardMap.putString("cardType", card.cardType)
                  
                  if (card.productName != null) {
                    cardMap.putString("productName", card.productName)
                  } else {
                    cardMap.putNull("productName")
                  }
                  
                  cardMap.putString("cardBin", card.cardBin)
                  
                  if (card.cardIssuerIcaNumber != null) {
                    cardMap.putString("cardIssuerIcaNumber", card.cardIssuerIcaNumber)
                  } else {
                    cardMap.putNull("cardIssuerIcaNumber")
                  }
                  
                  cardMap.putString("cardValidationType", card.cardValidationType)
                  cardMap.putBoolean("isDefaultCard", card.isDefaultCard)
                  cardMap.putBoolean("expireSoon", card.expireSoon)
                  cardMap.putBoolean("isExpired", card.isExpired)
                  cardMap.putBoolean("isMasterpassMember", card.isMasterpassMember)
                  
                  cardsArray.pushMap(cardMap)
                }
                cardResponseResult.putArray("cards", cardsArray)
                
                // Android SDK CardResponse doesn't have accountInformation or recipientCards
                // These fields are iOS-specific, so we set them to null for Android
                cardResponseResult.putNull("accountInformation")
                cardResponseResult.putNull("recipientCards")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.RemoveCardResponse>) {
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
              if (resultObj is com.paycore.masterpass.models.account.RemoveCardResponse) {
                val removeCardResult = Arguments.createMap()
                
                // RemoveCardResponse typically contains success confirmation
                // Map any fields from RemoveCardResponse if available
                removeCardResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.UpdateUserIdResponse>) {
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
              
              // Handle UpdateUserIdResponse result
              val resultObj = response.result
              if (resultObj is com.paycore.masterpass.models.account.UpdateUserIdResponse) {
                val updateUserIdResult = Arguments.createMap()
                
                // UpdateUserIdResponse typically contains success confirmation
                // Map any fields from UpdateUserIdResponse if available
                updateUserIdResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.UpdateUserMsisdnResponse>) {
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
              
              // Handle UpdateUserMsisdnResponse result
              val resultObj = response.result
              if (resultObj is com.paycore.masterpass.models.account.UpdateUserMsisdnResponse) {
                val updateUserMsisdnResult = Arguments.createMap()
                
                // UpdateUserMsisdnResponse typically contains success confirmation
                // Map any fields from UpdateUserMsisdnResponse if available
                updateUserMsisdnResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.account.AddUserIdResponse>) {
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
              
              // Handle AddUserIdResponse result
              val resultObj = response.result
              if (resultObj is com.paycore.masterpass.models.account.AddUserIdResponse) {
                val addUserIdResult = Arguments.createMap()
                
                // AddUserIdResponse typically contains success confirmation
                // Map any fields from AddUserIdResponse if available
                addUserIdResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Recurring Order Register - Bridge working")
      result.putString("rrn", rrn)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Recurring Order Update
  
  @ReactMethod
  fun recurringOrderUpdate(jToken: String, accountKey: String?, cardAlias: String?, productId: String?, amountLimit: String?, expireDate: String, rrn: String, promise: Promise) {
    try {
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Recurring Order Update - Bridge working")
      result.putString("rrn", rrn)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Recurring Order Delete
  
  @ReactMethod
  fun recurringOrderDelete(jToken: String, accountKey: String?, accountChangedEventName: String?, cardAlias: String?, productId: String?, authenticationMethod: String?, rrn: String, promise: Promise) {
    try {
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Recurring Order Delete - Bridge working")
      result.putString("rrn", rrn)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Verify
  
  @ReactMethod
  fun verify(jToken: String, otp: String, promise: Promise) {
    try {
      val mp = getMasterPassInstance()
      mp.verify(
        jToken = jToken,
        otpCode = otp,
        verifyListener = object : VerifyListener {
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.validateflow.VerifyResponse>) {
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
              if (resultObj is com.paycore.masterpass.models.validateflow.VerifyResponse) {
                val verifyResult = Arguments.createMap()
                
                // VerifyResponse typically contains success confirmation
                // Map any fields from VerifyResponse if available
                verifyResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
      val mp = getMasterPassInstance()
      mp.resendOtp(
        jToken = jToken,
        resendOtpListener = object : ResendOtpListener {
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.validateflow.ResendOtpResponse>) {
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
              
              // Handle ResendOtpResponse result
              val resultObj = response.result
              if (resultObj is com.paycore.masterpass.models.validateflow.ResendOtpResponse) {
                val resendOtpResult = Arguments.createMap()
                
                // ResendOtpResponse typically contains success confirmation
                // Map any fields from ResendOtpResponse if available
                resendOtpResult.putString("status", "success")
                
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
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
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
              if (!error.mdErrorMsg.isNullOrBlank()) {
                errorMessage.append(" [MD Error: ${error.mdErrorMsg}]")
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
      // Android SDK uses MPWebView for 3D Secure validation
      // Found: Transaction3DListener and MPWebView.loadUrl(Transaction3DListener) exist in SDK
      // However, 3D Secure URL typically comes from payment response
      // Full implementation requires:
      // 1. Get 3D Secure URL from payment response (paymentRequest/directPayment)
      // 2. Create MPWebView instance
      // 3. Set Transaction3DListener via webView.callback
      // 4. Call MPWebView.loadUrl(Transaction3DListener) with the URL
      // 
      // For now, this is a placeholder that indicates MPWebView and Transaction3DListener are available
      // but the 3D Secure URL needs to come from payment flow
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Start 3D Validation - Bridge working (MPWebView and Transaction3DListener available, but 3D Secure URL required from payment response)")
      result.putString("jToken", jToken)
      if (returnURL != null) {
        result.putString("returnURL", returnURL)
      } else {
        result.putNull("returnURL")
      }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Payment
  
  @ReactMethod
  fun payment(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      val orderNo = params.getString("orderNo")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Payment - Bridge working")
      result.putString("jToken", jToken)
      result.putString("amount", amount)
      result.putString("orderNo", orderNo)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Direct Payment
  
  @ReactMethod
  fun directPayment(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Direct Payment - Bridge working")
      result.putString("jToken", jToken)
      result.putString("amount", amount)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Register And Purchase
  
  @ReactMethod
  fun registerAndPurchase(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Register And Purchase - Bridge working")
      result.putString("jToken", jToken)
      result.putString("amount", amount)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - QR Payment
  
  @ReactMethod
  fun qrPayment(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "QR Payment - Bridge working")
      result.putString("jToken", jToken)
      result.putString("amount", amount)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Money Send
  
  @ReactMethod
  fun moneySend(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val moneySendType = params.getString("moneySendType")
      val amount = params.getString("amount")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Money Send - Bridge working")
      result.putString("jToken", jToken)
      result.putString("moneySendType", moneySendType)
      result.putString("amount", amount)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Complete Registration
  
  @ReactMethod
  fun completeRegistration(jToken: String, accountKey: String?, accountAlias: String, isMsisdnValidatedByMerchant: Boolean?, responseToken: String?, promise: Promise) {
    try {
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Complete Registration - Bridge working")
      result.putString("accountAlias", accountAlias)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Digital Loan
  
  @ReactMethod
  fun digitalLoan(params: ReadableMap, promise: Promise) {
    try {
      val jToken = params.getString("jToken") ?: throw IllegalArgumentException("jToken is required")
      val amount = params.getString("amount")
      
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Digital Loan - Bridge working")
      result.putString("jToken", jToken)
      result.putString("amount", amount)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Start Loan Validation
  
  @ReactMethod
  fun startLoanValidation(jToken: String, returnURL: String, promise: Promise) {
    try {
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Start Loan Validation - Bridge working (MPWebView needed for full implementation)")
      result.putString("jToken", jToken)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
    }
  }
  
  // MARK: - Helper Methods
  
  private fun convertMPResponseToMap(response: com.paycore.masterpass.models.general.MPResponse<*>): WritableMap {
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
