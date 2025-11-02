package br.com.wallet.finance.domain.exception;

public class PixKeyAlreadyExistsException extends RuntimeException {
    public PixKeyAlreadyExistsException(String message) {
        super(message);
    }
}