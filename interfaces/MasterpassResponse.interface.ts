export interface MasterpassException {
  level?: string;
  code?: string;
  message?: string;
  vposErrorCode?: string;
  vposErrorMessage?: string;
  retrievalReferenceNumber?: string;
  thirdPartyApiResponse?: string;
  acquirerIcaNumber?: string;
}

export interface MasterpassResponse<T = Record<string, unknown>> {
  version?: string;
  buildId?: string;
  statusCode?: number;
  message?: string;
  correlationId?: string;
  requestId?: string;
  exception?: MasterpassException;
  result?: T;
  success?: boolean;
  merchantId?: number;
  terminalGroupId?: string;
  language?: string;
}

