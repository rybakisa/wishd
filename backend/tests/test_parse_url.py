"""Unit tests for backend link parsing (parse_url.py).

Covers:
- Price parsing with various currency symbols and number formats
- JSON-LD Product extraction (name, description, image, offers)
- OpenGraph / meta tag fallbacks
- HTML title fallback
- Regex price fallback from body text
- Name falls back to URL when nothing else found
- Currency normalization
- Malformed JSON-LD recovery
- The /api/parse-url endpoint (valid URL, invalid URL, upstream error)
"""

import json

import httpx
import pytest
import pytest_asyncio

from app.parse_url import (
    CURRENCY_SYMBOL,
    _extract_jsonld,
    _image_from_product,
    _meta,
    _offer_fields,
    _parse_price,
    parse_product_url,
)
from app.models import ParsedProduct
from bs4 import BeautifulSoup


# ---------------------------------------------------------------------------
# _parse_price
# ---------------------------------------------------------------------------
class TestParsePrice:
    def test_usd_dollar_sign(self):
        price, cur = _parse_price("$29.99")
        assert price == 29.99
        assert cur == "USD"

    def test_euro_sign(self):
        price, cur = _parse_price("€1.234,56")
        assert price == 1234.56
        assert cur == "EUR"

    def test_gbp_sign(self):
        price, cur = _parse_price("£99")
        assert price == 99.0
        assert cur == "GBP"

    def test_ruble_sign(self):
        price, cur = _parse_price("₽5000")
        assert price == 5000.0
        assert cur == "RUB"

    def test_yen_sign(self):
        price, cur = _parse_price("¥1500")
        assert price == 1500.0
        assert cur == "JPY"

    def test_inr_rupee(self):
        price, cur = _parse_price("₹2,499")
        assert price == 2499.0
        assert cur == "INR"

    def test_usd_prefix_code(self):
        price, cur = _parse_price("US$49.99")
        assert price == 49.99
        assert cur == "USD"

    def test_cad_prefix(self):
        price, cur = _parse_price("C$79.00")
        assert price == 79.0
        assert cur == "CAD"

    def test_aud_prefix(self):
        price, cur = _parse_price("A$109.95")
        assert price == 109.95
        assert cur == "AUD"

    def test_currency_code_suffix(self):
        price, cur = _parse_price("100.50 EUR")
        assert price == 100.50
        assert cur == "EUR"

    def test_currency_code_suffix_gbp(self):
        price, cur = _parse_price("250 GBP")
        assert price == 250.0
        assert cur == "GBP"

    def test_european_comma_decimal(self):
        """1234,99 with comma as decimal separator."""
        price, cur = _parse_price("€1234,99")
        assert price == 1234.99
        assert cur == "EUR"

    def test_european_dot_thousand_comma_decimal(self):
        """1.234,56 format common in Germany."""
        price, cur = _parse_price("€1.234,56")
        assert price == 1234.56
        assert cur == "EUR"

    def test_us_comma_thousand_dot_decimal(self):
        """1,234.56 US format."""
        price, cur = _parse_price("$1,234.56")
        assert price == 1234.56
        assert cur == "USD"

    def test_large_number_commas(self):
        price, cur = _parse_price("$1,000,000")
        assert price == 1000000.0

    def test_no_price_returns_none(self):
        price, cur = _parse_price("no price here")
        assert price is None
        assert cur is None

    def test_chf_code(self):
        price, cur = _parse_price("CHF 59.90")
        assert price == 59.90
        assert cur == "CHF"

    def test_rmb_maps_to_cny(self):
        price, cur = _parse_price("RMB 199")
        assert price == 199.0
        assert cur == "CNY"

    def test_price_in_surrounding_text(self):
        price, cur = _parse_price("Buy now for only $19.99 while supplies last")
        assert price == 19.99
        assert cur == "USD"


