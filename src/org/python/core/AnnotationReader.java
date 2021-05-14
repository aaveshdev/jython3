/*
 * Copyright (c) 2008 Jython Developers
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.core;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class reads a classfile from a byte array and pulls out the value of the class annotation
 * for APIVersion, which can then be retrieved by a call to getVersion().
 *
 * Hopefully the use of ClassReader in this implementation is not too expensive. I suspect it is not
 * since EmptyVisitor is just a bag of empty methods so shouldn't cost too much. If it turns out to
 * cost too much, we will want to implement a special purpose ClassReader that only reads out the
 * APIVersion annotation I think.
 */
public class AnnotationReader extends ClassVisitor {

    private boolean nextVisitIsVersion = false;
    private boolean nextVisitIsMTime = false;
    private boolean nextVisitIsFilename = false;

    private int version = -1;
    private long mtime = -1;
    private String filename = null;

    /**
     * Reads the classfile bytecode in data and to extract the version.
     * @throws IOException - if the classfile is malformed.
     */
    public AnnotationReader(byte[] data) throws IOException {
        super(Opcodes.ASM7);
        ClassReader r;
        try {
            r = new ClassReader(data);
        } catch (ArrayIndexOutOfBoundsException e) {
            IOException ioe = new IOException("Malformed bytecode: not enough data", e);
            throw ioe;
        }
        r.accept(this, 0);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        nextVisitIsVersion = desc.equals("Lorg/python/compiler/APIVersion;");
        nextVisitIsMTime = desc.equals("Lorg/python/compiler/MTime;");
        nextVisitIsFilename = desc.equals("Lorg/python/compiler/Filename;");
        return new AnnotationVisitor(Opcodes.ASM7) {

        	public void visit(String name, Object value) {
        		if (nextVisitIsVersion) {
        			version = (Integer)value;
        			nextVisitIsVersion = false;
        		} else if (nextVisitIsMTime) {
        			mtime = (Long)value;
        			nextVisitIsMTime = false;
        		} else if (nextVisitIsFilename) {
                    filename = (String)value;
                    nextVisitIsFilename = false;
                }
        	}
		};
    }

    public int getVersion() {
        return version;
    }

    public long getMTime() {
        return mtime;
    }

    public String getFilename() {
        return filename;
    }
}
