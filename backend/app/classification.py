"""Small, local Naive Bayes assistant trained only on administrator-reviewed labels."""

from collections import Counter, defaultdict
from dataclasses import dataclass
import math
import re

MINIMUM_SAMPLES = 12
TOKEN_PATTERN = re.compile(r"[a-záéíóúüñ0-9]+", re.IGNORECASE)


@dataclass(frozen=True)
class LabeledReport:
    text: str
    category: str


def _tokens(value: str) -> list[str]:
    return TOKEN_PATTERN.findall(value.casefold())


class LocalityCategoryClassifier:
    def __init__(self, examples: list[LabeledReport], minimum_samples: int = MINIMUM_SAMPLES):
        self.examples = examples
        self.class_counts = Counter(example.category for example in examples)
        self.ready = len(examples) >= minimum_samples and len(self.class_counts) >= 2 and min(self.class_counts.values(), default=0) >= 2
        self.vocabulary: set[str] = set()
        self.word_counts: dict[str, Counter[str]] = defaultdict(Counter)
        self.word_totals: Counter[str] = Counter()
        if self.ready:
            self._fit(examples)

    def _fit(self, examples: list[LabeledReport]) -> None:
        for example in examples:
            words = _tokens(example.text)
            self.vocabulary.update(words)
            self.word_counts[example.category].update(words)
            self.word_totals[example.category] += len(words)

    def predict(self, value: str) -> tuple[str, float] | None:
        if not self.ready or not self.vocabulary:
            return None
        words = _tokens(value)
        if not words:
            return None
        total = len(self.examples)
        scores: dict[str, float] = {}
        for category, count in self.class_counts.items():
            score = math.log(count / total)
            denominator = self.word_totals[category] + len(self.vocabulary)
            for word in words:
                score += math.log((self.word_counts[category][word] + 1) / denominator)
            scores[category] = score
        peak = max(scores.values())
        probabilities = {key: math.exp(value - peak) for key, value in scores.items()}
        normalizer = sum(probabilities.values())
        label = max(scores, key=scores.get)
        return label, probabilities[label] / normalizer


def classification_metrics(examples: list[LabeledReport]) -> dict[str, int | bool | float | None]:
    labels = sorted({example.category for example in examples})
    correct = 0
    predicted_counts = Counter()
    actual_counts = Counter()
    true_positive = Counter()
    validation_samples = 0
    if len(examples) >= MINIMUM_SAMPLES and len(labels) >= 2:
        for index, example in enumerate(examples):
            training = examples[:index] + examples[index + 1:]
            classifier = LocalityCategoryClassifier(training, minimum_samples=max(2, MINIMUM_SAMPLES - 1))
            prediction = classifier.predict(example.text)
            if prediction is None:
                continue
            predicted = prediction[0]
            validation_samples += 1
            actual_counts[example.category] += 1
            predicted_counts[predicted] += 1
            if predicted == example.category:
                correct += 1
                true_positive[predicted] += 1
    class_scores = []
    for label in labels:
        precision = true_positive[label] / predicted_counts[label] if predicted_counts[label] else 0.0
        recall = true_positive[label] / actual_counts[label] if actual_counts[label] else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        class_scores.append((precision, recall, f1))
    ready = len(examples) >= MINIMUM_SAMPLES and len(labels) >= 2 and min(Counter(item.category for item in examples).values(), default=0) >= 2
    return {
        "reviewed_count": len(examples),
        "validation_samples": validation_samples,
        "ready": ready,
        "minimum_samples": MINIMUM_SAMPLES,
        "accuracy": correct / validation_samples if validation_samples else None,
        "macro_precision": sum(item[0] for item in class_scores) / len(class_scores) if validation_samples and class_scores else None,
        "macro_recall": sum(item[1] for item in class_scores) / len(class_scores) if validation_samples and class_scores else None,
        "macro_f1": sum(item[2] for item in class_scores) / len(class_scores) if validation_samples and class_scores else None,
    }