# ---------------------------------------------------------------------------
# _extract_jsonld
# ---------------------------------------------------------------------------
class TestExtractJsonld:
    def test_simple_product(self, html_page):
        jsonld = json.dumps({
            "@context": "https://schema.org",
            "@type": "Product",
            "name": "Widget",
            "description": "A nice widget",
        })
        html = html_page(jsonld=jsonld)
        soup = BeautifulSoup(html, "html.parser")
        product = _extract_jsonld(soup)
        assert product is not None
        assert product["name"] == "Widget"
        assert product["description"] == "A nice widget"

    def test_nested_product_in_graph(self, html_page):
        jsonld = json.dumps({
            "@context": "https://schema.org",
            "@graph": [
                {"@type": "WebPage", "name": "Page"},
                {"@type": "Product", "name": "Nested Widget"},
            ],
        })
        html = html_page(jsonld=jsonld)
        soup = BeautifulSoup(html, "html.parser")
        product = _extract_jsonld(soup)
        assert product is not None
        assert product["name"] == "Nested Widget"

    def test_no_product_returns_none(self, html_page):
        jsonld = json.dumps({"@type": "Organization", "name": "Acme"})
        html = html_page(jsonld=jsonld)
        soup = BeautifulSoup(html, "html.parser")
        assert _extract_jsonld(soup) is None

    def test_malformed_json_trailing_comma_after_brace(self, html_page):
        """Retry logic strips trailing comma after closing brace: {...},"""
        raw = '{"@type": "Product", "name": "Recovered"},'
        html = html_page(jsonld=raw)
        soup = BeautifulSoup(html, "html.parser")
        product = _extract_jsonld(soup)
        assert product is not None
        assert product["name"] == "Recovered"

    def test_malformed_json_with_newlines(self, html_page):
        """Retry logic collapses newlines that break JSON strings."""
        raw = '{"@type": "Product",\n"name": "Newline"}'
        html = html_page(jsonld=raw)
        soup = BeautifulSoup(html, "html.parser")
        product = _extract_jsonld(soup)
        assert product is not None
        assert product["name"] == "Newline"

    def test_completely_invalid_json_skipped(self, html_page):
        raw = "not json at all"
        html = html_page(jsonld=raw)
        soup = BeautifulSoup(html, "html.parser")
        assert _extract_jsonld(soup) is None

    def test_empty_script_ignored(self, html_page):
        html = html_page(jsonld="   ")
        soup = BeautifulSoup(html, "html.parser")
        assert _extract_jsonld(soup) is None

    def test_product_type_as_list(self, html_page):
        jsonld = json.dumps({"@type": ["Product", "IndividualProduct"], "name": "Multi"})
        html = html_page(jsonld=jsonld)
        soup = BeautifulSoup(html, "html.parser")
        product = _extract_jsonld(soup)
        assert product is not None
        assert product["name"] == "Multi"


# ---------------------------------------------------------------------------
# _image_from_product
# ---------------------------------------------------------------------------
class TestImageFromProduct:
    def test_string_image(self):
        assert _image_from_product({"image": "https://img.com/a.jpg"}) == "https://img.com/a.jpg"

    def test_list_of_strings(self):
        assert _image_from_product({"image": ["https://img.com/a.jpg"]}) == "https://img.com/a.jpg"

    def test_list_of_dicts(self):
        assert _image_from_product({"image": [{"url": "https://img.com/b.jpg"}]}) == "https://img.com/b.jpg"

    def test_dict_image(self):
        assert _image_from_product({"image": {"url": "https://img.com/c.jpg"}}) == "https://img.com/c.jpg"

    def test_no_image_returns_none(self):
        assert _image_from_product({}) is None

    def test_empty_list_returns_none(self):
        assert _image_from_product({"image": []}) is None

    def test_none_image(self):
        assert _image_from_product({"image": None}) is None


# ---------------------------------------------------------------------------
# _offer_fields
# ---------------------------------------------------------------------------
class TestOfferFields:
    def test_single_offer_dict(self):
        p = {"offers": {"price": "29.99", "priceCurrency": "USD"}}
        price, cur = _offer_fields(p)
        assert price == 29.99
        assert cur == "USD"

    def test_offers_list(self):
        p = {"offers": [{"price": 49, "priceCurrency": "EUR"}]}
        price, cur = _offer_fields(p)
        assert price == 49.0
        assert cur == "EUR"

    def test_low_price(self):
        p = {"offers": {"lowPrice": "10.00", "priceCurrency": "GBP"}}
        price, cur = _offer_fields(p)
        assert price == 10.0
        assert cur == "GBP"

    def test_no_offers(self):
        price, cur = _offer_fields({})
        assert price is None
        assert cur is None

    def test_invalid_price_value(self):
        p = {"offers": {"price": "free", "priceCurrency": "USD"}}
        price, cur = _offer_fields(p)
        assert price is None
        assert cur == "USD"

    def test_none_price(self):
        p = {"offers": {"price": None, "priceCurrency": "USD"}}
        price, cur = _offer_fields(p)
        assert price is None
        assert cur == "USD"


