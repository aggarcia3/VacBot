// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend;

import java.io.Closeable;

/**
 * The contract that any front-end interface available to VacBot must implement.
 * <p>
 * A front-end interface manages communication with a possibly remote front-end
 * layer, which has the responsibilities of retrieving messages for the bot to
 * process and sending responses to the end user(s).
 * </p>
 * <p>
 * The implementations of this interface must be thread-safe.
 * </p>
 *
 * @author Alejandro González García
 * @param <T> The concrete type of text messages that the front-end interface
 *            works with.
 */
public interface FrontendInterface<T extends TextMessage> extends Closeable {
	/**
	 * Retrieves the next message that the application should compute a response to.
	 * <p>
	 * If there are no available messages for the bot to process, this method blocks
	 * the current thread, waiting for a message to arrive, and then returns it.
	 * Otherwise, if there are pending messages, the bot returns them in sequence,
	 * one per call to this method.
	 * </p>
	 *
	 * @return The described message. It never is {@code null}.
	 * @throws FrontendCommunicationException If the front-end reported an
	 *                                        unrecoverable error while performing
	 *                                        the operation.
	 * @throws InterruptedException           If the current thread was interrupted
	 *                                        while waiting for a message.
	 */
	public T awaitNextMessage() throws FrontendCommunicationException, InterruptedException;

	/**
	 * Checks whether there are pending messages to compute a response to. In other
	 * words, checks whether a call to {@link #awaitNextMessage()} would not block
	 * because there are messages to process.
	 *
	 * @return True if and only if there are pending messages to process, false in
	 *         other case.
	 * @throws FrontendCommunicationException If the front-end reported an
	 *                                        unrecoverable error while performing
	 *                                        the operation.
	 */
	public boolean hasPendingMessages() throws FrontendCommunicationException;

	/**
	 * Sends a text message to the front-end interface represented by this object,
	 * so it is visible to the end-user(s).
	 *
	 * @param message The message to send.
	 * @throws FrontendCommunicationException If the front-end reported an
	 *                                        unrecoverable error while performing
	 *                                        the operation.
	 * @throws IllegalArgumentException       If {@code message} is {@code null}.
	 */
	public void sendMessage(final T message) throws FrontendCommunicationException;

	/**
	 * Returns the concrete type of text messages that this front-end interface
	 * works with. This method is mainly useful for downcasting in a type-safe
	 * manner.
	 *
	 * @return The specified type.
	 */
	public Class<T> getTextMessageType();
}
