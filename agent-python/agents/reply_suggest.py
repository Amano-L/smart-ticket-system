from schemas import ReplySuggestRequest, ReplySuggestResponse

TEMPLATES = {
    "REFUND": "您好，关于您的退款申请，我们已收到，将尽快为您处理，预计 3 个工作日到账。",
    "TECH": "您好，关于您反馈的技术问题，我们已记录并正在排查，请稍等。",
    "COMPLAINT": "您好，非常抱歉给您带来不好的体验，我们会尽快跟进处理。",
    "CONSULT": "您好，关于您的咨询，我们会尽快为您解答。",
    "UNKNOWN": "您好，我们已收到您的工单，会尽快为您处理。",
}

# 简化：先按 UNKNOWN 模板返回（真实场景会调大模型，传入历史工单作为上下文）
def reply_suggest(req: ReplySuggestRequest) -> ReplySuggestResponse:
    suggestion = TEMPLATES["UNKNOWN"]
    return ReplySuggestResponse(suggestion=suggestion, references=[], fallback=False)