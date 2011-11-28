package org.piax.ov.ovs.dtn;

/**
 * {@.en Exception thrown by DTN implementations.}
 * {@.ja DTN 実装により throw される Exception です．}
 */
public class DTNException extends RuntimeException {
    private static final long serialVersionUID = 7584043132342547319L;
    /**
     * {@.en Constructs a new DTNException object.}
     * {@.ja 新規 DTNException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     * @param cause {@.en cause cause of the error (underlying exception)}{@.ja 原因となるエラーです．}
     */
    public DTNException (String message, Throwable cause) {
	super(message, cause);
    }

    /**
     * {@.en Constructs a new DTNException object.}
     * {@.ja 新規 DTNException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     */
    public DTNException (String message) {
	super(message);
    }

    /**
     * {@.en Constructs a new DTNException object.}
     * {@.ja 新規 DTNException を生成します．}
     * @param e {@.en exception}{@.ja exception ．}
     */
    public DTNException (Exception e) {
        super ("Exception thrown in DTN: " + e);
    }
    
}
