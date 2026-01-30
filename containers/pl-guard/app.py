# pl-guard/app.py
import os
from typing import List, Dict, Any
import numpy as np
import torch
from fastapi import FastAPI
from pydantic import BaseModel, Field
from transformers import AutoTokenizer, AutoModelForSequenceClassification

MODEL_ID = os.getenv("HF_MODEL_ID", "NASK-PIB/HerBERT-PL-Guard")
HF_TOKEN = os.getenv("HF_TOKEN")
DEVICE = "cpu"  # keep CPU-only by default for portability

tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_ID, token=HF_TOKEN)
model.to(DEVICE)
model.eval()

# Try to map id->label robustly
id2label = getattr(model.config, "id2label", None) or {}
# Normalize keys to int if they came as strings
id2label = {int(k): v for k, v in id2label.items()} if id2label else {}

app = FastAPI(title="PL-Guard moderation service", version="1.0.0")


class ModerateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=4000)


class BatchModerateRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=64)


def _predict(texts: List[str]) -> List[Dict[str, Any]]:
    with torch.no_grad():
        enc = tokenizer(
            texts,
            padding=True,
            truncation=True,
            max_length=256,
            return_tensors="pt",
        )
        enc = {k: v.to(DEVICE) for k, v in enc.items()}
        logits = model(**enc).logits.detach().cpu().numpy()

    probs = torch.softmax(torch.from_numpy(logits), dim=-1).numpy()

    results = []
    for i, text in enumerate(texts):
        p = probs[i]
        top_idx = int(np.argmax(p))
        labels = []
        for idx, score in enumerate(p):
            label = id2label.get(idx, str(idx))
            labels.append({"label": label, "score": float(score)})

        labels.sort(key=lambda x: x["score"], reverse=True)

        results.append(
            {
                "text": text,
                "top": labels[0],
                "labels": labels,
            }
        )
    return results


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_ID, "device": DEVICE}


@app.post("/moderate")
def moderate(req: ModerateRequest):
    return _predict([req.text])[0]


@app.post("/moderate/batch")
def moderate_batch(req: BatchModerateRequest):
    return {"results": _predict(req.texts)}
