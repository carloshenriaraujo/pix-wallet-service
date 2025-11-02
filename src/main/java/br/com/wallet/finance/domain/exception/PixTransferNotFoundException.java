package br.com.wallet.finance.domain.exception;

public class PixTransferNotFoundException extends RuntimeException {
    public PixTransferNotFoundException(String message) {
        super(message);
    }
}