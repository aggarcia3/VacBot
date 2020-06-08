// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.dispatchers;

import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.frontend.TextOnlyTextMessage;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerationException;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerator;
import es.uvigo.esei.sing.vacbot.settings.MessageDispatcherFactory;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;

/**
 * A text message dispatcher that handles dispatching text only messages.
 * <p>
 * Even though this class defines a public constructor, its instances are
 * meant to be obtained from a {@link MessageDispatcherFactory}.
 * </p>
 *
 * @author Alejandro González García
 */
public final class TextOnlyTextMessageDispatcher extends TextMessageDispatcher<TextOnlyTextMessage, Void> {
	/**
	 * Creates a new text only text message dispatcher for the specified operation
	 * settings.
	 *
	 * @param frontendInterface The front-end interface that this dispatcher should
	 *                          use.
	 * @param settings          The settings that define how the dispatcher should
	 *                          work.
	 * @throws IllegalArgumentException If a parameter is {@code null}.
	 */
	public TextOnlyTextMessageDispatcher(final FrontendInterface<TextOnlyTextMessage, Void> frontendInterface, final VacBotSettings settings) {
		super(frontendInterface, settings);
	}

	@Override
	protected TextOnlyTextMessage computeResponse(final TextOnlyTextMessage message) throws ResponseGenerationException {
		final String messageText = message.getText();
		final String responseText;

		if (ResponseGenerator.hasResponseTo(messageText)) {
			notifyForthcomingResponse(null);
		}

		responseText = ResponseGenerator.generateResponseTo(messageText, settings);
		return responseText != null ? new TextOnlyTextMessage(responseText) : null;
	}
}
