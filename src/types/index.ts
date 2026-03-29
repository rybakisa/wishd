export type AccessLevel = 'link' | 'public' | 'private';

export type CoverType = 'image' | 'emoji';

export interface Wishlist {
  id: string;
  owner_id: string;
  name: string;
  cover_type: CoverType | null;
  cover_value: string | null;
  access: AccessLevel;
  share_token: string;
  created_at: string;
  item_count?: number;
}

export interface WishlistItem {
  id: string;
  wishlist_id: string;
  name: string;
  url: string | null;
  image_url: string | null;
  description: string | null;
  price: number | null;
  currency: string | null;
  size: string | null;
  comment: string | null;
  sort_order: number;
  created_at: string;
}

export type RootStackParamList = {
  Home: undefined;
  WishlistDetail: { wishlistId: string };
  CreateWishlist: undefined;
  AddItem: { wishlistId: string };
  Auth: { redirectAfter?: keyof RootStackParamList };
  ShareView: { shareToken: string };
};
