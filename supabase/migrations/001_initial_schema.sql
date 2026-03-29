-- Wishlist App MVP — Initial Schema
-- Run in Supabase SQL editor or via supabase CLI

-- Enable UUID extension
create extension if not exists "pgcrypto";

-- Users (extends Supabase auth.users)
create table public.users (
  id uuid references auth.users(id) on delete cascade primary key,
  email text unique not null,
  display_name text,
  avatar_url text,
  created_at timestamptz default now() not null
);

-- Wishlists
create table public.wishlists (
  id uuid default gen_random_uuid() primary key,
  owner_id uuid references public.users(id) on delete cascade not null,
  name text not null,
  cover_type text check (cover_type in ('image', 'emoji')),
  cover_value text,
  access text not null default 'link' check (access in ('link', 'public', 'private')),
  share_token uuid default gen_random_uuid() not null unique,
  created_at timestamptz default now() not null
);

-- Wishlist Items
create table public.wishlist_items (
  id uuid default gen_random_uuid() primary key,
  wishlist_id uuid references public.wishlists(id) on delete cascade not null,
  name text not null,
  url text,
  image_url text,
  description text,
  price numeric(12, 2),
  currency char(3) default 'USD',
  size text,
  comment text,
  sort_order integer default 0 not null,
  created_at timestamptz default now() not null
);

-- Indexes
create index on public.wishlists(owner_id);
create index on public.wishlists(share_token);
create index on public.wishlist_items(wishlist_id);

-- Row Level Security
alter table public.users enable row level security;
alter table public.wishlists enable row level security;
alter table public.wishlist_items enable row level security;

-- Users RLS
create policy "Users can read/write own profile"
  on public.users for all
  using (id = auth.uid())
  with check (id = auth.uid());

-- Auto-create user profile on signup
create function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.users (id, email, display_name, avatar_url)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)),
    new.raw_user_meta_data->>'avatar_url'
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Wishlists RLS
create policy "Owner can CRUD own wishlists"
  on public.wishlists for all
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

create policy "Anyone can read public wishlists"
  on public.wishlists for select
  using (access = 'public');

create policy "Anyone can read link wishlists (by share_token)"
  on public.wishlists for select
  using (access = 'link');
-- Note: share_token validation is enforced at the app/API layer

-- Wishlist Items RLS
create policy "Owner can CRUD items in own wishlists"
  on public.wishlist_items for all
  using (
    wishlist_id in (
      select id from public.wishlists where owner_id = auth.uid()
    )
  )
  with check (
    wishlist_id in (
      select id from public.wishlists where owner_id = auth.uid()
    )
  );

create policy "Anyone can read items of readable wishlists"
  on public.wishlist_items for select
  using (
    wishlist_id in (
      select id from public.wishlists where access in ('public', 'link')
    )
  );
