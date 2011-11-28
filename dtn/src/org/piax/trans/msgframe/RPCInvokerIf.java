/*
 * RPCInvokerIf.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
 * Communications Technology
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
 * 2007/11/01 designed and implemented by M. Yoshida.
 * 
 * $Id: RPCInvokerIf.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.msgframe;

/**
 * stub生成の対象となるオブジェクトが他のピアからRPC呼び出し可能であることを
 * 示すインタフェース。
 * RPC呼び出しされるメソッドは、RPCServiceIfを実装したインタフェースに宣言
 * しておく必要がある。
 * 
 * @author      Mikio Yoshida
 * @version     2.2.0
 */
public interface RPCInvokerIf {

}
