import { NativeModules } from 'react-native';
import { MasterpassInitializeParams } from '../interfaces/MasterpassInitializeParams.interface';
import { MasterpassAddCardParams } from '../interfaces/MasterpassAddCardParams.interface';
import { MasterpassResponse } from '../interfaces/MasterpassResponse.interface';

const { MasterpassModule } = NativeModules;

if (!MasterpassModule) {
  console.warn('MasterpassModule native module is not available');
}

class MasterpassService {
  /**
   * Initialize Masterpass SDK
   */
  async initialize(
    params: MasterpassInitializeParams,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      const response = await MasterpassModule.initialize(
        params.merchantId,
        params.terminalGroupId || null,
        params.language || null,
        params.url,
        params.cipherText || null,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass initialize failed: ${errorMessage}`);
    }
  }

  /**
   * Add Card to Masterpass
   */
  async addCard(
    params: MasterpassAddCardParams,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate and provide defaults for required card fields
      if (!params.card) {
        throw new Error('Card information is required');
      }

      // Ensure card fields have default values and are not empty
      // Remove any spaces or special characters from card number
      const cardNumber = (params.card.cardNumber || '').replace(/\s+/g, '');
      
      // Normalize expiry date format (MM/YY or MMYY -> MMYY)
      let expiryDate = (params.card.expiryDate || '').replace(/\s+/g, '');
      if (expiryDate.includes('/')) {
        expiryDate = expiryDate.replace('/', '');
      }
      
      const cvv = (params.card.cvv || '').replace(/\s+/g, '');
      const cardHolderName = (params.card.cardHolderName || '').trim();

      const card = {
        cardNumber: cardNumber,
        expiryDate: expiryDate,
        cvv: cvv,
        cardHolderName: cardHolderName,
      };

      // Validate required fields
      if (!card.cardNumber || card.cardNumber.length === 0) {
        throw new Error('Card number is required');
      }

      if (!card.expiryDate || card.expiryDate.length === 0) {
        throw new Error('Expiry date is required');
      }

      if (!card.cvv || card.cvv.length === 0) {
        throw new Error('CVV is required');
      }

      // Validate card number format (should be 13-19 digits)
      if (!/^\d{13,19}$/.test(card.cardNumber)) {
        throw new Error(`Card number must be 13-19 digits. Received: ${card.cardNumber.length} digits`);
      }

      // Validate expiry date format (should be 4 digits: MMYY)
      if (!/^\d{4}$/.test(card.expiryDate)) {
        throw new Error(`Expiry date must be 4 digits (MMYY). Received: '${card.expiryDate}'`);
      }

      // Validate CVV format (should be 3-4 digits)
      if (!/^\d{3,4}$/.test(card.cvv)) {
        throw new Error(`CVV must be 3-4 digits. Received: '${card.cvv}'`);
      }

      // Ensure jToken is provided
      if (!params.jToken || params.jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      const response = await MasterpassModule.addCard(
        params.jToken,
        params.accountKey || null,
        params.accountKeyType || null,
        params.rrn || null,
        params.userId || null,
        card, // Use validated card object
        params.cardAlias || null,
        params.isMsisdnValidatedByMerchant ?? false,
        params.authenticationMethod || null,
        params.additionalParams || null,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass addCard failed: ${errorMessage}`);
    }
  }

  /**
   * Link Account To Merchant
   */
  async linkAccountToMerchant(
    jToken: string,
    accountKey?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      const response = await MasterpassModule.linkAccountToMerchant(
        jToken,
        accountKey || null,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(
        `Masterpass linkAccountToMerchant failed: ${errorMessage}`,
      );
    }
  }

  /**
   * Account Access - Get card information
   */
  async accountAccess(
    jToken: string,
    accountKey?: string,
    accountKeyType?: string,
    userId?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate accountKeyType if provided
      if (accountKeyType && !['MSISDN', 'ID'].includes(accountKeyType.toUpperCase())) {
        throw new Error('accountKeyType must be either "MSISDN" or "ID"');
      }

      const response = await MasterpassModule.accountAccess(
        jToken,
        accountKey || null,
        accountKeyType || null,
        userId || null,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass accountAccess failed: ${errorMessage}`);
    }
  }

  /**
   * Remove Card from Masterpass
   */
  async removeCard(
    jToken: string,
    accountKey?: string,
    cardAlias?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      const response = await MasterpassModule.removeCard(
        jToken,
        accountKey || null,
        cardAlias || null,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass removeCard failed: ${errorMessage}`);
    }
  }

  /**
   * Update User ID in Masterpass
   */
  async updateUserId(
    jToken: string,
    accountKey?: string,
    currentUserId?: string,
    newUserId?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate newUserId
      if (!newUserId || newUserId.trim().length === 0) {
        throw new Error('newUserId is required');
      }

      const response = await MasterpassModule.updateUserId(
        jToken,
        accountKey || null,
        currentUserId || null,
        newUserId,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass updateUserId failed: ${errorMessage}`);
    }
  }

  /**
   * Update User MSISDN in Masterpass
   */
  async updateUserMsisdn(
    jToken: string,
    accountKey?: string,
    newMsisdn?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate newMsisdn
      if (!newMsisdn || newMsisdn.trim().length === 0) {
        throw new Error('newMsisdn is required');
      }

      const response = await MasterpassModule.updateUserMsisdn(
        jToken,
        accountKey || null,
        newMsisdn,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass updateUserMsisdn failed: ${errorMessage}`);
    }
  }

  /**
   * Add User ID to Masterpass
   */
  async addUserId(
    jToken: string,
    accountKey?: string,
    currentUserId?: string,
    newUserId?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate newUserId
      if (!newUserId || newUserId.trim().length === 0) {
        throw new Error('newUserId is required');
      }

      const response = await MasterpassModule.addUserId(
        jToken,
        accountKey || null,
        currentUserId || null,
        newUserId,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass addUserId failed: ${errorMessage}`);
    }
  }

  /**
   * Verify OTP in Masterpass
   */
  async verify(
    jToken: string,
    otp: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate otp
      if (!otp || otp.trim().length === 0) {
        throw new Error('otp is required');
      }

      const response = await MasterpassModule.verify(
        jToken,
        otp,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass verify failed: ${errorMessage}`);
    }
  }

  /**
   * Resend OTP in Masterpass
   */
  async resendOtp(
    jToken: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      const response = await MasterpassModule.resendOtp(
        jToken,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass resendOtp failed: ${errorMessage}`);
    }
  }

  /**
   * Start 3D Validation in Masterpass
   */
  async start3DValidation(
    jToken: string,
    returnURL?: string,
  ): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!jToken || jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate returnURL - SDK requires a valid URL, not empty string
      // Even though documentation says it's optional, SDK throws error if empty
      if (!returnURL || returnURL.trim().length === 0) {
        throw new Error('returnURL is required for 3D Validation');
      }

      const response = await MasterpassModule.start3DValidation(
        jToken,
        returnURL,
      );

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass start3DValidation failed: ${errorMessage}`);
    }
  }

  /**
   * Payment Request
   */
  async payment(params: {
    jToken: string;
    accountKey?: string;
    cardAlias?: string;
    amount?: string;
    orderNo?: string;
    rrn?: string;
    cvv?: string;
    currencyCode?: string;
    paymentType?: string;
    acquirerIcaNumber?: string;
    installmentCount?: number;
    authenticationMethod?: string;
    secure3DModel?: string;
  }): Promise<MasterpassResponse> {
    if (!MasterpassModule) {
      throw new Error('MasterpassModule is not available');
    }

    try {
      // Validate jToken
      if (!params.jToken || params.jToken.trim().length === 0) {
        throw new Error('jToken is required');
      }

      // Validate required fields for iOS (Android may have different requirements)
      if (!params.accountKey || params.accountKey.trim().length === 0) {
        throw new Error('accountKey is required');
      }
      if (!params.cardAlias || params.cardAlias.trim().length === 0) {
        throw new Error('cardAlias is required');
      }
      if (!params.amount || params.amount.trim().length === 0) {
        throw new Error('amount is required');
      }
      if (!params.orderNo || params.orderNo.trim().length === 0) {
        throw new Error('orderNo is required');
      }
      if (!params.rrn || params.rrn.trim().length === 0) {
        throw new Error('rrn is required');
      }
      if (!params.cvv || params.cvv.trim().length === 0) {
        throw new Error('cvv is required');
      }
      if (!params.currencyCode || params.currencyCode.trim().length === 0) {
        throw new Error('currencyCode is required');
      }
      if (!params.paymentType || params.paymentType.trim().length === 0) {
        throw new Error('paymentType is required');
      }
      if (
        !params.authenticationMethod ||
        params.authenticationMethod.trim().length === 0
      ) {
        throw new Error('authenticationMethod is required');
      }

      const response = await MasterpassModule.payment({
        jToken: params.jToken,
        accountKey: params.accountKey,
        cardAlias: params.cardAlias,
        amount: params.amount,
        orderNo: params.orderNo,
        rrn: params.rrn,
        cvv: params.cvv,
        currencyCode: params.currencyCode,
        paymentType: params.paymentType,
        acquirerIcaNumber: params.acquirerIcaNumber || null,
        installmentCount: params.installmentCount || 0,
        authenticationMethod: params.authenticationMethod,
        secure3DModel: params.secure3DModel || null,
      });

      return response as MasterpassResponse;
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      throw new Error(`Masterpass payment failed: ${errorMessage}`);
    }
  }
}

export default new MasterpassService();

