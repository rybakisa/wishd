import pytest


@pytest.fixture
def html_page():
    """Build an HTML page with configurable sections."""

    def _build(
        *,
        title: str | None = None,
        meta_tags: list[tuple[str, str, str]] | None = None,
        jsonld: str | None = None,
        body: str = "",
    ) -> str:
        parts = ["<html><head>"]
        if title:
            parts.append(f"<title>{title}</title>")
        for attr, name, content in meta_tags or []:
            parts.append(f'<meta {attr}="{name}" content="{content}">')
        if jsonld:
            parts.append(
                f'<script type="application/ld+json">{jsonld}</script>'
            )
        parts.append(f"</head><body>{body}</body></html>")
        return "".join(parts)

    return _build
