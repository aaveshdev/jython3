/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class Util {

    private static final String UTF_8 = "UTF-8";
    private static final char SEPCHAR = File.separatorChar;
    private static final String SEP = File.separator;
    private static final String INIT_PY = "__init__.py";
    private static final String SEP_INIT_PY = SEP + INIT_PY;
    private static int gensymCount = -1;

    public static String gensym(String base) {
        gensymCount++;
        return base + gensymCount;
    }

    public static String getSystemTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp.endsWith(SEP)) {
            return tmp;
        }
        return tmp + SEP;
    }

    /**
     * Returns the parent qname of {@code qname} -- everything up to the
     * last dot (exclusive), or if there are no dots, the empty string.
     */
    public static String getQnameParent(String qname) {
        if (qname == null || qname.length() == 0) {
            return "";
        }
        int index = qname.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return qname.substring(0, index);
    }

    /**
     * Determines the fully-qualified module name for the specified file.  A
     * module's qname is a function of the module's absolute path and the sys
     * path; it does not depend on how the module name may have been specified
     * in import statements. The module qname is the relative path of the module
     * under the load path, with slashes converted to dots.
     *
     * @param file absolute canonical path to a file (__init__.py for dirs)
     * @return null if {@code file} is not somewhere under the load path
     */
    public static String moduleQname(String file) {
        boolean initpy = file.endsWith(SEP_INIT_PY);
        if (initpy) {
            file = file.substring(0, file.length() - SEP_INIT_PY.length());
        } else if (file.endsWith(".py")) {
            file = file.substring(0, file.length() - ".py".length());
        }
        for (String root : Indexer.idx.getLoadPath()) {
            if (file.startsWith(root)) {
                return file.substring(root.length()).replace(SEPCHAR, '.');
            }
        }
        return null;
    }

    public static String arrayToString(Collection<String> strings) {
        StringBuffer sb = new StringBuffer();
        for (String s : strings) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    public static String arrayToSortedStringSet(Collection<String> strings) {
        Set<String> sorter = new TreeSet<String>();
        sorter.addAll(strings);
        return arrayToString(sorter);
    }

    /**
     * Given an absolute {@code path} to a file (not a directory),
     * returns the module name for the file.  If the file is an __init__.py,
     * returns the last component of the file's parent directory, else
     * returns the filename without path or extension.
     */
    public static String moduleNameFor(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            throw new IllegalStateException("failed assertion: " + path);
        }
        String fname = f.getName();
        if (fname.equals(INIT_PY)) {
            return f.getParentFile().getName();
        }
        return fname.substring(0, fname.lastIndexOf('.'));
    }

    public static File joinPath(File dir, String file) {
        return joinPath(dir.getAbsolutePath(), file);
    }

    public static File joinPath(String dir, String file) {
        if (dir.endsWith(SEP)) {
            return new File(dir + file);
        }
        return new File(dir + SEP + file);
    }

    public static void writeFile(String path, String contents) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            out.print(contents);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static String readFile(String filename) throws Exception {
        return readFile(new File(filename));
    }

    public static String readFile(File path) throws Exception {
        // Don't use line-oriented file read -- need to retain CRLF if present
        // so the style-run and link offsets are correct.
        return new String(getBytesFromFile(path), UTF_8);
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("file too large: " + file);
            }

            byte[] bytes = new byte[(int)length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                   && (numRead = is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Failed to read whole file " + file);
            }
            return bytes;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static String getMD5(File path) throws Exception {
        byte[] bytes = getBytesFromFile(path);
        return getMD5(bytes);
    }

    public static String getMD5(byte[] fileContents) throws Exception {
	MessageDigest algorithm = MessageDigest.getInstance("MD5");
	algorithm.reset();
	algorithm.update(fileContents);
	byte messageDigest[] = algorithm.digest();
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < messageDigest.length; i++) {
            sb.append(String.format("%02x", 0xFF & messageDigest[i]));
	}
	return sb.toString();
    }

    /**
     * Return absolute path for {@code path}.
     * Make sure path ends with SEP if it's a directory.
     * Does _not_ resolve symlinks, since the caller may need to play
     * symlink tricks to produce the desired paths for loaded modules.
     */
    public static String canonicalize(String path) {
        File f = new File(path);
        path = f.getAbsolutePath();
        if (f.isDirectory() && !path.endsWith(SEP)) {
            return path + SEP;
        }
        return path;
    }

    static boolean isReadableFile(String path) {
        File f = new File(path);
        return f.canRead() && f.isFile();
    }
}
