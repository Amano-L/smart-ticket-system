from fastapi import FastAPI
from schemas import ClassifyRequest, ClassifyResponse, ReplySuggestRequest, ReplySuggestResponse
from agents.classify import classify
from agents.reply_suggest import reply_suggest

app = FastAPI(title="Smart Ticket Agent", version="v1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/agent/classify", response_model=ClassifyResponse)
def api_classify(req: ClassifyRequest):
    return classify(req)


@app.post("/agent/reply-suggest", response_model=ReplySuggestResponse)
def api_reply_suggest(req: ReplySuggestRequest):
    return reply_suggest(req)