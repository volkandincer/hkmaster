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
      // TODO: Real SDK implementation
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Add Card - Bridge working")
      result.putString("jToken", jToken)
      result.putString("cardAlias", cardAlias)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message ?: "Unknown error", e)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Link Account To Merchant failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Account Access failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Remove Card failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Update User ID failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Update User MSISDN failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Add User ID failed", null)
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
      // Note: SDK requires MPText, but we'll pass OTP as string for now
      // TODO: Create MPText instance from OTP string
      mp.verify(
        jToken = jToken,
        otpCode = otp,
        verifyListener = object : VerifyListener {
          override fun onSuccess(response: com.paycore.masterpass.models.general.MPResponse<com.paycore.masterpass.models.validateflow.VerifyResponse>) {
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Verify failed", null)
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
            val result = convertMPResponseToMap(response)
            promise.resolve(result)
          }
          
          override fun onFailed(error: com.paycore.masterpass.response.ServiceError) {
            promise.reject("ERROR", error.responseDesc ?: "Resend OTP failed", null)
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
      val result = Arguments.createMap()
      result.putInt("statusCode", 200)
      result.putString("message", "Start 3D Validation - Bridge working (MPWebView needed for full implementation)")
      result.putString("jToken", jToken)
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
