from __future__ import annotations

from fastapi import APIRouter, Depends, Query

from presentation.api.runtime import get_runtime

router = APIRouter()


@router.get("/api/parse-url")
async def parse_url(
    url: str = Query(..., description="Product URL to parse"),
    runtime=Depends(get_runtime),
):
    result = await runtime.product_service.parse_url(url)
    return result.model_dump(by_alias=True)
