// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.responsegen;

/**
 * Exception thrown to indicate unrecoverable errors when generating a response
 * to a user message.
 *
 * @author Alejandro González García
 */
public final class ResponseGenerationException extends Exception {
	/**
	 * Neutralizes JVM implementation differences during serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a response generation exception with the provided detail message.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 */
	public ResponseGenerationException(final String message) {
		super(message);
	}

	/**
	 * Creates a response generation exception with the provided detail message and
	 * cause.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 * @param cause   The throwable that caused this exception. It can be
	 *                {@code null}.
	 */
	public ResponseGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }

	/**
	 * Creates a response generation exception with the provided cause.
	 *
	 * @param cause The throwable that caused this exception. It can be
	 *              {@code null}.
	 */
	public ResponseGenerationException(final Throwable cause) {
        super(cause);
    }
}
