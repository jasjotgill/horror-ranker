package io.github.jasjotgill.horror_ranker.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.jasjotgill.horror_ranker.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Applies to every controller: when one throws, the matching method here builds the response.
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return respond(ex.getStatus(), ex.getMessage());
	}

	// A @Valid request body broke one of its constraints.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(new ErrorResponse(status.value(), "Validation failed", fields));
	}

	// The body was missing or was not valid JSON.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		return respond(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON");
	}

	// Safety net for a database constraint the service did not check first, e.g. two people
	// joining with the same nickname at the same instant.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(DataIntegrityViolationException ex) {
		return respond(HttpStatus.CONFLICT, "That conflicts with something already saved. Please try again.");
	}

	private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
	}

}
