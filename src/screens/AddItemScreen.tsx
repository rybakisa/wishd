import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp, NativeStackScreenProps } from '@react-navigation/native-stack';
import { supabase } from '../lib/supabase';
import { useAuth } from '../hooks/useAuth';
import { RootStackParamList } from '../types';

type Props = NativeStackScreenProps<RootStackParamList, 'AddItem'>;
type Nav = NativeStackNavigationProp<RootStackParamList, 'AddItem'>;

const CURRENCIES = ['USD', 'EUR', 'GBP', 'CAD', 'AUD'];

interface ParsedProduct {
  name: string;
  image_url: string | null;
  price: string;
  currency: string;
  description: string;
}

export function AddItemScreen({ route }: Props) {
  const { wishlistId } = route.params;
  const navigation = useNavigation<Nav>();
  const { session } = useAuth();

  const [url, setUrl] = useState('');
  const [name, setName] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [size, setSize] = useState('');
  const [comment, setComment] = useState('');
  const [parsing, setParsing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [urlParsed, setUrlParsed] = useState(false);

  const handleParseUrl = async () => {
    if (!url.trim()) return;
    setParsing(true);
    try {
      const apiUrl = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:4000';
      const res = await fetch(`${apiUrl}/api/parse-url?url=${encodeURIComponent(url.trim())}`);
      if (!res.ok) throw new Error('Parse failed');
      const data: ParsedProduct = await res.json();
      if (data.name) setName(data.name);
      if (data.image_url) setImageUrl(data.image_url);
      if (data.price) setPrice(data.price);
      if (data.currency) setCurrency(data.currency);
      if (data.description) setDescription(data.description);
      setUrlParsed(true);
    } catch {
      // Graceful fallback: just keep the URL, let user fill in manually
      setUrlParsed(false);
      Alert.alert(
        'Could not auto-fill',
        'We couldn\'t extract product details from that URL. Please fill in manually.',
        [{ text: 'OK' }]
      );
    } finally {
      setParsing(false);
    }
  };

  const handleSave = async () => {
    if (!name.trim()) {
      Alert.alert('Name required', 'Please enter a name for this item.');
      return;
    }
    if (!session) return;

    setSaving(true);
    try {
      const { error } = await supabase.from('wishlist_items').insert({
        wishlist_id: wishlistId,
        name: name.trim(),
        url: url.trim() || null,
        image_url: imageUrl.trim() || null,
        description: description.trim() || null,
        price: price ? parseFloat(price) : null,
        currency: price ? currency : null,
        size: size.trim() || null,
        comment: comment.trim() || null,
        sort_order: Date.now(),
      });
      if (error) throw error;
      navigation.goBack();
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      {/* URL paste + parse */}
      <Text style={styles.label}>Product URL</Text>
      <View style={styles.urlRow}>
        <TextInput
          style={[styles.input, styles.urlInput]}
          placeholder="Paste a product link to auto-fill"
          placeholderTextColor="#9ca3af"
          value={url}
          onChangeText={setUrl}
          keyboardType="url"
          autoCapitalize="none"
          autoCorrect={false}
        />
        <TouchableOpacity
          style={[styles.parseButton, (!url.trim() || parsing) && styles.disabled]}
          onPress={handleParseUrl}
          disabled={!url.trim() || parsing}
        >
          {parsing ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.parseButtonText}>Fill</Text>
          )}
        </TouchableOpacity>
      </View>
      {urlParsed && (
        <Text style={styles.parsedBadge}>✓ Auto-filled from product page</Text>
      )}

      {/* Name */}
      <Text style={styles.label}>Gift name *</Text>
      <TextInput
        style={styles.input}
        placeholder="e.g. AirPods Pro"
        placeholderTextColor="#9ca3af"
        value={name}
        onChangeText={setName}
        maxLength={120}
      />

      {/* Description */}
      <Text style={styles.label}>Description</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        placeholder="Optional notes"
        placeholderTextColor="#9ca3af"
        value={description}
        onChangeText={setDescription}
        multiline
        numberOfLines={3}
      />

      {/* Price + Currency */}
      <Text style={styles.label}>Price</Text>
      <View style={styles.priceRow}>
        <TextInput
          style={[styles.input, styles.priceInput]}
          placeholder="0.00"
          placeholderTextColor="#9ca3af"
          value={price}
          onChangeText={setPrice}
          keyboardType="decimal-pad"
        />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.currencyScroll}>
          {CURRENCIES.map((c) => (
            <TouchableOpacity
              key={c}
              style={[styles.currencyBtn, currency === c && styles.currencyBtnActive]}
              onPress={() => setCurrency(c)}
            >
              <Text style={[styles.currencyText, currency === c && styles.currencyTextActive]}>{c}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* Size */}
      <Text style={styles.label}>Size</Text>
      <TextInput
        style={styles.input}
        placeholder="e.g. M, 42, 8.5"
        placeholderTextColor="#9ca3af"
        value={size}
        onChangeText={setSize}
        maxLength={40}
      />

      {/* Comment */}
      <Text style={styles.label}>Comment for gifter</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        placeholder="e.g. I prefer the midnight colorway"
        placeholderTextColor="#9ca3af"
        value={comment}
        onChangeText={setComment}
        multiline
        numberOfLines={2}
      />

      {/* Save */}
      <TouchableOpacity
        style={[styles.saveButton, (!name.trim() || saving) && styles.disabled]}
        onPress={handleSave}
        disabled={!name.trim() || saving}
      >
        {saving ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.saveButtonText}>Add to wishlist</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  content: { padding: 20, paddingBottom: 48 },
  label: { fontSize: 14, fontWeight: '600', color: '#374151', marginBottom: 6, marginTop: 18 },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 13,
    fontSize: 15,
    color: '#111827',
  },
  multiline: { minHeight: 72, textAlignVertical: 'top', paddingTop: 13 },
  urlRow: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  urlInput: { flex: 1 },
  parseButton: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 13,
    justifyContent: 'center',
    alignItems: 'center',
    minWidth: 56,
  },
  parseButtonText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  parsedBadge: { fontSize: 12, color: '#16a34a', marginTop: 6, fontWeight: '500' },
  priceRow: { flexDirection: 'row', gap: 10, alignItems: 'center' },
  priceInput: { width: 120 },
  currencyScroll: { flex: 1 },
  currencyBtn: {
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    backgroundColor: '#fff',
    marginRight: 6,
  },
  currencyBtnActive: { borderColor: '#6366f1', backgroundColor: '#eef2ff' },
  currencyText: { fontSize: 13, fontWeight: '500', color: '#374151' },
  currencyTextActive: { color: '#6366f1' },
  saveButton: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 28,
  },
  disabled: { opacity: 0.5 },
  saveButtonText: { fontSize: 16, fontWeight: '700', color: '#fff' },
});
