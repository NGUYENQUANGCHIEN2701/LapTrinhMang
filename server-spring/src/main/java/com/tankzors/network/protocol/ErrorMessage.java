package com.tankzors.network.protocol;

/**
 * Message gửi từ server về client khi có lỗi xảy ra.
 */
public class ErrorMessage extends GameMessage {

    private String message;
    private int code;

    public ErrorMessage() {
        super("ERROR");
    }

    public ErrorMessage(String message, int code) {
        super("ERROR");
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
