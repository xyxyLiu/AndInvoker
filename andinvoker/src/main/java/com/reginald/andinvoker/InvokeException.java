package com.reginald.andinvoker;

/**
 * 远程调用失败异常
 */
public class InvokeException extends IllegalStateException {

    public InvokeException() {
    }

    public InvokeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvokeException(String message) {
        super(message);
    }

    public InvokeException(Throwable cause) {
        super(cause);
    }
}
