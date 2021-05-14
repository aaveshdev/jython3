package org.python.util.install;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class UnicodeSequencesTest extends TestCase {

    private static Set _latin1Encodings;

    public void testUmlaute() {
        String fileEncoding = System.getProperty("file.encoding", "unknown");
        if (getLatin1Encodings().contains(fileEncoding)) {
            assertEquals("�", UnicodeSequences.a2);
            assertEquals("�", UnicodeSequences.A2);
            assertEquals("�", UnicodeSequences.o2);
            assertEquals("�", UnicodeSequences.O2);
            assertEquals("�", UnicodeSequences.u2);
            assertEquals("�", UnicodeSequences.U2);
        }
    }

    private static Set getLatin1Encodings() {
        if (_latin1Encodings == null) {
            _latin1Encodings = new HashSet(3);
            _latin1Encodings.add("ISO-LATIN-1");
            _latin1Encodings.add("ISO-8859-1");
            _latin1Encodings.add("Cp1252");
        }
        return _latin1Encodings;
    }

}
