"""generates code for binops in PyObject and for all "simple" ops in PyInstance"""

from java_templating import JavaTemplate as jt, concat, csub, strfy

binops = \
	[('add', '+'), ('sub', '-'), ('mul', '*'), ('div', '/'),
         ('floordiv', '//'), ('truediv', '/'),
	 ('mod', '%'), ('divmod', 'divmod'), ('pow', '**'), 
	 ('lshift', '<<'), ('rshift', '>>'), ('and', '&'), ('or', '|'), ('xor', '^')]

template = jt("""
    `csub`(
    /**
     * Equivalent to the standard Python __%(name)s__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the %(name)s, or null if this operation
     *            is not defined
     **/
     );
    public PyObject `concat`(__,`name, __)(PyObject other) { `function; }

    `csub`(
    /**
     * Equivalent to the standard Python __r%(name)s__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the %(name)s, or null if this operation
     *            is not defined.
     **/
     );
    public PyObject `concat`(__r,`name, __)(PyObject other) { `rfunction; }

    `csub`(
    /**
     * Equivalent to the standard Python __i%(name)s__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the i%(name)s, or null if this operation
     *            is not defined
     **/
     );
    public PyObject `concat`(__i, `name, __)(PyObject other) { `ifunction; }

    `csub`(
   /**
     * Implements the Python expression <code>this %(bareop)s o2</code>
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the %(name)s.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
     );
    public final PyObject `concat`(_, `name)(PyObject o2) {
    	`divhook;
    	PyType t1 = this.getType();
    	PyType t2 = o2.getType();
    	if (t1 == t2 || t1.builtin && t2.builtin) {
    		return this.`concat`(_basic_, `name)(o2);
    	}
    	return _binop_rule(t1, o2, t2, `strfy`(`concat`(__,`name,__)),
                                       `strfy`(`concat`(__r,`name,__)),
                                       `op);
    }

    `csub`(
    /**
     * Implements the Python expression <code>this %(bareop)s o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the %(name)s.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
     );
    final PyObject `concat`(_basic_, `name)(PyObject o2) {
        PyObject x = `concat`(__,`name,__)(o2);
        if (x != null) {
            return x;
        }
        x = o2.`concat`(__r,`name,__)(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop(`op, o2));
    }

    `csub`(
   /**
     * Implements the Python expression <code>this %(bareop)s= o2</code>
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the i%(name)s.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
     );
    public final PyObject `concat`(_i, `name)(PyObject o2) {
    	`idivhook;
    	PyType t1 = this.getType();
    	PyType t2 = o2.getType();
    	if (t1 == t2 || t1.builtin && t2.builtin) {
    		return this.`concat`(_basic_i, `name)(o2);
    	}
        PyObject impl = t1.lookup(`strfy`(`concat`(__i,`name,__)));
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
    	return _binop_rule(t1, o2, t2, `strfy`(`concat`(__,`name,__)),
                                       `strfy`(`concat`(__r,`name,__)),
                                       `op);
    }

    `csub`(
    /**
     * Implements the Python expression <code>this %(bareop)s= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the i%(name)s.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
     );
    final PyObject `concat`(_basic_i, `name)(PyObject o2) {
        PyObject x = `concat`(__i,`name,__)(o2);
        if (x != null) {
            return x;
        }
        return this.`concat`(_basic_, `name)(o2);
    }
    
""", bindings={'csub': csub, 'concat': concat,
               'strfy': strfy}, start='ClassBodyDeclarations')

def main():
    fp = open('binops.txt', 'w')

    fp.write('    // Generated by make_binops.py (Begin)\n\n')

    for name, op in binops:
            ifunction = rfunction = function = jt('return null;')
            divhook = jt("")
            idivhook = jt("")

            if name == 'pow':
                    function = jt('return __pow__(other, null);')
            if name == 'div':
                    hook = '''
            if (Options.Qnew)
                return _%struediv(o2);'''
                    divhook = jt(hook % '')
                    idivhook = jt(hook % 'i')


            template.tnaked().texpand(
                {
                'name': jt(name),
                'op': jt('"%s"' % op),
                'bareop': op,
                'function':function,
                'rfunction':rfunction,
                'ifunction':ifunction,
                'divhook':divhook,
                'idivhook':idivhook
                }, output=fp, nindent=1)
            fp.write('\n')
            fp.write('\n')

    fp.write('    // Generated by make_binops.py (End)\n\n')

    fp.close()

if __name__ == '__main__':
    main()
