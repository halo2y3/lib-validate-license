package co.com.validate.license.exception;

public class EmailException extends RuntimeException {

	private static final long serialVersionUID = 1412040912143293559L;


	/***
	 * create a {@link EmailException} instance with message process result
	 * @param message process status value
	 */
	public EmailException(String message) {
		super(message);
	}

	/***
	 * create a {@link EmailException} instance with message, ResponseCode process result and cause of error
	 * @param message message with description of error process
	 * @param responseCode process status value
	 * @param cause instance of {@link Throwable} with the cause of error
	 */
	public EmailException(String message, Throwable cause) {
		super(message, cause);
	}

}
