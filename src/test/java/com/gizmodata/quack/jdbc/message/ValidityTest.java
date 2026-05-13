package com.gizmodata.quack.jdbc.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidityTest {

    @Test
    void wordCountRoundsUpToNextLong() {
        assertEquals(0, Validity.wordCount(0));
        assertEquals(1, Validity.wordCount(1));
        assertEquals(1, Validity.wordCount(64));
        assertEquals(2, Validity.wordCount(65));
        assertEquals(16, Validity.wordCount(1024));
    }

    @Test
    void wireByteCountMatches64BitAlignment() {
        assertEquals(0, Validity.wireByteCount(0));
        assertEquals(8, Validity.wireByteCount(1));
        assertEquals(8, Validity.wireByteCount(64));
        assertEquals(16, Validity.wireByteCount(65));
    }

    @Test
    void nullValidityMeansEveryRowValid() {
        assertTrue(Validity.isValid(null, 0));
        assertTrue(Validity.isValid(null, 1000));
        assertFalse(Validity.isNull(null, 5));
    }

    @Test
    void bitPositionsMatchWireOrder() {
        // Bit N of byte M corresponds to row (M*8 + N).
        long[] validity = Validity.fromBytes(new byte[]{(byte) 0b0000_0001}, 1);
        assertTrue(Validity.isValid(validity, 0));

        validity = Validity.fromBytes(new byte[]{(byte) 0b1000_0000}, 8);
        assertTrue(Validity.isValid(validity, 7));
        assertFalse(Validity.isValid(validity, 0));

        // Row 65 -> byte 8, bit 1
        validity = Validity.fromBytes(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0b00000010}, 66);
        assertTrue(Validity.isValid(validity, 65));
        assertFalse(Validity.isValid(validity, 64));
    }

    @Test
    void roundTripsThroughBytes() {
        // Mark rows 0, 3, 7, 63, 64, 127 valid; the rest null
        int count = 128;
        long[] validity = new long[Validity.wordCount(count)];
        int[] valid = {0, 3, 7, 63, 64, 127};
        for (int r : valid) Validity.setValid(validity, r);

        byte[] wire = Validity.toBytes(validity, count);
        long[] roundTripped = Validity.fromBytes(wire, count);

        for (int r = 0; r < count; r++) {
            boolean expectValid = false;
            for (int v : valid) if (v == r) expectValid = true;
            assertEquals(expectValid, Validity.isValid(roundTripped, r),
                    "row " + r + " validity mismatch after round-trip");
        }
    }

    @Test
    void allValidSetsEveryBitUpToCount() {
        long[] validity = Validity.allValid(70);
        for (int i = 0; i < 70; i++) {
            assertTrue(Validity.isValid(validity, i), "row " + i + " expected valid");
        }
        // Bits past count are 0 and would be reported as invalid — but we don't
        // call isValid past count, so this is fine.
    }

    @Test
    void setNullClearsBit() {
        long[] validity = Validity.allValid(10);
        assertTrue(Validity.isValid(validity, 5));
        Validity.setNull(validity, 5);
        assertFalse(Validity.isValid(validity, 5));
        // Neighbors untouched
        assertTrue(Validity.isValid(validity, 4));
        assertTrue(Validity.isValid(validity, 6));
    }
}
