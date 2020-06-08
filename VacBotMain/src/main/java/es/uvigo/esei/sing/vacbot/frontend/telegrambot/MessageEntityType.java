// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend.telegrambot;

import java.util.Locale;

import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import lombok.NonNull;

/**
 * Represents a Telegram message entity type, which contains additional logic to
 * handle some entity types operations.
 *
 * @author Alejandro González García
 */
public enum MessageEntityType {
	/**
	 * A mention to a user profile (i.e. @username).
	 */
	MENTION {
		@Override
		public String adjustEntityInText(final String messageText, final MessageEntity entity) {
			super.adjustEntityInText(messageText, entity);

			final int entityOffset = entity.getOffset();
			final int entityLength = entity.getLength();
			final int originalMessageLength = messageText.length();
			final StringBuilder stringBuilder = new StringBuilder(originalMessageLength - entityLength);

			// Create a new string without the message entity
			stringBuilder.append(
				messageText.substring(
					0,
					// Extra sanity checks
					Math.min(Math.max(entityOffset, 0), originalMessageLength)
				)
			);
			stringBuilder.append(
				messageText.substring(
					Math.max(Math.min(entityOffset + entityLength, originalMessageLength), 0),
					originalMessageLength
				)
			);

			return stringBuilder.toString();
		}
	},
	/**
	 * A hashtag (i.e. #hashtag).
	 */
	HASHTAG,
	/**
	 * A cashtag (i.e. $USD).
	 */
	CASHTAG,
	/**
	 * A command for a bot (i.e. /start@jobs_bot).
	 */
	BOT_COMMAND {
		@Override
		public String adjustEntityInText(final String messageText, final MessageEntity entity) {
			super.adjustEntityInText(messageText, entity);

			if (entity.getText().startsWith("/start")) {
				// Discard anything else from the text
				return "/start";
			} else {
				// Remove the command
				return MENTION.adjustEntityInText(messageText, entity);
			}
		}
	},
	/**
	 * A URL (i.e. https://telegram.org).
	 */
	URL,
	/**
	 * An email address (i.e. do-not-reply@telegram.org).
	 */
	EMAIL,
	/**
	 * A phone number (i.e. +1-212-555-0123).
	 */
	PHONE_NUMBER,
	/**
	 * <span style="font-weight: bold;">Bold text</span>.
	 */
	BOLD,
	/**
	 * <span style="font-style: italic;">Italic text</span>.
	 */
	ITALIC,
	/**
	 * <span style="text-decoration: underline;">Underlined text</span>.
	 */
	UNDERLINE,
	/**
	 * <span style="text-decoration: line-through;">Strikethrough text</span>.
	 */
	STRIKETHROUGH,
	/**
	 * <span style="font-family: monospace;">A monospaced single line of
	 * text</span>.
	 */
	CODE,
	/**
	 * <span style="font-family: monospace;">Monospaced text (it can have several
	 * lines)</span>.
	 */
	PRE,
	/**
	 * A text link.
	 */
	TEXT_LINK,
	/**
	 * A textual mention, for users for which the user profile was not specified.
	 */
	TEXT_MENTION {
		@Override
		public String adjustEntityInText(final String messageText, final MessageEntity entity) {
			// Same treatment as mentions
			return MENTION.adjustEntityInText(messageText, entity);
		}
	};

	/**
	 * Returns the {@link MessageEntityType} for a given message entity.
	 *
	 * @param entity The entity to get its type.
	 * @return The associated message entity type.
	 * @throws IllegalArgumentException If {@code entity} is {@code null} or a
	 *                                  suitable message entity type couldn't be
	 *                                  found.
	 */
	public static MessageEntityType getType(@NonNull final MessageEntity entity) {
		return MessageEntityType.valueOf(entity.getType().toUpperCase(Locale.ROOT));
	}

	/**
	 * Modifies the specified message text, so that the specified entity presence in
	 * it is normalized to a more amenable form for further operations.
	 *
	 * @param messageText The text of the message.
	 * @param entity      The entity whose presence will be considered.
	 * @return The processed message text.
	 * @throws IllegalArgumentException If any argument is {@code null}, or the
	 *                                  message entity is not adequate for this
	 *                                  constant.
	 * @implNote The default implementation of this method checks the correctness of
	 *           its parameters and returns the original message text.
	 */
	public String adjustEntityInText(@NonNull final String messageText, @NonNull final MessageEntity entity) {
		if (getType(entity) != this) {
			throw new IllegalArgumentException("The provided entity type doesn't match the used enum constant");
		}

		// Guard against unboxing throwing NPE and null text
		if (entity.getOffset() == null || entity.getLength() == null || entity.getText() == null) {
			throw new IllegalArgumentException("A message entity field has an unexpected null value");
		}

		return messageText;
	}
}
