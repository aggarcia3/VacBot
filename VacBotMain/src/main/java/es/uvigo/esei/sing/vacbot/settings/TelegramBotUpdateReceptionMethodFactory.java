// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramBotFrontendInterface;

/**
 * The contract that every Telegram bot update reception method factory must
 * implement.
 *
 * @author Alejandro González García
 */
@FunctionalInterface
public interface TelegramBotUpdateReceptionMethodFactory {
	/**
	 * Instantiates a new Telegram bot front-end interface, according to the
	 * provided user settings and the concrete update reception method represented
	 * by this factory.
	 *
	 * @param factory The factory for which the Telegram bot front-end interface
	 *                will be created.
	 * @return The described front-end interface.
	 * @throws IllegalArgumentException       If {@code factory} is {@code null}.
	 * @throws FrontendCommunicationException If some unrecoverable error occurs
	 *                                        while communicating with the front-end
	 *                                        during the operation.
	 */
	public TelegramBotFrontendInterface getFrontendInterface(final TelegramBotFrontendInterfaceFactory factory) throws FrontendCommunicationException;
}
