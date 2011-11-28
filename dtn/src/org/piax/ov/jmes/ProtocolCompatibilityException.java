package org.piax.ov.jmes;

/**
 * {@.en Exception thrown by implementations when protocol is not compatible.}
 * {@.ja プロトコルが異なり扱えない場合に throw される Exception です．}
 */
public class ProtocolCompatibilityException extends RuntimeException {

    private static final long serialVersionUID = -345163264636407845L;

    /**
     * {@.en Constructs a new ProtocolCompatibilityException object.}
     * {@.ja 新規 ProtocolCompatibilityException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     * @param cause {@.en cause cause of the error (underlying exception)}{@.ja 原因となるエラーです．}
     */
    public ProtocolCompatibilityException (String message, Throwable cause) {
    super(message, cause);
    }

    /**
     * {@.en Constructs a new ProtocolCompatibilityException object.}
     * {@.ja 新規 ProtocolCompatibilityException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     */
    public ProtocolCompatibilityException (String message) {
    super(message);
    }

    /**
     * {@.en Constructs a new ProtocolCompatibilityException object.}
     * {@.ja 新規 ProtocolCompatibilityException を生成します．}
     * @param e {@.en exception}{@.ja exception ．}
     */
    public ProtocolCompatibilityException (Exception e) {
        super ("Exception thrown: " + e);
    }
    
}
