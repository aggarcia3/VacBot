// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import com.github.lalyos.jfiglet.FigletFont;

import es.uvigo.esei.sing.vacbot.dispatchers.TextMessageDispatcher;
import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.TextMessage;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerator;
import es.uvigo.esei.sing.vacbot.settings.SettingsFacade;
import es.uvigo.esei.sing.vacbot.settings.SettingsLoadException;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;

/**
 * The entry point of the VacBot application, responsible for parsing command
 * line arguments, loading settings and managing the bot lifecycle.
 *
 * @author Alejandro González García
 */
public final class VacBot {
	private static final String WELCOME_BANNER;
	private static final Logger LOGGER = LoggerFactory.getLogger(VacBot.class);

	@Parameter(
		converter = FileConverter.class, validateValueWith = SettingsFileParameterValidator.class,
		description = "settings file"
	)
	private File settingsFile = null;

	@Parameter(
		names = { "-q", "--quiet" },
		description = "If specified, suppresses printing non-logging messages to the standard output and error streams."
	)
	private boolean quietMode = false;

	@Parameter(
		names = { "-h", "--help" }, help = true,
		description = "Prints a usage help message."
	)
	private boolean showHelp = false;

	static {
		String bannerText;

		try {
			bannerText = FigletFont.convertOneLine(
				VacBot.class.getResourceAsStream("/cyberlarge.flf"), VacBot.class.getSimpleName()
			);
		} catch (final IOException exc) {
			bannerText = VacBot.class.getSimpleName() + System.lineSeparator();

			LOGGER.info(
				"An error occurred while generating the welcome banner text. Using a fallback string instead"
			);
		}

		WELCOME_BANNER = bannerText;
	}

	/**
	 * Method invoked by the JVM to start application execution.
	 *
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(final String[] args) {
		try {
			new VacBot().run(args);
		} catch (final Exception exc) {
			LOGGER.error(
				"An unhandled exception has occurred. The application will now exit", exc
			);

			System.exit(1);
		}
	}

	/**
	 * Executes the main code of the application. Currently it is responsible for
	 * parsing the command line arguments, reading the settings and managing the
	 * message dispatch loop.
	 *
	 * @param args The command line arguments.
	 */
	private void run(final String[] args) {
		// Parse the command line
		final JCommander jCommander = new JCommander(this);
		jCommander.parse(args);
		jCommander.setProgramName(VacBot.class.getSimpleName());

		if (showHelp) {
			jCommander.usage();
			return;
		}

		if (!quietMode) {
			System.out.println(WELCOME_BANNER);
		}

		final TextMessageDispatcher<? extends TextMessage, ? extends Object> messageDispatcher;
		try {
			final InputStream settingsStream;

			if (settingsFile == null) {
				if (!quietMode) {
					System.err.println("> Reading settings from the standard input stream...");
				}

				settingsStream = System.in;
			} else {
				if (!quietMode) {
					System.err.println("> Reading settings from \"" + settingsFile + "\"...");
				}

				settingsStream = new FileInputStream(settingsFile);
			}

			final VacBotSettings settings = SettingsFacade.loadFromInputStream(settingsStream);
			System.err.println("> Read settings: " + settings);

			ResponseGenerator.initialize(settings);

			messageDispatcher = settings.getMessageDispatcherFactory().getTextMessageDispatcher(settings);
		} catch (final FileNotFoundException | SettingsLoadException exc) {
			if (!quietMode) {
				System.err.println("! Couldn't load the application settings. The application can't start.");
			}

			LOGGER.error("Couldn't load the application settings", exc);

			System.exit(1);
			return;
		} catch (final FrontendCommunicationException exc) {
			if (!quietMode) {
				System.err.println("! Couldn't connect to the front-end interface. The application can't start.");
			}

			LOGGER.error("Couldn't connect to the front-end interface", exc);

			System.exit(2);
			return;
		}

		Runtime.getRuntime().addShutdownHook(new Thread("Dispatch stop thread") {
			@Override
			public void run() {
				if (!quietMode) {
					System.err.println("> Stopping...");
				}

				messageDispatcher.stop();
			}
		});

		// Hint the GC to kick in to take initialization trash out
		System.gc();

		// Start the message dispatch loop. This won't return
		System.err.println("> Starting dispatch of incoming text messages");
		messageDispatcher.dispatchUntilInterrupted();
	}

	/**
	 * Validates the settings file command line parameter.
	 *
	 * @author Alejandro González García
	 */
	private static final class SettingsFileParameterValidator implements IValueValidator<File> {
		@Override
		public void validate(final String name, final File value) throws ParameterException {
			if (value != null) {
				final Path path = value.toPath();
				if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
					throw new ParameterException("The specified file is not a regular file, or is not readable");
				}
			}
		}
	}
}
