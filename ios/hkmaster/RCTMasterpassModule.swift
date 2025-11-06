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
    // Initialize SDK
    MasterPass.initialize(
      merchantId: merchantId.intValue,
      terminalGroupId: terminalGroupId,
      language: language ?? "en-US",
      url: url,
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
      "url": url,
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
    resolver(["statusCode": 200, "message": "Add Card - Not implemented yet", "jToken": jToken])
  }
  
  // MARK: - Link Account To Merchant
  
  @objc func linkAccountToMerchant(_ jToken: String, accountKey: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Link Account To Merchant - Not implemented yet"])
  }
  
  // MARK: - Account Access
  
  @objc func accountAccess(_ jToken: String, accountKey: String?, accountKeyType: String?, userId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Account Access - Not implemented yet"])
  }
  
  // MARK: - Remove Card
  
  @objc func removeCard(_ jToken: String, accountKey: String?, cardAlias: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Remove Card - Not implemented yet"])
  }
  
  // MARK: - Update User ID
  
  @objc func updateUserId(_ jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Update User ID - Not implemented yet"])
  }
  
  // MARK: - Update User MSISDN
  
  @objc func updateUserMsisdn(_ jToken: String, accountKey: String?, newMsisdn: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Update User MSISDN - Not implemented yet"])
  }
  
  // MARK: - Add User ID
  
  @objc func addUserId(_ jToken: String, accountKey: String?, currentUserId: String?, newUserId: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Add User ID - Not implemented yet"])
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
    resolver(["statusCode": 200, "message": "Verify - Not implemented yet"])
  }
  
  // MARK: - Resend OTP
  
  @objc func resendOtp(_ jToken: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Resend OTP - Not implemented yet"])
  }
  
  // MARK: - Start 3D Validation
  
  @objc func start3DValidation(_ jToken: String, returnURL: String?, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Start 3D Validation - Not implemented yet"])
  }
  
  // MARK: - Payment
  
  @objc func payment(_ params: NSDictionary, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    resolver(["statusCode": 200, "message": "Payment - Not implemented yet"])
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
