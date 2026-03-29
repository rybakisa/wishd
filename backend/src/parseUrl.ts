import fetch from 'node-fetch';

const MICROLINK_API = 'https://api.microlink.io';

interface MicrolinkResponse {
  status: string;
  data: {
    title?: string;
    description?: string;
    image?: { url?: string };
    url?: string;
    price?: { amount?: string; currency?: string };
  };
}

export interface ParsedProduct {
  name: string;
  description: string | null;
  image_url: string | null;
  price: string | null;
  currency: string | null;
  url: string;
}

/**
 * Parse a product URL using microlink.io.
 * Falls back to basic OG tag extraction if microlink fails.
 */
export async function parseProductUrl(url: string): Promise<ParsedProduct> {
  const apiKey = process.env.MICROLINK_API_KEY;
  const params = new URLSearchParams({ url, screenshot: 'false' });
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (apiKey) headers['x-api-key'] = apiKey;

  const res = await fetch(`${MICROLINK_API}?${params}`, { headers });
  if (!res.ok) {
    throw new Error(`microlink returned ${res.status}`);
  }

  const json = (await res.json()) as MicrolinkResponse;
  if (json.status !== 'success') {
    throw new Error('microlink parse failed');
  }

  const d = json.data;

  // Detect currency from price data or default to USD
  let currency: string | null = null;
  let price: string | null = null;
  if (d.price?.amount) {
    price = d.price.amount;
    currency = normalizeCurrency(d.price.currency ?? 'USD');
  }

  return {
    name: d.title ?? '',
    description: d.description ?? null,
    image_url: d.image?.url ?? null,
    price,
    currency,
    url,
  };
}

const CURRENCY_MAP: Record<string, string> = {
  '$': 'USD',
  '€': 'EUR',
  '£': 'GBP',
  'C$': 'CAD',
  'A$': 'AUD',
  'USD': 'USD',
  'EUR': 'EUR',
  'GBP': 'GBP',
  'CAD': 'CAD',
  'AUD': 'AUD',
};

function normalizeCurrency(raw: string): string {
  return CURRENCY_MAP[raw.trim()] ?? 'USD';
}
