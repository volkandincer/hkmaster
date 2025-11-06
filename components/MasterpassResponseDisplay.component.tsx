import React from 'react';
import { View, Text, ScrollView, StyleSheet } from 'react-native';
import { MasterpassResponse } from '../interfaces/MasterpassResponse.interface';

interface MasterpassResponseDisplayProps {
  response: MasterpassResponse | null;
  error: string | null;
}

export const MasterpassResponseDisplay: React.FC<
  MasterpassResponseDisplayProps
> = ({ response, error }) => {
  if (!response && !error) {
    return null;
  }

  return (
    <ScrollView style={styles.container} nestedScrollEnabled>
      <View style={styles.content}>
        {error ? (
          <View style={styles.errorContainer}>
            <Text style={styles.errorTitle}>Error</Text>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        ) : response ? (
          <View style={styles.successContainer}>
            <Text style={styles.successTitle}>Response</Text>
            <Text style={styles.responseText}>
              {JSON.stringify(response, null, 2)}
            </Text>
          </View>
        ) : null}
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginTop: 16,
  },
  content: {
    padding: 16,
  },
  errorContainer: {
    backgroundColor: '#FFEBEE',
    padding: 16,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#F44336',
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#C62828',
    marginBottom: 8,
  },
  errorText: {
    fontSize: 14,
    color: '#B71C1C',
    fontFamily: 'monospace',
  },
  successContainer: {
    backgroundColor: '#E8F5E9',
    padding: 16,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#4CAF50',
  },
  successTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#2E7D32',
    marginBottom: 8,
  },
  responseText: {
    fontSize: 12,
    color: '#1B5E20',
    fontFamily: 'monospace',
  },
});

