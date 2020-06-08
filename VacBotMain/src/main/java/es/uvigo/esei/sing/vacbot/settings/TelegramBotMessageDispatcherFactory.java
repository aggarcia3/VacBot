// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import org.telegram.telegrambots.meta.ApiConstants;
import org.telegram.telegrambots.meta.api.objects.Chat;

import es.uvigo.esei.sing.vacbot.dispatchers.TextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramTextMessage;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramTextMessageDispatcher;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Creates the Telegram bot front-end interface according to the user-provided
 * settings.
 *
 * @author Alejandro González García
 */
@XmlRootElement(name = "telegramBotFrontend")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class TelegramBotMessageDispatcherFactory implements MessageDispatcherFactory<TelegramTextMessage, Chat> {
	/**
	 * The user name for the chatbot.
	 */
	@Getter @NonNull
	@XmlElement(name = "userName", required = true)
	private final String userName = null;

	/**
	 * The token to use to authenticate to the Telegram API.
	 */
	@Getter @NonNull
	@XmlElement(name = "token", required = true)
	@XmlJavaTypeAdapter(TokenAdapter.class)
	private final String token = null;

	/**
	 * The maximum number of threads used for asynchronous methods executions. The
	 * default is 1.
	 */
	@Getter
	@XmlElement(name = "maxAsyncThreads")
	private final int maxAsyncThreads = 1;

	/**
	 * The maximum number of incoming webhook connections. The default is 40.
	 */
	@Getter
	@XmlElement(name = "maxWebhookConnections")
	private final int maxWebhookConnections = 40;

	/**
	 * The Telegram API base URL. A sane default value is automatically chosen.
	 */
	@Getter
	@XmlElement(name = "telegramBaseUrl")
	private final String baseUrl = ApiConstants.BASE_URL;

	@Getter @NonNull
	@XmlElements({
		@XmlElement(name = "longPollingUpdate", type = TelegramBotLongPollingUpdateReceptionMethodFactory.class),
		@XmlElement(name = "webhookUpdate", type = TelegramBotWebhookUpdateReceptionMethodFactory.class)
	})
	private final TelegramBotUpdateReceptionMethodFactory updateReceptionMethodFactory = null;

	@Override
	public TextMessageDispatcher<TelegramTextMessage, Chat> getTextMessageDispatcher(
		final VacBotSettings settings
	) throws FrontendCommunicationException {
		return new TelegramTextMessageDispatcher(updateReceptionMethodFactory.getFrontendInterface(this), settings);
	}

	/**
	 * A helper class to map a token string value wrapped in a element to a string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "token")
	private static final class TokenSetting {
		@XmlValue @Getter
		private final String tokenString;

		private TokenSetting() {
			this.tokenString = null;
		}

		private TokenSetting(@NonNull final String tokenString) {
			this.tokenString = tokenString;
		}
	}

	/**
	 * A XML type adapter to map {@link TokenSetting}, a POJO representing a
	 * {@code <directory>} element, to its value.
	 *
	 * @author Alejandro González García
	 */
	private static final class TokenAdapter extends XmlAdapter<TokenSetting, String> {
		@Override
		public String unmarshal(@NonNull final TokenSetting v) throws Exception {
			String actualToken = v.getTokenString();

			// Support specifying tokens in environment variables
			if (actualToken.startsWith("env:")) {
				actualToken = System.getenv(actualToken.substring(4));

				if (actualToken == null) {
					throw new IllegalArgumentException("The specified token environment variable doesn't have a value");
				}
			}

			return actualToken;
		}

		@Override
		public TokenSetting marshal(@NonNull final String v) throws Exception {
			// Beware: this marshals the token as-is, in plain form
			return new TokenSetting(v);
		}
	}
}
