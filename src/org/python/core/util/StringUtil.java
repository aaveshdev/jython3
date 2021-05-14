/* Copyright (c) 2012 Jython Developers */
package org.python.core.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.python.core.BaseBytes;
import org.python.core.Py;
import org.python.core.PyBuffer;

/**
 * String Utility methods.
 *
 */
public class StringUtil {

    /**
     * Encodes this String into a sequence of bytes. Each byte contains the low-order bits of its
     * corresponding char.
     *
     * @param string a String value
     * @return a byte array with one byte for each char in string
     */
    public static byte[] toBytes(String string) {
        try {
            return string.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {
            // This JVM is whacked, it doesn't even have iso-8859-1
            throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
        }
    }

    /**
     * Return a new String with chars corresponding to buf from off to off + len.
     *
     * @param buf an array of bytes
     * @param off the initial offset
     * @param len the length
     * @return a new String corresponding to the bytes in buf
     */
    @SuppressWarnings("deprecation")
    public static String fromBytes(byte[] buf, int off, int len) {
        // Yes, I know the method is deprecated, but it is the fastest
        // way of converting between between byte[] and String
        return new String(buf, 0, off, len);
    }

    /**
     * Return a new String with chars corresponding to buf.
     *
     * @param buf an array of bytes
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(byte[] buf) {
        return fromBytes(buf, 0, buf.length);
    }

    /**
     * Return a new String with chars corresponding to buf.
     *
     * @param buf a ByteBuffer of bytes
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(ByteBuffer buf) {
        return fromBytes(buf.array(), buf.arrayOffset() + buf.position(),
                buf.arrayOffset() + buf.limit());
    }

    /**
     * Return a new String with chars corresponding to buf, which is a byte-oriented buffer obtained
     * through the buffer API. It depends on the implementation of {@link PyBuffer#toString()}
     * provided by each buffer implementation.
     *
     * @param buf a PyBuffer of bytes
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(PyBuffer buf) {
        return buf.toString();
    }

    /**
     * Return a new String with chars corresponding to b.
     *
     * @param b a BaseBytes containing bytes
     * @return a new String corresponding to the bytes in b
     */
    public static String fromBytes(BaseBytes b) {

        int size = b.__len__();
        StringBuilder buf = new StringBuilder(size);
        for (int j = 0; j < size; j++) {
            buf.append((char)b.intAt(j));
        }
        return buf.toString();
    }

    /**
     * Decapitalize a String if it begins with a capital letter, e.g.:
     * {@code decapitalize("FooBar") == "fooBar"}
     *
     * @param string a String
     * @return a decapitalized String
     */
    public static String decapitalize(String string) {
        char c0 = string.charAt(0);
        if (!Character.isUpperCase(c0)) {
            return string;
        }
        if (string.length() > 1 && Character.isUpperCase(string.charAt(1))) {
            return string;
        }
        char[] chars = string.toCharArray();
        chars[0] = Character.toLowerCase(c0);
        return new String(chars);
    }
}
