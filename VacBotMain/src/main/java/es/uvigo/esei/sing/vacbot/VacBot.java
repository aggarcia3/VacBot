// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.frontend.TextMessage;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramTextMessage;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;

/**
 * The entry point of the VacBot application.
 *
 * @author Alejandro González García
 */
public final class VacBot {
	public static void main(final String[] args) {
		try {
			// TODO: move XML reading to its class and package
			final Unmarshaller jaxbUnmarshaller = JAXBContext.newInstance(VacBotSettings.class).createUnmarshaller();

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

			jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {
				@Override
				public boolean handleEvent(final ValidationEvent event) {
					return false;
				}
			});

			jaxbUnmarshaller.setSchema(
				SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
					VacBot.class.getResource("/settings.xsd")
				)
			);

			final VacBotSettings settings = jaxbUnmarshaller.unmarshal(
				new StreamSource(new FileInputStream(args[0])), VacBotSettings.class
			).getValue();

			System.out.println("Read settings:");
			System.out.println(settings);
			System.out.println();

			// TODO: move this loop and make it handle different types of text messages
			// with template methods
			final FrontendInterface<? extends TextMessage> frontendInterface = settings.getFrontendInterfaceFactory()
				.getFrontendInterface();

			assert frontendInterface.getTextMessageType() == TelegramTextMessage.class;

			@SuppressWarnings("unchecked") // Safe by contract
			final FrontendInterface<TelegramTextMessage> textFrontendInterface = (FrontendInterface<TelegramTextMessage>) frontendInterface;

			while (true) {
				final TelegramTextMessage message = textFrontendInterface.awaitNextMessage();

				System.out.println("Received: " + message);

				textFrontendInterface.sendMessage(
					new TelegramTextMessage(
						"Hello!", Integer.MIN_VALUE, null, null, message.getChat(),
						null, null, null, null, null, message.getThisMessage(),
						null, null, null
					)
				);
			}
		} catch (final FileNotFoundException exc) {
			exc.printStackTrace();
		} catch (final JAXBException exc) {
			exc.printStackTrace();
		} catch (final SAXException exc) {
			exc.printStackTrace();
		} catch (final FrontendCommunicationException exc) {
			exc.printStackTrace();
		} catch (final InterruptedException exc) {
			exc.printStackTrace();
		}
	}
}
