package org.piax.ov;

/**
 * {@.en Exception thrown by Overlay implementations when the query is not supported.}
 * {@.ja Overlay 実装によりサポートされないクエリが指定されたとき; throw される Exception です．}
 */
public class UnsupportedConditionException extends RuntimeException {
    private static final long serialVersionUID = -4296454437797995503L;

    /**
     * {@.en Constructs a new UnsupportedConditionException object.}
     * {@.ja 新規 UnsupportedConditionException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     * @param cause {@.en cause cause of the error (underlying exception)}{@.ja 原因となるエラーです．}
     */
    public UnsupportedConditionException (String message, Throwable cause) {
    super(message, cause);
    }

    /**
     * {@.en Constructs a new UnsupportedConditionException object.}
     * {@.ja 新規 UnsupportedConditionException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     */
    public UnsupportedConditionException (String message) {
    super(message);
    }

    /**
     * {@.en Constructs a new UnsupportedConditionException object.}
     * {@.ja 新規 UnsupportedConditionException を生成します．}
     * @param e {@.en exception}{@.ja exception ．}
     */
    public UnsupportedConditionException (Exception e) {
        super ("Exception thrown: " + e);
    }
    
}
