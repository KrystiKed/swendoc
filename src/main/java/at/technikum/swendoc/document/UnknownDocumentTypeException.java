package at.technikum.swendoc.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownDocumentTypeException extends RuntimeException {

    public UnknownDocumentTypeException(String type) {
        super("Unknown document type " + type + "; expected word, pdf or excel");
    }
}
