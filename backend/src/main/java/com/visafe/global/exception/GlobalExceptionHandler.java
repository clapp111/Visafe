package com.visafe.global.exception;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error(
                    "RestApiException: code={}, status={}",
                    errorCode.getCode(),
                    errorCode.getHttpStatus().value(),
                    e);
        } else {
            log.warn(
                    "RestApiException: code={}, status={}, message={}",
                    errorCode.getCode(),
                    errorCode.getHttpStatus().value(),
                    errorCode.getMessage());
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST;
        log.warn(
                "MethodArgumentNotValidException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());

        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : errorCode.getMessage();

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST_TYPE;
        log.warn(
                "MethodArgumentTypeMismatchException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_REQUEST_TYPE;
        log.warn(
                "HttpMessageNotReadableException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INVALID_PARAMETER;
        log.warn(
                "MissingServletRequestParameterException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestCookie(MissingRequestCookieException e) {
        ErrorCode errorCode = CustomErrorCode.JWT_REFRESH_MISSING;
        log.warn(
                "MissingRequestCookieException: code={}, status={}, cookie={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getCookieName());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_HANDLER_NOT_FOUND;
        log.warn(
                "NoHandlerFoundException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.create(errorCode.getCode(), ""));
    }

    // Spring 6.1+에서 매핑되지 않은 URL은 NoHandlerFoundException 대신 이 예외로 들어온다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_HANDLER_NOT_FOUND;
        log.warn(
                "NoResourceFoundException: code={}, status={}, method={}, path={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getHttpMethod(),
                e.getResourcePath());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_METHOD_NOT_ALLOWED;
        log.warn(
                "HttpRequestMethodNotSupportedException: code={}, status={}, method={}, supported={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getMethod(),
                e.getSupportedHttpMethods());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(errorCode.getHttpStatus());
        Set<HttpMethod> supportedMethods = e.getSupportedHttpMethods();
        if (supportedMethods != null) {
            response.allow(supportedMethods.toArray(HttpMethod[]::new)); // Allow 헤더 (RFC 9110)
        }
        return response.body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE;
        log.warn(
                "HttpMediaTypeNotSupportedException: code={}, status={}, contentType={}, supported={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getContentType(),
                e.getSupportedMediaTypes());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(errorCode.getHttpStatus());
        List<MediaType> supportedMediaTypes = e.getSupportedMediaTypes();
        if (!supportedMediaTypes.isEmpty()) {
            response.header(HttpHeaders.ACCEPT, MediaType.toString(supportedMediaTypes)); // 받을 수 있는 형식 안내
        }
        return response.body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    // 로그인 실패. 이메일이 없는 경우도 같은 예외·메시지로 응답한다. (가입된 이메일인지 알 수 없게)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        ErrorCode errorCode = CustomErrorCode.AUTH_LOGIN_FAILED;
        log.warn(
                "BadCredentialsException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    // 컨트롤러·서비스에서 권한 검사(@PreAuthorize 등)에 실패한 경우. 없으면 아래 Exception 핸들러가 500으로 바꾼다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = CustomErrorCode.AUTH_FORBIDDEN;
        log.warn(
                "AccessDeniedException: code={}, status={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        ErrorCode errorCode = CustomErrorCode.COMMON_INTERNAL_SERVER_ERROR;
        log.error(
                "UnexpectedException: code={}, status={}",
                errorCode.getCode(),
                errorCode.getHttpStatus().value(),
                e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.create(errorCode.getCode(), errorCode.getMessage()));
    }
}
