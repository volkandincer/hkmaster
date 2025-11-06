export interface MasterpassAddCardParams {
  jToken: string;
  accountKey?: string;
  accountKeyType?: string;
  rrn?: string;
  userId?: string;
  card?: {
    cardNumber?: string;
    expiryDate?: string;
    cvv?: string;
    cardHolderName?: string;
  };
  cardAlias?: string;
  isMsisdnValidatedByMerchant?: boolean;
  authenticationMethod?: string;
  additionalParams?: Record<string, string>;
}

