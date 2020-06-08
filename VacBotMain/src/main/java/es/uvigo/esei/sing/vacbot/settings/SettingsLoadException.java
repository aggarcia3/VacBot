// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

/**
 * Exception thrown to indicate an error while loading application settings.
 *
 * @author Alejandro González García
 */
public final class SettingsLoadException extends Exception {
	/**
	 * Neutralizes JVM implementation differences during serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a settings load exception with the provided detail message.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 */
	SettingsLoadException(final String message) {
		super(message);
	}

	/**
	 * Creates a settings load exception with the provided detail message and cause.
	 *
	 * @param message The detail message for the exception. It can be {@code null}.
	 * @param cause   The throwable that caused this exception. It can be
	 *                {@code null}.
	 */
	SettingsLoadException(final String message, final Throwable cause) {
        super(message, cause);
    }

	/**
	 * Creates a settings load exception with the provided cause.
	 *
	 * @param cause The throwable that caused this exception. It can be
	 *              {@code null}.
	 */
	SettingsLoadException(final Throwable cause) {
        super(cause);
    }
}
