import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { MasterpassButton } from '../components/MasterpassButton.component';
import { MasterpassResponseDisplay } from '../components/MasterpassResponseDisplay.component';
import MasterpassService from '../services/MasterpassService';
import { MasterpassResponse } from '../interfaces/MasterpassResponse.interface';

// Helper functions for generating random values
const generateRandomString = (length: number): string => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
};

const generateRandomNumber = (min: number, max: number): number => {
  return Math.floor(Math.random() * (max - min + 1)) + min;
};

const generateRandomCardNumber = (): string => {
  // Generate a valid-looking card number (16 digits)
  let cardNumber = '';
  for (let i = 0; i < 16; i++) {
    cardNumber += generateRandomNumber(0, 9).toString();
  }
  return cardNumber;
};

const generateRandomExpiryDate = (): string => {
  const month = generateRandomNumber(1, 12).toString().padStart(2, '0');
  const year = generateRandomNumber(25, 30).toString();
  return `${month}/${year}`;
};

const generateRandomCVV = (): string => {
  return generateRandomNumber(100, 999).toString();
};

const generateRandomRRN = (): string => {
  // RRN is typically 12 digits
  let rrn = '';
  for (let i = 0; i < 12; i++) {
    rrn += generateRandomNumber(0, 9).toString();
  }
  return rrn;
};

// Default config values
const DEFAULT_CONFIG = {
  merchantId: 123456,
  terminalGroupId: undefined,
  language: 'tr-TR',
  url: 'https://mp-test-sdk.masterpassturkiye.com/', // Trailing slash required for SDK to append paths correctly
  cipherText: undefined,
};

export const MasterpassTestScreen: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [response, setResponse] = useState<MasterpassResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleInitialize = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const result = await MasterpassService.initialize({
        merchantId: DEFAULT_CONFIG.merchantId,
        terminalGroupId: DEFAULT_CONFIG.terminalGroupId,
        language: DEFAULT_CONFIG.language,
        url: DEFAULT_CONFIG.url,
        cipherText: DEFAULT_CONFIG.cipherText,
      });

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleAddCard = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomRRN = generateRandomRRN();
      const randomUserId = `user-${generateRandomString(12)}`;
      const randomCardNumber = generateRandomCardNumber();
      const randomExpiryDate = generateRandomExpiryDate();
      const randomCVV = generateRandomCVV();
      const randomCardHolderName = `Test User ${generateRandomString(8)}`;
      const randomCardAlias = `Card ${generateRandomString(6)}`;

      const result = await MasterpassService.addCard({
        jToken: randomJToken,
        accountKey: undefined,
        accountKeyType: undefined,
        rrn: randomRRN,
        userId: randomUserId,
        card: {
          cardNumber: randomCardNumber,
          expiryDate: randomExpiryDate,
          cvv: randomCVV,
          cardHolderName: randomCardHolderName,
        },
        cardAlias: randomCardAlias,
        isMsisdnValidatedByMerchant: false,
        authenticationMethod: undefined,
        additionalParams: undefined,
      });

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleLinkAccountToMerchant = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;

      const result = await MasterpassService.linkAccountToMerchant(
        randomJToken,
        randomAccountKey,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleAccountAccess = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;
      const randomUserId = `user-${generateRandomString(12)}`;
      const accountKeyType = 'MSISDN'; // or 'ID'

      const result = await MasterpassService.accountAccess(
        randomJToken,
        randomAccountKey,
        accountKeyType,
        randomUserId,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleRemoveCard = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;
      const randomCardAlias = `card-${generateRandomString(10)}`;

      const result = await MasterpassService.removeCard(
        randomJToken,
        randomAccountKey,
        randomCardAlias,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleUpdateUserId = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;
      const randomCurrentUserId = `user-${generateRandomString(12)}`;
      const randomNewUserId = `user-${generateRandomString(12)}-new`;

      const result = await MasterpassService.updateUserId(
        randomJToken,
        randomAccountKey,
        randomCurrentUserId,
        randomNewUserId,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleUpdateUserMsisdn = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;
      const randomNewMsisdn = `5${generateRandomNumber(1000000000, 9999999999)}`; // Turkish phone format

      const result = await MasterpassService.updateUserMsisdn(
        randomJToken,
        randomAccountKey,
        randomNewMsisdn,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleAddUserId = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomAccountKey = `account-${generateRandomString(12)}`;
      const randomCurrentUserId = `user-${generateRandomString(12)}`;
      const randomNewUserId = `user-${generateRandomString(12)}-new`;

      const result = await MasterpassService.addUserId(
        randomJToken,
        randomAccountKey,
        randomCurrentUserId,
        randomNewUserId,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleVerify = useCallback(async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      // Generate random values for each call
      const randomJToken = `jtoken-${generateRandomString(16)}-${Date.now()}`;
      const randomOtp = generateRandomNumber(100000, 999999).toString(); // 6-digit OTP

      const result = await MasterpassService.verify(
        randomJToken,
        randomOtp,
      );

      setResponse(result);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Bilinmeyen hata';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleClear = useCallback(() => {
    setResponse(null);
    setError(null);
  }, []);

  return (
    <View style={styles.container}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}>
        <View style={styles.headerContainer}>
          <Text style={styles.title}>Masterpass SDK Test</Text>
          <Text style={styles.subtitle}>Functions</Text>
        </View>

        <View style={styles.buttonContainer}>
          <MasterpassButton
            title="Initialize SDK"
            onPress={handleInitialize}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Add Card"
            onPress={handleAddCard}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Link Account To Merchant"
            onPress={handleLinkAccountToMerchant}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Account Access"
            onPress={handleAccountAccess}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Remove Card"
            onPress={handleRemoveCard}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Update User ID"
            onPress={handleUpdateUserId}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Update User MSISDN"
            onPress={handleUpdateUserMsisdn}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Add User ID"
            onPress={handleAddUserId}
            loading={loading}
            disabled={loading}
          />

          <MasterpassButton
            title="Verify OTP"
            onPress={handleVerify}
            loading={loading}
            disabled={loading}
          />

          {(response || error) && (
            <MasterpassButton
              title="Clear Response"
              onPress={handleClear}
              disabled={loading}
            />
          )}
        </View>

        <MasterpassResponseDisplay response={response} error={error} />
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
  },
  headerContainer: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#000000',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 16,
    color: '#666666',
  },
  buttonContainer: {
    marginBottom: 16,
  },
});

