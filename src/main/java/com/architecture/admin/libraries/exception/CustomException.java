package com.architecture.admin.libraries.exception;

/*****************************************************
 * 예외 처리 - 사용자 예외처리
 ****************************************************/
public class CustomException extends RuntimeException {
    private Exception e;
    private CustomError customError;
    private Object data = "";

    public CustomException(CustomError customError) {

        this.customError = customError;
    }

    public CustomException(CustomError customError, Object data) {

        this.customError = customError;
        this.data = data;
    }

    public CustomException(Exception e, CustomError customError) {

        this.e = e;
        this.customError = customError;
    }

    public CustomException(Exception e, CustomError customError, Object data) {

        this.e = e;
        this.customError = customError;
        this.data = data;
    }

    public Exception getE() {
        return e;
    }

    public CustomError getCustomError() {
        return customError;
    }

    public Object getData() {
        return data;
    }
}