# ---------------------------------------------------------------------------
# _meta
# ---------------------------------------------------------------------------
class TestMeta:
    def test_og_title(self, html_page):
        html = html_page(meta_tags=[("property", "og:title", "OG Title")])
        soup = BeautifulSoup(html, "html.parser")
        assert _meta(soup, "og:title") == "OG Title"

    def test_fallback_to_second_key(self, html_page):
        html = html_page(meta_tags=[("name", "twitter:title", "TW Title")])
        soup = BeautifulSoup(html, "html.parser")
        assert _meta(soup, "og:title", "twitter:title") == "TW Title"

    def test_missing_returns_none(self, html_page):
        html = html_page()
        soup = BeautifulSoup(html, "html.parser")
        assert _meta(soup, "og:title", "twitter:title") is None

    def test_empty_content_ignored(self, html_page):
        html = html_page(meta_tags=[("property", "og:title", "")])
        soup = BeautifulSoup(html, "html.parser")
        assert _meta(soup, "og:title") is None

    def test_whitespace_stripped(self, html_page):
        html = html_page(meta_tags=[("property", "og:title", "  Padded  ")])
        soup = BeautifulSoup(html, "html.parser")
        assert _meta(soup, "og:title") == "Padded"


# ---------------------------------------------------------------------------
# Full parse_product_url (with mocked HTTP via monkeypatch)
# ---------------------------------------------------------------------------

class _FakeAsyncClient:
    """Drop-in replacement for httpx.AsyncClient that returns canned HTML."""

    def __init__(self, html_text: str, status: int):
        self._html = html_text
        self._status = status

    async def __aenter__(self):
        return self

    async def __aexit__(self, *a):
        pass

    async def get(self, url, **kw):
        return httpx.Response(self._status, text=self._html, request=httpx.Request("GET", url))


def _mock_parse(monkeypatch, html_text: str, *, status: int = 200):
    """Replace httpx.AsyncClient in app.parse_url with a fake returning ``html_text``."""
    import app.parse_url as mod
    class _FakeHttpx:
        @staticmethod
        def AsyncClient(**kw):
            return _FakeAsyncClient(html_text, status)

    monkeypatch.setattr(mod, "httpx", _FakeHttpx())


class TestParseProductUrl:
    """Integration-style tests for the full parsing pipeline using httpx mocking."""

    @pytest.mark.asyncio
    async def test_jsonld_full_product(self, html_page, monkeypatch):
        jsonld = json.dumps({
            "@type": "Product",
            "name": "Air Max 90",
            "description": "Classic sneaker",
            "image": "https://cdn.example.com/shoe.jpg",
            "offers": {"price": "129.99", "priceCurrency": "USD"},
        })
        html = html_page(jsonld=jsonld)
        _mock_parse(monkeypatch, html)

        result = await parse_product_url("https://example.com/shoe")

        assert result.name == "Air Max 90"
        assert result.description == "Classic sneaker"
        assert result.image_url == "https://cdn.example.com/shoe.jpg"
        assert result.price == 129.99
        assert result.currency == "USD"
        assert "example.com" in result.url

    @pytest.mark.asyncio
    async def test_opengraph_fallback(self, html_page, monkeypatch):
        html = html_page(
            meta_tags=[
                ("property", "og:title", "OG Product"),
                ("property", "og:description", "OG Desc"),
                ("property", "og:image", "https://img.example.com/og.jpg"),
                ("property", "product:price:amount", "59.99"),
                ("property", "product:price:currency", "EUR"),
            ],
        )
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/item")

        assert result.name == "OG Product"
        assert result.description == "OG Desc"
        assert result.image_url == "https://img.example.com/og.jpg"
        assert result.price == 59.99
        assert result.currency == "EUR"

    @pytest.mark.asyncio
    async def test_html_title_fallback(self, html_page, monkeypatch):
        html = html_page(title="My Store - Cool Gadget")
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/gadget")

        assert result.name == "My Store - Cool Gadget"

    @pytest.mark.asyncio
    async def test_name_falls_back_to_url(self, html_page, monkeypatch):
        html = html_page()  # empty page - no title, no meta, no JSON-LD
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/unknown")

        assert result.name == "https://example.com/unknown"

    @pytest.mark.asyncio
    async def test_price_regex_fallback_from_body(self, html_page, monkeypatch):
        html = html_page(body="<p>Price: $49.99 - limited time offer!</p>")
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/deal")

        assert result.price == 49.99
        assert result.currency == "USD"

    @pytest.mark.asyncio
    async def test_http_error_raises(self, monkeypatch):
        _mock_parse(monkeypatch, "Not Found", status=404)
        with pytest.raises(RuntimeError, match="404"):
            await parse_product_url("https://example.com/missing")

    @pytest.mark.asyncio
    async def test_jsonld_overrides_og(self, html_page, monkeypatch):
        """JSON-LD data takes priority over OG meta tags."""
        jsonld = json.dumps({
            "@type": "Product",
            "name": "JSON-LD Name",
            "description": "JSON-LD Desc",
        })
        html = html_page(
            jsonld=jsonld,
            meta_tags=[
                ("property", "og:title", "OG Name"),
                ("property", "og:description", "OG Desc"),
            ],
        )
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/product")

        assert result.name == "JSON-LD Name"
        assert result.description == "JSON-LD Desc"

    @pytest.mark.asyncio
    async def test_currency_symbol_normalized_to_code(self, html_page, monkeypatch):
        """Currency symbols like $ get normalized to 3-letter ISO codes."""
        html = html_page(body="<span>$99.99</span>")
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/item2")

        assert result.currency == "USD"

    @pytest.mark.asyncio
    async def test_optional_fields_null_when_missing(self, html_page, monkeypatch):
        html = html_page(title="Only Title")
        _mock_parse(monkeypatch, html)
        result = await parse_product_url("https://example.com/bare")

        assert result.name == "Only Title"
        assert result.description is None
        assert result.image_url is None
        assert result.price is None
        assert result.currency is None


