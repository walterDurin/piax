package org.piax.trans;

public interface SecurityManager {
    // Convert src object to network representation.
    public byte[] wrap(Object src);
    // Convert network representation to Object.
    public Object unwrap(byte[] src); 
}
