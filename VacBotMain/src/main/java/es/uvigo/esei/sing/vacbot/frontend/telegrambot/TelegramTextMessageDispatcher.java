// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend.telegrambot;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import es.uvigo.esei.sing.vacbot.dispatchers.TextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerationException;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerator;
import es.uvigo.esei.sing.vacbot.settings.MessageDispatcherFactory;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;

/**
 * A text message dispatcher that handles dispatching Telegram text messages.
 * <p>
 * Even though this class defines a public constructor, its instances are meant
 * to be obtained from a {@link MessageDispatcherFactory}.
 * </p>
 *
 * @author Alejandro González García
 */
public final class TelegramTextMessageDispatcher extends TextMessageDispatcher<TelegramTextMessage, Chat> {
	private static final String[] FORWARDED_CANNED_RESPONSES = {
		"I appreciate you forward messages to me, but I won't engage in what might be chain messages.",
		"Why are you forwarding me a message? Nevermind...",
		"Uh... No, thanks, I'm very cautious about possible chain messages.",
		"Okay, but I don't want to deal with forwarded messages.",
		"Tell me something not never seen before in a chat, please 🙂"
	};

	/**
	 * Creates a new Telegram text message dispatcher for the specified operation
	 * settings.
	 *
	 * @param frontendInterface The front-end interface that this dispatcher should
	 *                          use.
	 * @param settings          The settings that define how the dispatcher should
	 *                          work.
	 * @throws IllegalArgumentException If a parameter is {@code null}.
	 */
	public TelegramTextMessageDispatcher(final FrontendInterface<TelegramTextMessage, Chat> frontendInterface, final VacBotSettings settings) {
		super(frontendInterface, settings);
	}

	@Override
	protected TelegramTextMessage computeResponse(final TelegramTextMessage message) throws ResponseGenerationException {
		final Random prng = ThreadLocalRandom.current();
		String messageText = message.getText();
		TelegramTextMessage responseMessage = null;
		String responseText = null;
		List<MessageEntity> messageEntities = message.getEntities(); // May be null if no entities
		final Chat chat = message.getChat();

		// Only bother with canned responses and deleting mentions and commands if the text is not empty
		if (!messageText.isBlank()) {
			if (
				message.getForwardedDate() != null || message.getForwardedFromUser() != null ||
				message.getForwardedSenderName() != null
			) {
				responseText = FORWARDED_CANNED_RESPONSES[prng.nextInt(FORWARDED_CANNED_RESPONSES.length)];
			} else if (messageEntities != null && !messageEntities.isEmpty()) {
				for (final MessageEntity messageEntity : messageEntities) {
					messageText = MessageEntityType.getType(messageEntity)
						.adjustEntityInText(messageText, messageEntity);
				}
			}
		}

		// Delegate to the generator if a canned response is not applicable
		if (responseText == null) {
			if (ResponseGenerator.hasResponseTo(messageText)) {
				notifyForthcomingResponse(chat);

				responseText = ResponseGenerator.generateResponseTo(messageText, settings);
			}
		}

		if (responseText != null) {
			responseMessage = new TelegramTextMessage(
				responseText,
				Integer.MIN_VALUE, null, null, chat,
				null, null, null, null, null, message.getThisMessage(),
				null, null, null
			);
		}

		return responseMessage;
	}
}
