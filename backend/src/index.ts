import express from 'express';
import cors from 'cors';
import { parseProductUrl } from './parseUrl';

const app = express();
const PORT = process.env.PORT ?? 4000;

app.use(cors({ origin: '*' }));
app.use(express.json());

/**
 * GET /api/parse-url?url=<product-url>
 * Proxies to microlink.io and returns normalized product data.
 */
app.get('/api/parse-url', async (req, res) => {
  const url = req.query.url as string;

  if (!url) {
    res.status(400).json({ error: 'url query parameter is required' });
    return;
  }

  try {
    new URL(url); // validate it's a real URL
  } catch {
    res.status(400).json({ error: 'Invalid URL' });
    return;
  }

  try {
    const product = await parseProductUrl(url);
    res.json(product);
  } catch (err: any) {
    res.status(502).json({ error: 'Could not parse product URL', details: err.message });
  }
});

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.listen(PORT, () => {
  console.log(`Wishlist API running on port ${PORT}`);
});
