package co.com.validate.license.exception;

import java.util.Date;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler{

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public @ResponseBody ResponseEntity<ExceptionResponse> handlerAccessDeniedException(final Exception ex,
			final HttpServletRequest request, final HttpServletResponse response) {
		ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "HTTP ERROR 403 Forbidden", "Forbidden");
		return new ResponseEntity<>(exceptionResponse, HttpStatus.FORBIDDEN);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@Override
	@Nullable
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.error("ERROR handleMethodArgumentNotValid:", ex);
		ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Validacion fallida", request.getDescription(false));
		return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(InvalidDataAccessApiUsageException.class)
	public final ResponseEntity<ExceptionResponse> invalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex, WebRequest request){
		log.error("ERROR invalidDataAccessApiUsageException:", ex);
		ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(), request.getDescription(false));
		return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public final ResponseEntity<ExceptionResponse> manejarTodasExcepciones(Exception ex, WebRequest request){
		log.error("ERROR Exception:", ex);
		ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(), request.getDescription(false));
		return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Getter
	@Setter
	public class ExceptionResponse {

		private Date timestamp;
		private String mensaje;
		private String detalles;

		public ExceptionResponse() {
		}

		public ExceptionResponse(Date timestamp, String mensaje, String detalles) {
			this.timestamp = timestamp;
			this.mensaje = mensaje;
			this.detalles = detalles;
		}

	}

}
