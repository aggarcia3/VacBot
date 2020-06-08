// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.dispatchers.TextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.dispatchers.TextOnlyTextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.TextOnlyTextMessage;
import es.uvigo.esei.sing.vacbot.frontend.cli.CommandLineFrontendInterface;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Creates the command line front-end interface according to the user-provided
 * settings.
 *
 * @author Alejandro González García
 */
@XmlRootElement(name = "commandLineInterfaceFrontend")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class CommandLineInterfaceDispatcherFactory implements MessageDispatcherFactory<TextOnlyTextMessage, Void> {
	@Override
	public TextMessageDispatcher<TextOnlyTextMessage, Void> getTextMessageDispatcher(
		final VacBotSettings settings
	) throws FrontendCommunicationException {
		return new TextOnlyTextMessageDispatcher(new CommandLineFrontendInterface(), settings);
	}
}
