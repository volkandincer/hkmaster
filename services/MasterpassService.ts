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
}

export default new MasterpassService();

