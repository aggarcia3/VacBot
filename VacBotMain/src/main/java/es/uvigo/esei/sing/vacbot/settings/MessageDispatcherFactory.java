// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.dispatchers.TextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.TextMessage;

/**
 * A factory responsible for instantiating the text message dispatcher, that
 * will retrieve and send messages to a front-end interface.
 *
 * @author Alejandro González García
 * @param <T> The concrete type of text messages that the text dispatcher works
 *            with.
 * @param <U> The concrete type of the forthcoming message notification data
 *            that the dispatcher works with.
 */
public interface MessageDispatcherFactory<T extends TextMessage, U> {
	/**
	 * Returns a new text message dispatcher to be used with VacBot, associated to
	 * the configured front-end interface.
	 *
	 * @param settings The application settings.
	 * @return A text message dispatcher that dispatches messages to and from the
	 *         relevant front-end interface. The returned value never is
	 *         {@code null}.
	 * @throws FrontendCommunicationException If some unrecoverable error occurs
	 *                                        while communicating with the front-end
	 *                                        during the operation.
	 * @throws IllegalArgumentException       If the parameter is {@code null}.
	 */
	public TextMessageDispatcher<T, U> getTextMessageDispatcher(final VacBotSettings settings) throws FrontendCommunicationException;
}
