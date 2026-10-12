"""Local Naive Bayes helper with a stable, report-level evaluation split."""

from collections import Counter, defaultdict
from dataclasses import dataclass
import math
import re
from uuid import UUID

MINIMUM_TRAINING_SAMPLES = 12
MINIMUM_VALIDATION_SAMPLES = 15
MINIMUM_VALIDATION_PER_CLASS = 3
VALIDATION_BUCKETS = 5
CLASSIFIER_VERSION = "naive-bayes-v1"
TOKEN_PATTERN = re.compile(r"[a-záéíóúüñ0-9]+", re.IGNORECASE)


@dataclass(frozen=True)
class LabeledReport:
    text: str
    category: str
    report_id: UUID | None = None


@dataclass(frozen=True)
class EvaluatedReport:
    actual_category: str
    suggested_category: str
    initial_category: str


def is_validation_report(report_id: UUID) -> bool:
    """Assign each report permanently to either train or validation by its ID."""
    return report_id.int % VALIDATION_BUCKETS == 0


def _tokens(value: str) -> list[str]:
    return TOKEN_PATTERN.findall(value.casefold())


class LocalityCategoryClassifier:
    def __init__(self, examples: list[LabeledReport], minimum_samples: int = MINIMUM_TRAINING_SAMPLES):
        self.examples = examples
        self.class_counts = Counter(example.category for example in examples)
        self.ready = (
            len(examples) >= minimum_samples
            and len(self.class_counts) >= 2
            and min(self.class_counts.values(), default=0) >= 2
        )
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


def classification_metrics(
    reviewed: list[LabeledReport],
    evaluated: list[EvaluatedReport],
) -> dict:
    training = [
        example for example in reviewed
        if example.report_id is None or not is_validation_report(example.report_id)
    ]
    labels = sorted({example.category for example in reviewed} | {item.actual_category for item in evaluated})
    actual = Counter(item.actual_category for item in evaluated)
    robust = (
        len(evaluated) >= MINIMUM_VALIDATION_SAMPLES
        and len(labels) >= 2
        and all(actual[label] >= MINIMUM_VALIDATION_PER_CLASS for label in labels)
    )

    def scores(predicted: list[str]) -> dict[str, float | None]:
        correct = sum(item.actual_category == prediction for item, prediction in zip(evaluated, predicted))
        predicted_counts = Counter(predicted)
        true_positive = Counter(
            item.actual_category
            for item, prediction in zip(evaluated, predicted)
            if item.actual_category == prediction
        )
        class_scores = []
        per_class = {}
        for label in labels:
            precision = true_positive[label] / predicted_counts[label] if predicted_counts[label] else 0.0
            recall = true_positive[label] / actual[label] if actual[label] else 0.0
            f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
            class_scores.append((precision, recall, f1))
            per_class[label] = {
                "samples": actual[label],
                "precision": precision if robust else None,
                "recall": recall if robust else None,
                "f1": f1 if robust else None,
            }
        return {
            "accuracy": correct / len(evaluated) if robust and evaluated else None,
            "macro_precision": sum(item[0] for item in class_scores) / len(class_scores) if robust and class_scores else None,
            "macro_recall": sum(item[1] for item in class_scores) / len(class_scores) if robust and class_scores else None,
            "macro_f1": sum(item[2] for item in class_scores) / len(class_scores) if robust and class_scores else None,
            "per_class": per_class,
        }

    suggestion = scores([item.suggested_category for item in evaluated])
    baseline = scores([item.initial_category for item in evaluated])
    return {
        "reviewed_count": len(reviewed),
        "training_count": len(training),
        "validation_samples": len(evaluated),
        "minimum_training_samples": MINIMUM_TRAINING_SAMPLES,
        "minimum_validation_samples": MINIMUM_VALIDATION_SAMPLES,
        "minimum_validation_per_class": MINIMUM_VALIDATION_PER_CLASS,
        "model_ready": LocalityCategoryClassifier(training).ready,
        "ready": robust,
        "classifier_version": CLASSIFIER_VERSION,
        "accuracy": suggestion["accuracy"],
        "macro_precision": suggestion["macro_precision"],
        "macro_recall": suggestion["macro_recall"],
        "macro_f1": suggestion["macro_f1"],
        "baseline_accuracy": baseline["accuracy"],
        "baseline_macro_f1": baseline["macro_f1"],
        "per_class": suggestion["per_class"],
    }
