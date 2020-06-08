// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend;

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
 * @param <U> The forthcoming notification data type, to be used with
 *            {@link #notifyForthcomingResponse(U)}.
 */
public interface FrontendInterface<T extends TextMessage, U> extends AutoCloseable {
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
	 * Checks whether the specified message is meant to elicit a response from the
	 * bot, using a front-end specific algorithm.
	 *
	 * @param message The message to check.
	 * @return True if and only if the message is for the bot, false otherwise.
	 * @throws FrontendCommunicationException If the front-end reported an
	 *                                        unrecoverable error while performing
	 *                                        the operation.
	 * @throws IllegalArgumentException       If {@code message} is {@code null}.
	 */
	public boolean isMessageForBot(final T message) throws FrontendCommunicationException;

	/**
	 * Sends a notification to the end-user about a forthcoming response from the
	 * bot.
	 * <p>
	 * This notification is meant to improve user feedback, managing its
	 * expectations in the case that the response takes some time to compute. It
	 * must not be used to send information necessary for the user to interact with
	 * the bot.
	 * </p>
	 *
	 * @param notificationData A front-end interface specific object which contains
	 *                         the needed data or logic to generate the
	 *                         notification.
	 * @throws FrontendCommunicationException If the front-end reported an
	 *                                        unrecoverable error while performing
	 *                                        the operation.
	 * @throws IllegalArgumentException       If {@code notificationData} doesn't
	 *                                        satisfy a front-end specific predicate
	 *                                        (like, for instance, not being
	 *                                        {@code null}).
	 * @implNote The default implementation of this method does nothing.
	 */
	public default void notifyForthcomingResponse(final U notificationData) throws FrontendCommunicationException {
		// Do nothing
	}
}
