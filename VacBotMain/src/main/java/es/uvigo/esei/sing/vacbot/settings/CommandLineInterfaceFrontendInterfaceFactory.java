// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.frontend.TextOnlyTextMessage;
import es.uvigo.esei.sing.vacbot.frontend.cli.CommandLineFrontendInterface;
import lombok.ToString;

/**
 * Creates the command line front-end interface according to the user-provided
 * settings.
 *
 * @author Alejandro González García
 */
@ToString
public final class CommandLineInterfaceFrontendInterfaceFactory implements FrontendInterfaceFactory<TextOnlyTextMessage> {
	@Override
	public CommandLineFrontendInterface getFrontendInterface() {
		return new CommandLineFrontendInterface();
	}
}
