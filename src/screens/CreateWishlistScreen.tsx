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
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { supabase } from '../lib/supabase';
import { useAuth } from '../hooks/useAuth';
import { AccessLevel, CoverType, RootStackParamList } from '../types';

type Nav = NativeStackNavigationProp<RootStackParamList, 'CreateWishlist'>;

const EMOJI_OPTIONS = ['🎁', '🎂', '🏖️', '🏡', '👗', '💻', '📚', '🎮', '✈️', '🍕', '❤️', '⭐'];
const ACCESS_OPTIONS: { value: AccessLevel; label: string; description: string }[] = [
  { value: 'link', label: 'Link only', description: 'Anyone with the link can view' },
  { value: 'public', label: 'Public', description: 'Discoverable by anyone' },
  { value: 'private', label: 'Private', description: 'Only you can see it' },
];

export function CreateWishlistScreen() {
  const navigation = useNavigation<Nav>();
  const { session } = useAuth();
  const [name, setName] = useState('');
  const [coverType, setCoverType] = useState<CoverType>('emoji');
  const [selectedEmoji, setSelectedEmoji] = useState('🎁');
  const [access, setAccess] = useState<AccessLevel>('link');
  const [loading, setLoading] = useState(false);

  const handleCreate = async () => {
    if (!name.trim()) {
      Alert.alert('Name required', 'Please give your wishlist a name.');
      return;
    }
    if (!session) return;

    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('wishlists')
        .insert({
          owner_id: session.user.id,
          name: name.trim(),
          cover_type: coverType,
          cover_value: coverType === 'emoji' ? selectedEmoji : null,
          access,
        })
        .select()
        .single();

      if (error) throw error;
      navigation.replace('WishlistDetail', { wishlistId: data.id });
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Name */}
      <Text style={styles.label}>Wishlist name *</Text>
      <TextInput
        style={styles.input}
        placeholder="e.g. Birthday wishlist"
        placeholderTextColor="#9ca3af"
        value={name}
        onChangeText={setName}
        maxLength={80}
        autoFocus
      />

      {/* Cover */}
      <Text style={styles.label}>Cover</Text>
      <View style={styles.coverTypeRow}>
        <TouchableOpacity
          style={[styles.coverTypeBtn, coverType === 'emoji' && styles.coverTypeBtnActive]}
          onPress={() => setCoverType('emoji')}
        >
          <Text style={styles.coverTypeBtnText}>Emoji</Text>
        </TouchableOpacity>
      </View>

      {coverType === 'emoji' && (
        <View style={styles.emojiGrid}>
          {EMOJI_OPTIONS.map((emoji) => (
            <TouchableOpacity
              key={emoji}
              style={[styles.emojiBtn, selectedEmoji === emoji && styles.emojiBtnActive]}
              onPress={() => setSelectedEmoji(emoji)}
            >
              <Text style={styles.emojiText}>{emoji}</Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Access */}
      <Text style={styles.label}>Who can see this?</Text>
      {ACCESS_OPTIONS.map((opt) => (
        <TouchableOpacity
          key={opt.value}
          style={[styles.accessOption, access === opt.value && styles.accessOptionActive]}
          onPress={() => setAccess(opt.value)}
        >
          <View style={styles.radioOuter}>
            {access === opt.value && <View style={styles.radioInner} />}
          </View>
          <View style={styles.accessText}>
            <Text style={styles.accessLabel}>{opt.label}</Text>
            <Text style={styles.accessDescription}>{opt.description}</Text>
          </View>
        </TouchableOpacity>
      ))}

      {/* Create button */}
      <TouchableOpacity
        style={[styles.createButton, (!name.trim() || loading) && styles.disabled]}
        onPress={handleCreate}
        disabled={!name.trim() || loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.createButtonText}>Create wishlist</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  content: { padding: 20, paddingBottom: 40 },
  label: { fontSize: 14, fontWeight: '600', color: '#374151', marginBottom: 8, marginTop: 20 },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: '#111827',
  },
  coverTypeRow: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  coverTypeBtn: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    backgroundColor: '#fff',
  },
  coverTypeBtnActive: { borderColor: '#6366f1', backgroundColor: '#eef2ff' },
  coverTypeBtnText: { fontSize: 14, fontWeight: '500', color: '#374151' },
  emojiGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginBottom: 4 },
  emojiBtn: {
    width: 52,
    height: 52,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  emojiBtnActive: { borderColor: '#6366f1', backgroundColor: '#eef2ff' },
  emojiText: { fontSize: 26 },
  accessOption: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    padding: 14,
    marginBottom: 8,
  },
  accessOptionActive: { borderColor: '#6366f1', backgroundColor: '#eef2ff' },
  radioOuter: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  radioInner: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#6366f1' },
  accessText: { flex: 1 },
  accessLabel: { fontSize: 15, fontWeight: '600', color: '#111827' },
  accessDescription: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  createButton: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 28,
  },
  disabled: { opacity: 0.5 },
  createButtonText: { fontSize: 16, fontWeight: '700', color: '#fff' },
});
