import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { MasterpassButton } from '../components/MasterpassButton.component';
import { MasterpassResponseDisplay } from '../components/MasterpassResponseDisplay.component';
import MasterpassService from '../services/MasterpassService';
import { MasterpassResponse } from '../interfaces/MasterpassResponse.interface';

// Default değerler - Buraya test için gerçek değerleri ekleyebilirsiniz
const DEFAULT_CONFIG = {
  merchantId: 123456,
  terminalGroupId: undefined,
  language: 'tr-TR',
  url: 'https://api.masterpass.com',
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
          <Text style={styles.subtitle}>Initialize Function</Text>
        </View>

        <View style={styles.buttonContainer}>
          <MasterpassButton
            title="Initialize SDK"
            onPress={handleInitialize}
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

