"""Self-hosted product URL heuristic scraper.

Strategy:
1. JSON-LD `Product`/`Offer` schema.
2. OpenGraph + product meta tags.
3. HTML <title>, <meta name="description">, price regex.
"""
import json
import re
from typing import Optional, Tuple

import httpx
from bs4 import BeautifulSoup
from pydantic import BaseModel, ConfigDict

UA = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)

CURRENCY_SYMBOL = {
    "$": "USD", "US$": "USD", "USD": "USD",
    "€": "EUR", "EUR": "EUR",
    "£": "GBP", "GBP": "GBP",
    "¥": "JPY", "JPY": "JPY",
    "₽": "RUB", "RUB": "RUB",
    "C$": "CAD", "CAD": "CAD",
    "A$": "AUD", "AUD": "AUD",
    "CHF": "CHF",
    "₹": "INR", "INR": "INR",
    "CNY": "CNY", "RMB": "CNY",
}

PRICE_REGEX = re.compile(
    r"(?P<sym>[$€£¥₽₹]|US\$|C\$|A\$|USD|EUR|GBP|JPY|RUB|CAD|AUD|CHF|INR|CNY|RMB)\s*(?P<num>[\d.,]+)"
    r"|(?P<num2>[\d.,]+)\s*(?P<sym2>USD|EUR|GBP|JPY|RUB|CAD|AUD|CHF|INR|CNY|RMB)"
)


def _camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


class ParsedProduct(BaseModel):
    model_config = ConfigDict(alias_generator=_camel, populate_by_name=True)

    name: str
    description: Optional[str] = None
    image_url: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None
    url: str


def _parse_price(text: str) -> Tuple[Optional[float], Optional[str]]:
    m = PRICE_REGEX.search(text)
    if not m:
        return None, None
    sym = (m.group("sym") or m.group("sym2") or "").strip()
    num = m.group("num") or m.group("num2") or ""
    currency = CURRENCY_SYMBOL.get(sym, None)
    try:
        cleaned = num
        if "," in cleaned and "." in cleaned:
            if cleaned.rfind(",") > cleaned.rfind("."):
                cleaned = cleaned.replace(".", "").replace(",", ".")
            else:
                cleaned = cleaned.replace(",", "")
        elif "," in cleaned:
            if re.match(r"^\d+,\d{2}$", cleaned):
                cleaned = cleaned.replace(",", ".")
            else:
                cleaned = cleaned.replace(",", "")
        price = float(cleaned)
    except ValueError:
        return None, currency
    return price, currency


def _first_dict_with_type(node, wanted: set) -> Optional[dict]:
    if isinstance(node, dict):
        t = node.get("@type")
        types = t if isinstance(t, list) else [t] if t else []
        if any(x in wanted for x in types):
            return node
        for v in node.values():
            found = _first_dict_with_type(v, wanted)
            if found:
                return found
    elif isinstance(node, list):
        for x in node:
            found = _first_dict_with_type(x, wanted)
            if found:
                return found
    return None


def _extract_jsonld(soup: BeautifulSoup) -> Optional[dict]:
    for script in soup.find_all("script", type="application/ld+json"):
        raw = script.string or script.get_text() or ""
        if not raw.strip():
            continue
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            try:
                data = json.loads(raw.replace("\n", " ").strip().rstrip(","))
            except json.JSONDecodeError:
                continue
        product = _first_dict_with_type(data, {"Product"})
        if product:
            return product
    return None


def _image_from_product(p: dict) -> Optional[str]:
    img = p.get("image")
    if isinstance(img, str):
        return img
    if isinstance(img, list) and img:
        first = img[0]
        if isinstance(first, str):
            return first
        if isinstance(first, dict):
            return first.get("url")
    if isinstance(img, dict):
        return img.get("url")
    return None


def _offer_fields(p: dict) -> Tuple[Optional[float], Optional[str]]:
    offers = p.get("offers")
    if not offers:
        return None, None
    first = offers[0] if isinstance(offers, list) and offers else offers
    if not isinstance(first, dict):
        return None, None
    price = first.get("price") or first.get("lowPrice")
    currency = first.get("priceCurrency")
    try:
        price_f = float(price) if price is not None else None
    except (TypeError, ValueError):
        price_f = None
    return price_f, currency


def _meta(soup: BeautifulSoup, *keys: str) -> Optional[str]:
    for key in keys:
        tag = soup.find("meta", property=key) or soup.find("meta", attrs={"name": key})
        if tag and tag.get("content"):
            return tag["content"].strip()
    return None


async def parse_product_url(url: str) -> ParsedProduct:
    async with httpx.AsyncClient(
        timeout=15.0,
        follow_redirects=True,
        headers={"User-Agent": UA, "Accept-Language": "en-US,en;q=0.9"},
    ) as client:
        resp = await client.get(url)
    if resp.status_code >= 400:
        raise RuntimeError(f"Fetch returned {resp.status_code}")

    soup = BeautifulSoup(resp.text, "html.parser")

    name: Optional[str] = None
    description: Optional[str] = None
    image_url: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None

    product = _extract_jsonld(soup)
    if product:
        raw_name = product.get("name")
        if isinstance(raw_name, str):
            name = raw_name.strip()
        raw_desc = product.get("description")
        if isinstance(raw_desc, str):
            description = raw_desc.strip()
        image_url = _image_from_product(product)
        price, currency = _offer_fields(product)

    name = name or _meta(soup, "og:title", "twitter:title")
    description = description or _meta(soup, "og:description", "twitter:description", "description")
    image_url = image_url or _meta(soup, "og:image", "twitter:image")
    if price is None:
        price_str = _meta(soup, "product:price:amount", "og:price:amount", "twitter:data1")
        if price_str:
            try:
                price = float(price_str.replace(",", ""))
            except ValueError:
                pass
    currency = currency or _meta(soup, "product:price:currency", "og:price:currency")

    if not name:
        title = soup.find("title")
        if title and title.string:
            name = title.string.strip()
    if price is None or currency is None:
        p2, c2 = _parse_price(soup.get_text(" ", strip=True)[:5000])
        price = price if price is not None else p2
        currency = currency or c2

    if currency:
        currency = CURRENCY_SYMBOL.get(currency, currency[:3].upper())

    return ParsedProduct(
        name=name or url,
        description=description,
        image_url=image_url,
        price=price,
        currency=currency,
        url=str(resp.url),
    )
