package org.piax.trans;

/**
 * {@.en Exception thrown by implementations when unsupported target, command or payload is specified.}
 * {@.ja 実装によりサポートされないクエリやペイロードが指定されたとき; throw される Exception です．}
 */
public class ProtocolUnsupportedException extends RuntimeException {
    private static final long serialVersionUID = -4296454437797995503L;

    /**
     * {@.en Constructs a new UnsupoortedTargetException object.}
     * {@.ja 新規 UnsupoortedTargetException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     * @param cause {@.en cause cause of the error (underlying exception)}{@.ja 原因となるエラーです．}
     */
    public ProtocolUnsupportedException (String message, Throwable cause) {
    super(message, cause);
    }

    /**
     * {@.en Constructs a new UnsupoortedTargetException object.}
     * {@.ja 新規 UnsupoortedTargetException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     */
    public ProtocolUnsupportedException (String message) {
    super(message);
    }

    /**
     * {@.en Constructs a new UnsupoortedTargetException object.}
     * {@.ja 新規 UnsupoortedTargetException を生成します．}
     * @param e {@.en exception}{@.ja exception ．}
     */
    public ProtocolUnsupportedException (Exception e) {
        super ("Exception thrown: " + e);
    }
    
}
