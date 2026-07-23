package RMC_Booking_Engine.rmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * AC coverage matrix — every VALIDATION Part 3 AC-* must be registered.
 *
 * <p>Registry format ({@code ac-coverage.tsv}): {@code AC|STATUS|owner1;owner2}
 *
 * <p>COVERED owners must exist on disk when the full app tree is present (after {@code git pull}
 * of {@code origin/dev-MJ}). GAP / MANUAL / PARTIAL owners may be planned paths.
 *
 * <p>See {@code docs/AC_COVERAGE.md}, {@code docs/VALIDATION.md} Part 4, and {@code docs/README.md}.
 */
class AcCoverageMatrixTest {

  private static final List<String> REQUIRED_ACS =
      List.of(
          "AC-GUEST-001",
          "AC-GUEST-002",
          "AC-GUEST-003",
          "AC-GUEST-004",
          "AC-GUEST-005",
          "AC-GUEST-006",
          "AC-GUEST-007",
          "AC-GUEST-008",
          "AC-INV-001",
          "AC-INV-002",
          "AC-INV-003",
          "AC-INV-004",
          "AC-PAY-001",
          "AC-PAY-002",
          "AC-PAY-003",
          "AC-PAY-004",
          "AC-PAY-005",
          "AC-PAY-006",
          "AC-PAY-007",
          "AC-PAY-008",
          "AC-PAY-009",
          "AC-PAY-010",
          "AC-CXL-001",
          "AC-CXL-002",
          "AC-CXL-003",
          "AC-CXL-004",
          "AC-CXL-005",
          "AC-CXL-006",
          "AC-CXL-007",
          "AC-STAFF-001",
          "AC-STAFF-002",
          "AC-STAFF-003",
          "AC-STAFF-004",
          "AC-SEC-001",
          "AC-SEC-002",
          "AC-SEC-003",
          "AC-SEC-004",
          "AC-SEC-005",
          "AC-SEC-006",
          "AC-SEC-007",
          "AC-SEC-008",
          "AC-OBS-001",
          "AC-OBS-002",
          "AC-OBS-003",
          "AC-CFG-001",
          "AC-OPS-001",
          "AC-OPS-002",
          "AC-OPS-003",
          "AC-OPS-004",
          "AC-DOC-001",
          "AC-DOC-002",
          "AC-DOC-003",
          "AC-DOC-004",
          "AC-UI-001",
          "AC-UI-002",
          "AC-UI-003",
          "AC-UI-004");

  private static final Set<String> ALLOWED_STATUSES =
      Set.of("COVERED", "PARTIAL", "GAP", "MANUAL");

  @Test
  void everyAcIsRegisteredWithValidStatusAndOwners() throws Exception {
    List<CoverageEntry> entries = loadCoverage();
    Set<String> seen = new HashSet<>();

    for (CoverageEntry entry : entries) {
      assertFalse(entry.ac().isBlank(), "ac must not be blank");
      assertTrue(
          ALLOWED_STATUSES.contains(entry.status()),
          () -> entry.ac() + " has invalid status: " + entry.status());
      assertFalse(entry.owners().isEmpty(), () -> entry.ac() + " needs owners");
      assertTrue(seen.add(entry.ac()), () -> "duplicate AC: " + entry.ac());
    }

    for (String required : REQUIRED_ACS) {
      assertTrue(seen.contains(required), () -> "missing AC registration: " + required);
    }
    assertEquals(REQUIRED_ACS.size(), seen.size(), "unexpected extra or missing ACs in registry");
  }

  @Test
  void coveredOwnersExistWhenFullTreeIsPresent() throws Exception {
    Path repoRoot = resolveRepoRoot();
    List<CoverageEntry> entries = loadCoverage();
    boolean sawCovered = false;
    boolean fullTree =
        Files.exists(
            repoRoot.resolve(
                "rmc_backend/src/main/java/RMC_Booking_Engine/rmc/service/MayaPaymentService.java"));

    for (CoverageEntry entry : entries) {
      if (!"COVERED".equals(entry.status())) {
        continue;
      }
      sawCovered = true;
      if (!fullTree) {
        continue;
      }
      for (String relative : entry.owners()) {
        Path ownerPath = repoRoot.resolve(relative);
        assertTrue(
            Files.exists(ownerPath),
            () -> entry.ac() + " COVERED owner missing: " + relative);
      }
    }
    assertTrue(sawCovered, "expected at least one COVERED AC");
  }

  private static List<CoverageEntry> loadCoverage() throws Exception {
    try (var in =
            AcCoverageMatrixTest.class.getClassLoader().getResourceAsStream("ac-coverage.tsv");
        var reader =
            new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(in, "ac-coverage.tsv missing"),
                    StandardCharsets.UTF_8))) {
      List<CoverageEntry> entries = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        final String raw = line.trim();
        if (raw.isEmpty() || raw.startsWith("#")) {
          continue;
        }
        String[] parts = raw.split("\\|", 3);
        assertEquals(3, parts.length, () -> "bad registry line: " + raw);
        List<String> owners = List.of(parts[2].split(";"));
        entries.add(new CoverageEntry(parts[0].trim(), parts[1].trim(), owners));
      }
      return entries;
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (Files.exists(cwd.resolve("docs/AC_COVERAGE.md"))) {
      return cwd;
    }
    Path parent = cwd.getParent();
    if (parent != null && Files.exists(parent.resolve("docs/AC_COVERAGE.md"))) {
      return parent;
    }
    return cwd;
  }

  private record CoverageEntry(String ac, String status, List<String> owners) {}
}
