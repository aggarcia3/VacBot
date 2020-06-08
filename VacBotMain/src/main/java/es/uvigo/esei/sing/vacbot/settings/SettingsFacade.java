// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides high-level settings-related functionality to other parts of the
 * application, hiding implementation details to them.
 * <p>
 * This class is the recommended way to instantiate a {@link VacBotSettings}
 * object.
 * </p>
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // Zero-ton
public final class SettingsFacade {
	private static final JAXBContext JAXB_SETTINGS_CONTEXT;
	private static final Schema SETTINGS_SCHEMA;

	static {
		try {
			// The JAXB context is safe according to experts
			JAXB_SETTINGS_CONTEXT = JAXBContext.newInstance(VacBotSettings.class);

			final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // Limit resource usage

			// Treat warnings during unmarshalling as errors
			schemaFactory.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(final SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void error(final SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void fatalError(final SAXParseException exception) throws SAXException {
					throw exception;
				}
			});

			// The Schema is thread-safe according to the Javadoc
			SETTINGS_SCHEMA = schemaFactory.newSchema(
				SettingsFacade.class.getResource("/settings.xsd")
			);
		} catch (final SAXException | JAXBException exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}

	/**
	 * Creates a {@link VacBotSettings} object by parsing the configuration file in
	 * the specified input stream.
	 *
	 * @param stream The stream to read the settings from.
	 * @return The described object that represents the settings.
	 * @throws SettingsLoadException If {@code stream} is {@code null}, or some
	 *                               error occurred while parsing the settings from
	 *                               the stream.
	 */
	public static VacBotSettings loadFromInputStream(final InputStream stream) throws SettingsLoadException {
		if (stream == null) {
			throw new SettingsLoadException("Can't load settings from a null input stream");
		}

		try {
			final Unmarshaller jaxbUnmarshaller = JAXB_SETTINGS_CONTEXT.createUnmarshaller();

			jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {
				@Override
				public boolean handleEvent(final ValidationEvent event) {
					return false;
				}
			});

			jaxbUnmarshaller.setSchema(SETTINGS_SCHEMA);

			return jaxbUnmarshaller.unmarshal(
				new StreamSource(stream), VacBotSettings.class
			).getValue();
		} catch (final JAXBException exc) {
			throw new SettingsLoadException(exc);
		}
	}
}
