import { NativeModules } from 'react-native';
import { MasterpassInitializeParams } from '../interfaces/MasterpassInitializeParams.interface';
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
}

export default new MasterpassService();

