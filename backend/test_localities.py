import unittest

from app.routes import BOGOTA_LOCALITIES, canonical_bogota_locality


class BogotaLocalityTests(unittest.TestCase):
    def test_all_twenty_localities_are_available(self):
        self.assertEqual(len(BOGOTA_LOCALITIES), 20)
        self.assertEqual(len(set(BOGOTA_LOCALITIES)), 20)

    def test_matching_is_case_insensitive_and_returns_canonical_name(self):
        self.assertEqual(canonical_bogota_locality("  usaquén "), "Usaquén")
        self.assertEqual(canonical_bogota_locality("CIUDAD BOLÍVAR"), "Ciudad Bolívar")

    def test_unknown_locality_is_rejected(self):
        self.assertIsNone(canonical_bogota_locality("Bogotá Centro"))


if __name__ == "__main__":
    unittest.main()
