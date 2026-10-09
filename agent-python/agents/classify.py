from schemas import ClassifyRequest, ClassifyResponse

KEYWORDS = {
    "REFUND": ["退款", "退货", "钱", "到账"],
    "TECH": ["打不开", "错误", "bug", "崩溃", "报错"],
    "COMPLAINT": ["投诉", "态度", "差评", "不满"],
    "CONSULT": ["怎么", "如何", "请问", "咨询"],
}


def classify(req: ClassifyRequest) -> ClassifyResponse:
    text = (req.title + " " + req.content).lower()
    for t, words in KEYWORDS.items():
        for w in words:
            if w in text:
                return ClassifyResponse(type=t, confidence=0.85, fallback=False)
    return ClassifyResponse(type="UNKNOWN", confidence=0.5, fallback=False)