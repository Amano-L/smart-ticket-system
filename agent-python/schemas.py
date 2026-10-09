from pydantic import BaseModel
from typing import List


class ClassifyRequest(BaseModel):
    ticketId: int
    title: str
    content: str


class ClassifyResponse(BaseModel):
    type: str
    confidence: float
    fallback: bool


class ReplySuggestRequest(BaseModel):
    ticketId: int
    content: str
    history: List[dict] = []


class ReplySuggestResponse(BaseModel):
    suggestion: str
    references: List[dict] = []
    fallback: bool