import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  Share,
} from 'react-native';
import { useNavigation, useRoute, useFocusEffect } from '@react-navigation/native';
import type { NativeStackNavigationProp, NativeStackScreenProps } from '@react-navigation/native-stack';
import { supabase } from '../lib/supabase';
import { useAuth } from '../hooks/useAuth';
import { RootStackParamList, Wishlist, WishlistItem } from '../types';

type Props = NativeStackScreenProps<RootStackParamList, 'WishlistDetail'>;
type Nav = NativeStackNavigationProp<RootStackParamList, 'WishlistDetail'>;

export function WishlistDetailScreen({ route }: Props) {
  const { wishlistId } = route.params;
  const navigation = useNavigation<Nav>();
  const { session } = useAuth();
  const [wishlist, setWishlist] = useState<Wishlist | null>(null);
  const [items, setItems] = useState<WishlistItem[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    const [wishlistRes, itemsRes] = await Promise.all([
      supabase.from('wishlists').select('*').eq('id', wishlistId).single(),
      supabase
        .from('wishlist_items')
        .select('*')
        .eq('wishlist_id', wishlistId)
        .order('sort_order', { ascending: true }),
    ]);
    if (wishlistRes.data) setWishlist(wishlistRes.data);
    if (itemsRes.data) setItems(itemsRes.data);
    setLoading(false);
  }, [wishlistId]);

  useFocusEffect(fetchData);

  useEffect(() => {
    if (wishlist) {
      navigation.setOptions({ title: wishlist.name });
    }
  }, [wishlist, navigation]);

  const handleShare = async () => {
    if (!session) {
      navigation.navigate('Auth', { redirectAfter: 'WishlistDetail' });
      return;
    }
    if (!wishlist) return;
    const url = `https://wishlistapp.example/wl/${wishlist.share_token}`;
    try {
      await Share.share({ message: `Check out my wishlist: ${url}`, url });
    } catch (err: any) {
      Alert.alert('Share error', err.message);
    }
  };

  const handleDeleteItem = async (itemId: string) => {
    Alert.alert('Remove item', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove',
        style: 'destructive',
        onPress: async () => {
          await supabase.from('wishlist_items').delete().eq('id', itemId);
          setItems((prev) => prev.filter((i) => i.id !== itemId));
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={styles.loader}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  if (!wishlist) return null;

  const cover = wishlist.cover_type === 'emoji' ? wishlist.cover_value : '🎁';

  return (
    <View style={styles.container}>
      {/* Header info */}
      <View style={styles.header}>
        <Text style={styles.headerEmoji}>{cover}</Text>
        <View style={styles.headerInfo}>
          <Text style={styles.headerName}>{wishlist.name}</Text>
          <Text style={styles.headerMeta}>
            {items.length} items · {wishlist.access}
          </Text>
        </View>
        <TouchableOpacity style={styles.shareButton} onPress={handleShare}>
          <Text style={styles.shareButtonText}>Share</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        contentContainerStyle={items.length === 0 ? styles.emptyContainer : styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>No items yet</Text>
            <Text style={styles.emptySubtitle}>Tap + to add your first gift idea</Text>
          </View>
        }
        renderItem={({ item }) => (
          <ItemCard item={item} onDelete={() => handleDeleteItem(item.id)} />
        )}
      />

      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('AddItem', { wishlistId })}
      >
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

function ItemCard({ item, onDelete }: { item: WishlistItem; onDelete: () => void }) {
  return (
    <TouchableOpacity
      style={styles.card}
      onLongPress={onDelete}
      activeOpacity={0.8}
    >
      <View style={styles.cardImagePlaceholder}>
        <Text style={styles.cardImageText}>{item.name.charAt(0).toUpperCase()}</Text>
      </View>
      <View style={styles.cardInfo}>
        <Text style={styles.cardName} numberOfLines={2}>{item.name}</Text>
        {item.price != null && (
          <Text style={styles.cardPrice}>
            {item.currency ?? 'USD'} {item.price.toFixed(2)}
          </Text>
        )}
        {item.url && <Text style={styles.cardLink} numberOfLines={1}>{item.url}</Text>}
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  loader: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  headerEmoji: { fontSize: 40, marginRight: 12 },
  headerInfo: { flex: 1 },
  headerName: { fontSize: 18, fontWeight: '700', color: '#111827' },
  headerMeta: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  shareButton: {
    backgroundColor: '#6366f1',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
  },
  shareButtonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
  list: { padding: 16, gap: 12 },
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 80 },
  empty: { alignItems: 'center' },
  emptyTitle: { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 8 },
  emptySubtitle: { fontSize: 14, color: '#9ca3af' },
  card: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  cardImagePlaceholder: {
    width: 80,
    height: 80,
    backgroundColor: '#eef2ff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cardImageText: { fontSize: 28, fontWeight: '700', color: '#6366f1' },
  cardInfo: { flex: 1, padding: 12, justifyContent: 'center' },
  cardName: { fontSize: 15, fontWeight: '600', color: '#111827' },
  cardPrice: { fontSize: 14, color: '#6366f1', fontWeight: '600', marginTop: 4 },
  cardLink: { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  fab: {
    position: 'absolute',
    bottom: 32,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#6366f1',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 6,
  },
  fabIcon: { fontSize: 28, color: '#fff', lineHeight: 32 },
});
