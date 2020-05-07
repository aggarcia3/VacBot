// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend;

/**
 * Exception thrown to indicate unrecoverable errors when communicating with the
 * front-end interface in use, which usually mean that the requested action
 * could not be performed.
 *
 * @author Alejandro González García
 */
public final class FrontendCommunicationException extends Exception {
	/**
	 * Neutralizes JVM implementation differences during serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a front-end communication exception without a detail message.
	 */
	public FrontendCommunicationException() {
		super();
	}

	/**
	 * Creates a front-end communication exception with the provided detail message.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 */
	public FrontendCommunicationException(final String message) {
		super(message);
	}

	/**
	 * Creates a front-end communication exception with the provided detail message
	 * and cause.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 * @param cause   The throwable that caused this exception. It can be
	 *                {@code null}.
	 */
    public FrontendCommunicationException(final String message, final Throwable cause) {
        super(message, cause);
    }

	/**
	 * Creates a front-end communication exception with the provided cause.
	 *
	 * @param cause The throwable that caused this exception. It can be
	 *              {@code null}.
	 */
    public FrontendCommunicationException(final Throwable cause) {
        super(cause);
    }
}
