package demos.springdata.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends RuntimeException {

    private final HttpStatus errorCode;


    public PaymentException(HttpStatus errorCode) {
        this.errorCode = errorCode;
    }

    public PaymentException(String message, HttpStatus errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatus getErrorCode() {
        return errorCode;
    }
}
