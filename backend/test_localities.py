import unittest
from datetime import datetime, timezone
from uuid import uuid4
from fastapi import HTTPException

from app.models import Report, User
from app.routes import BOGOTA_LOCALITIES, canonical_bogota_locality, decide_report, locality_level, review_report_category, validate_map_filters
from app.schemas import CategoryReview, ReportDecision
from app.security import administrator


class BogotaLocalityTests(unittest.TestCase):
    def test_all_twenty_localities_are_available(self):
        self.assertEqual(len(BOGOTA_LOCALITIES), 20)
        self.assertEqual(len(set(BOGOTA_LOCALITIES)), 20)

    def test_matching_is_case_insensitive_and_returns_canonical_name(self):
        self.assertEqual(canonical_bogota_locality("  usaquén "), "Usaquén")
        self.assertEqual(canonical_bogota_locality("CIUDAD BOLÍVAR"), "Ciudad Bolívar")

    def test_unknown_locality_is_rejected(self):
        self.assertIsNone(canonical_bogota_locality("Bogotá Centro"))


class MapFilterTests(unittest.TestCase):
    def test_known_filters_are_accepted(self):
        validate_map_filters("Automóvil", "received")
        validate_map_filters(None, None)

    def test_unknown_category_is_rejected(self):
        with self.assertRaises(HTTPException):
            validate_map_filters("Camión", None)

    def test_unknown_status_is_rejected(self):
        with self.assertRaises(HTTPException):
            validate_map_filters(None, "pending")

    def test_moderation_statuses_are_valid_map_filters(self):
        validate_map_filters(None, "approved")
        validate_map_filters(None, "rejected")


class LocalityAnalyticsTests(unittest.TestCase):
    def test_priority_thresholds_match_home_summary(self):
        self.assertEqual(locality_level(10), "Alta")
        self.assertEqual(locality_level(5), "Media")
        self.assertEqual(locality_level(4), "Baja")
        self.assertEqual(locality_level(1), "Baja")


class AssistedClassificationTests(unittest.TestCase):
    def test_classifier_waits_for_human_reviewed_examples(self):
        from app.classification import LabeledReport, LocalityCategoryClassifier

        model = LocalityCategoryClassifier([
            LabeledReport("carro bloquea anden", "Automóvil"),
            LabeledReport("moto ocupa acera", "Motocicleta"),
        ])
        self.assertFalse(model.ready)
        self.assertIsNone(model.predict("vehiculo sobre acera"))

    def test_metrics_report_no_quality_score_without_labels(self):
        from app.classification import classification_metrics

        metrics = classification_metrics([])
        self.assertFalse(metrics["ready"])
        self.assertIsNone(metrics["accuracy"])

    def test_classifier_uses_only_confirmed_labels_and_reports_holdout_metrics(self):
        from app.classification import LabeledReport, LocalityCategoryClassifier, classification_metrics

        examples = [LabeledReport(f"carro sedan automovil parqueado {i}", "Automóvil") for i in range(6)]
        examples += [LabeledReport(f"moto motocicleta ciclomotor acera {i}", "Motocicleta") for i in range(6)]
        model = LocalityCategoryClassifier(examples)
        self.assertTrue(model.ready)
        self.assertEqual(model.predict("carro sedan estacionado")[0], "Automóvil")
        metrics = classification_metrics(examples)
        self.assertEqual(metrics["validation_samples"], 12)
        self.assertEqual(metrics["accuracy"], 1.0)

    def test_admin_category_review_persists_human_label(self):
        owner = User(id=uuid4(), name="Reportante", email="owner@example.com", password_hash="hashed", role="citizen")
        admin = User(id=uuid4(), name="Admin", email="admin@example.com", password_hash="hashed", role="admin")
        report = Report(
            id=uuid4(), user_id=owner.id, user=owner, category="Otro", description="camioneta obstruye paso",
            photo_key="report.jpg", locality="Suba", address="Calle 1 # 2-3", location=None,
            status="received", created_at=datetime.now(timezone.utc),
        )

        class Rows:
            def all(self):
                return []

        class FakeSession:
            def scalar(self, _query):
                return report

            def execute(self, _query):
                return Rows()

            def commit(self):
                pass

            def refresh(self, _report):
                pass

        updated = review_report_category(str(report.id), CategoryReview(category="Automóvil"), admin, FakeSession())
        self.assertEqual(report.reviewed_category, "Automóvil")
        self.assertEqual(updated.reviewed_category, "Automóvil")

class AdministratorRoleTests(unittest.TestCase):
    def test_citizen_cannot_access_admin_operations(self):
        citizen = User(name="Ciudadano", email="citizen@example.com", password_hash="hashed", role="citizen")
        with self.assertRaises(HTTPException) as raised:
            administrator(citizen)
        self.assertEqual(raised.exception.status_code, 403)

    def test_admin_role_is_accepted(self):
        admin = User(name="Admin", email="admin@example.com", password_hash="hashed", role="admin")
        self.assertIs(administrator(admin), admin)

    def test_only_approved_or_rejected_are_moderation_decisions(self):
        self.assertEqual(ReportDecision(status="approved").status, "approved")
        self.assertEqual(ReportDecision(status="rejected").status, "rejected")
        with self.assertRaises(ValueError):
            ReportDecision(status="received")

    def test_admin_can_decide_report_and_record_moderator(self):
        owner = User(id=uuid4(), name="Reportante", email="owner@example.com", password_hash="hashed", role="citizen")
        admin = User(id=uuid4(), name="Admin", email="admin@example.com", password_hash="hashed", role="admin")
        report = Report(
            id=uuid4(), user_id=owner.id, user=owner, category="Automóvil", description="",
            photo_key="report.jpg", locality="Suba", address="Calle 1 # 2-3", location=None,
            status="received", created_at=datetime.now(timezone.utc),
        )

        class FakeSession:
            def scalar(self, _query):
                return report

            def commit(self):
                pass

            def refresh(self, _report):
                pass

        result = decide_report(str(report.id), ReportDecision(status="rejected"), admin, FakeSession())
        self.assertEqual(result.status, "rejected")
        self.assertEqual(report.moderated_by_id, admin.id)
        self.assertIsNotNone(report.moderated_at)


if __name__ == "__main__":
    unittest.main()
