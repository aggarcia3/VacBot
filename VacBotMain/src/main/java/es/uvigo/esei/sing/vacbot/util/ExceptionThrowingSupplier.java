// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.util;

import java.util.function.Supplier;

/**
 * A {@link Supplier} which can throw an exception while supplying a result.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of result.
 * @param <E> The type of exception that can be thrown while supplying a result.
 */
@FunctionalInterface
public interface ExceptionThrowingSupplier<T, E extends Exception> extends Supplier<T> {
	/**
	 * Gets a result, throwing an exception if some error occurred during the
	 * operation.
	 *
	 * @return The result.
	 * @throws E If an error occurs during the operation.
	 */
	public T throwingGet() throws E;

	/**
	 * {@inheritDoc}
	 *
	 * @implNote The default implementation of this method calls
	 *           {@link #throwingGet()} and wraps any exception in a
	 *           {@link RuntimeException}. The use of this method is not recommended
	 *           unless the client is prepared to handle unchecked exceptions.
	 */
	public default T get() {
		try {
			return throwingGet();
		} catch (final Exception exc) {
			throw new RuntimeException(exc);
		}
	}
}
