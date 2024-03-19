package com.architecture.admin.libraries.exception;


import com.architecture.admin.libraries.TelegramLibrary;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;

/*****************************************************
 * 예외 처리 - 기본 제공
 ****************************************************/
@RestControllerAdvice
@ResponseStatus(HttpStatus.OK)
@Slf4j
public class CommonExceptionLibrary {

    /**
     * 텔레그램
     */
    @Autowired(required = false)
    protected TelegramLibrary telegramLibrary;


    @ExceptionHandler(Exception.class)
    public String except(Exception e, Model model) {
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        model.addAttribute("exception", e);
        return displayError(e.getMessage(), "9500");
    }

    @ExceptionHandler(RuntimeException.class)
    public String runtimeException(Exception e){
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        CustomError customError = CustomError.BAD_REQUEST;
        log.error("ErrorClass : {}", e.getClass());
        log.error("ErrorMessage : {}", e.getMessage());
        return displayError(customError.getMessage(), customError.getCode());
    }

    // 파라미터 타입 미스매치(bad request)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String methodArgumentTypeMismatchException(Exception e){
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        CustomError customError = CustomError.BAD_REQUEST_PARAMETER_TYPE_MISMATCH;
        // 에러 로그 개발단계에서만 사용
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(customError.getMessage(), customError.getCode());
    }

    // 사용자 필수 파라미터값 미입력(bad request)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String missingServletRequestParameterException(Exception e){
        CustomError error = CustomError.BAD_REQUEST_REQUIRED_VALUE;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode());
    }

    // SQL 관련 오류 (서버 오류)
    @ExceptionHandler(SQLException.class)
    public String sqlException(Exception e){
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        CustomError error = CustomError.SERVER_SQL_ERROR;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode());
    }

    // DB 관련 오류 (서버 오류) ex.duplicatedKeyException
    @ExceptionHandler(DataAccessException.class)
    public String dataAccessException(Exception e){
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        CustomError error = CustomError.SERVER_DATABASE_ERROR;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode());
    }

    @ExceptionHandler(BindException.class)
    public String exceptBind(BindException e) {
        telegramLibrary.sendMessage("@@ErrorClass : " + e.getClass() + "@@ErrorMessage : "+ e.getMessage());
        final String sMESSAGE = "message";
        final String[] message = new String[1];
        JSONObject obj = new JSONObject();
        obj.put("result", false);
        e.getAllErrors().forEach(objectError -> {
            if (!obj.has(sMESSAGE) || obj.getString(sMESSAGE) == null) {
                message[0] = objectError.getDefaultMessage();
            }
        });

        return displayError(message[0], "9400");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404(NoHandlerFoundException e) {
        return displayError(e.getMessage(), "9404");
    }

    @ExceptionHandler(CustomException.class)
    public String customExceptionHandle(CustomException e) {
        return displayError(e.getCustomError().getMessage(), e.getCustomError().getCode());
    }

    private String displayError(String sMessage, String sCode) {
        JSONObject obj = new JSONObject();
        obj.put("result", false);
        obj.put("code", sCode);
        obj.put("message", sMessage);

        return obj.toString();
    }
}
