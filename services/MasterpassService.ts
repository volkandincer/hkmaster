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
      const response = await MasterpassModule.addCard(
        params.jToken,
        params.accountKey || null,
        params.accountKeyType || null,
        params.rrn || null,
        params.userId || null,
        params.card || null,
        params.cardAlias || null,
        params.isMsisdnValidatedByMerchant || null,
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
}

export default new MasterpassService();

