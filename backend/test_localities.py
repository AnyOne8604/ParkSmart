import unittest
from fastapi import HTTPException

from app.routes import BOGOTA_LOCALITIES, canonical_bogota_locality, validate_map_filters


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


if __name__ == "__main__":
    unittest.main()
