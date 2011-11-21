package org.piax.ov;

public interface SecurityManager {
    // Convert src object to network representation.
    public byte[] wrap(Object src);
    // Convert network representation to Object.
    public Object unwrap(byte[] src); 
}
