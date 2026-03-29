import { useEffect, useState } from 'react';
import { supabase } from '../lib/supabase';
import { Wishlist } from '../types';
import { useAuth } from './useAuth';

export function useWishlists() {
  const { session } = useAuth();
  const [wishlists, setWishlists] = useState<Wishlist[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!session) {
      setWishlists([]);
      setLoading(false);
      return;
    }

    const fetchWishlists = async () => {
      setLoading(true);
      const { data, error } = await supabase
        .from('wishlists')
        .select('*, wishlist_items(count)')
        .eq('owner_id', session.user.id)
        .order('created_at', { ascending: false });

      if (!error && data) {
        const mapped = data.map((w: any) => ({
          ...w,
          item_count: w.wishlist_items?.[0]?.count ?? 0,
        }));
        setWishlists(mapped);
      }
      setLoading(false);
    };

    fetchWishlists();
  }, [session]);

  return { wishlists, loading };
}
