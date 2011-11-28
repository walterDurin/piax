package org.piax.ov.jmes.von;

public class VONException extends RuntimeException {
    private static final long serialVersionUID = -5842823389153834736L;

    /**
     * {@.en Constructs a new VONException object.}
     * {@.ja 新規 VONException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     * @param cause {@.en cause cause of the error (underlying exception)}{@.ja 原因となるエラーです．}
     */
    public VONException (String message, Throwable cause) {
    super(message, cause);
    }

    /**
     * {@.en Constructs a new VONException object.}
     * {@.ja 新規 VONException を生成します．}
     * @param message {@.en message description of the error}{@.ja エラーメッセージです．}
     */
    public VONException (String message) {
    super(message);
    }

    /**
     * {@.en Constructs a new VONException object.}
     * {@.ja 新規 VONException を生成します．}
     * @param e {@.en exception}{@.ja exception ．}
     */
    public VONException (Exception e) {
        super ("Exception thrown in VON: " + e);
    }

}
