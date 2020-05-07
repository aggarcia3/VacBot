// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.frontend.TextMessage;

/**
 * A factory responsible for instantiating the front-end interface to use with
 * VacBot, according to the user-provided settings.
 *
 * @author Alejandro González García
 * @param <T> The concrete type of text messages that the front-end interface
 *            works with.
 */
@FunctionalInterface
public interface FrontendInterfaceFactory<T extends TextMessage> {
	/**
	 * Returns a new front-end ready to be used with VacBot, fully initialized.
	 *
	 * @return The described front-end. The returned value never is {@code null}.
	 * @throws FrontendCommunicationException If some unrecoverable error occurs
	 *                                        while communicating with the front-end
	 *                                        during the operation.
	 */
	public FrontendInterface<T> getFrontendInterface() throws FrontendCommunicationException;
}
