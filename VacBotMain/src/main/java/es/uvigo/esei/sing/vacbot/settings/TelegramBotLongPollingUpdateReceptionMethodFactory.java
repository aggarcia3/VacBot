// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramBotFrontendInterface;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * A update reception method for the Telegram bot front-end interface based on
 * HTTPS long polling.
 *
 * @author Alejandro González García
 */
@XmlRootElement(name = "longPollingUpdate")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class TelegramBotLongPollingUpdateReceptionMethodFactory implements TelegramBotUpdateReceptionMethodFactory {
	@Override
	public TelegramBotFrontendInterface getFrontendInterface(@NonNull final TelegramBotMessageDispatcherFactory factory) throws FrontendCommunicationException {
		return new TelegramBotFrontendInterface(this, factory);
	}
}