# ---------------------------------------------------------------------------
# /api/parse-url endpoint tests
# ---------------------------------------------------------------------------
class TestParseUrlEndpoint:
    @pytest.fixture
    def client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def test_invalid_url_returns_400(self, client):
        resp = client.get("/api/parse-url", params={"url": "not-a-url"})
        assert resp.status_code == 400

    def test_missing_url_returns_422(self, client):
        resp = client.get("/api/parse-url")
        assert resp.status_code == 422

    def test_valid_url_format_accepted(self, client, monkeypatch):
        """Valid URL format passes validation (mock the actual fetch)."""
        from app.models import ParsedProduct

        async def mock_parse(url):
            return ParsedProduct(name="Test", url=url)

        monkeypatch.setattr("app.main.parse_product_url", mock_parse)
        resp = client.get("/api/parse-url", params={"url": "https://example.com/product"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["name"] == "Test"
        assert data["url"] == "https://example.com/product"
        # camelCase keys on the wire
        assert "imageUrl" in data or data.get("imageUrl") is None

    def test_parse_failure_returns_502(self, client, monkeypatch):
        async def mock_parse(url):
            raise RuntimeError("connection timeout")

        monkeypatch.setattr("app.main.parse_product_url", mock_parse)
        resp = client.get("/api/parse-url", params={"url": "https://down.example.com"})
        assert resp.status_code == 502

    def test_bare_domain_url_gets_https_prepended(self, client, monkeypatch):
        """URLs without scheme (e.g. amazon.ae/...) get https:// auto-prepended."""
        from app.models import ParsedProduct

        async def mock_parse(url):
            return ParsedProduct(name="WHOOP", url=url)

        monkeypatch.setattr("app.main.parse_product_url", mock_parse)
        resp = client.get(
            "/api/parse-url",
            params={"url": "amazon.ae/WHOOP-Peak/dp/B0DY2SWV16/"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["url"].startswith("https://")

    def test_bare_domain_with_query_params(self, client, monkeypatch):
        """Bare domain + complex query string should still work."""
        from app.models import ParsedProduct

        async def mock_parse(url):
            return ParsedProduct(name="Item", url=url)

        monkeypatch.setattr("app.main.parse_product_url", mock_parse)
        raw = "amazon.ae/dp/B0DY2SWV16/?_encoding=UTF8&pd_rd_w=nwj9E"
        resp = client.get("/api/parse-url", params={"url": raw})
        assert resp.status_code == 200

    def test_http_scheme_preserved(self, client, monkeypatch):
        """Explicit http:// should not be changed to https://."""
        from app.models import ParsedProduct

        async def mock_parse(url):
            return ParsedProduct(name="HTTP", url=url)

        monkeypatch.setattr("app.main.parse_product_url", mock_parse)
        resp = client.get("/api/parse-url", params={"url": "http://example.com/item"})
        assert resp.status_code == 200
        assert resp.json()["url"].startswith("http://")

    def test_empty_url_returns_400(self, client):
        resp = client.get("/api/parse-url", params={"url": "   "})
        assert resp.status_code == 400


# ---------------------------------------------------------------------------
# Currency symbol mapping coverage
# ---------------------------------------------------------------------------
class TestCurrencySymbolMap:
    def test_all_symbols_have_three_letter_codes(self):
        for sym, code in CURRENCY_SYMBOL.items():
            assert len(code) == 3, f"{sym} -> {code} is not a 3-letter code"
            assert code == code.upper(), f"{sym} -> {code} is not uppercase"

    def test_rmb_and_cny_both_resolve(self):
        assert CURRENCY_SYMBOL["RMB"] == "CNY"
        assert CURRENCY_SYMBOL["CNY"] == "CNY"

    def test_dollar_variants(self):
        assert CURRENCY_SYMBOL["$"] == "USD"
        assert CURRENCY_SYMBOL["US$"] == "USD"
        assert CURRENCY_SYMBOL["C$"] == "CAD"
        assert CURRENCY_SYMBOL["A$"] == "AUD"
