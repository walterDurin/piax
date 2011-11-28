/*
 * ObjectInputStreamX.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Revision History:
 * ---
 * 2007/01/07 designed and implemented by M. Yoshida.
 * 
 * $Id: ObjectInputStreamX.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * ObjectInputStream which can be specified the class loader.
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class ObjectInputStreamX extends ObjectInputStream {

    private ClassLoader loader;
    
    public ObjectInputStreamX(InputStream in, ClassLoader loader) 
            throws IOException {
        super(in);
        if (loader == null) {
            this.loader = ClassLoader.getSystemClassLoader();
        } else {
            this.loader = loader;
        }
//        enableResolveObject(true);
    }

    /* (non-Javadoc)
     * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass classDesc)
            throws IOException, ClassNotFoundException {
        String className = classDesc.getName();
        return Class.forName(className, false, loader);
    }
}
