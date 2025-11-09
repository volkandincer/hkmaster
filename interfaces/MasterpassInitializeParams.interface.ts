export interface MasterpassInitializeParams {
  merchantId: number;
  terminalGroupId?: string;
  language?: string;
  url: string;
  // iOS only
  cipherText?: string;
  // Android only
  verbose?: boolean;
  merchantSecretKey?: string;
}

