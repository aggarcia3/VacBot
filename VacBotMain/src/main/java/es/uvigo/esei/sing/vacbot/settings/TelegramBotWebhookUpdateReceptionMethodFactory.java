// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.telegrambot.TelegramBotFrontendInterface;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * A update reception method for the Telegram bot front-end interface based on
 * webhooks, which are HTTPS calls made by the Telegram servers to the specified
 * URL.
 *
 * @author Alejandro González García
 * @see <a href="https://core.telegram.org/bots/api#setwebhook">Telegram API
 *      documentation</a>, <a href=
 *      "https://github.com/rubenlagus/TelegramBotsExample/blob/1b558e6f87645d7ab0d66fa966bae5dbd8b2998a/src/main/java/org/telegram/Main.java">Telegrambots
 *      examples</a>
 */
@XmlRootElement(name = "webhookUpdate")
@ToString
public final class TelegramBotWebhookUpdateReceptionMethodFactory implements TelegramBotUpdateReceptionMethodFactory {
	/**
	 * The URL where Telegram servers will send HTTPS POST requests to.
	 */
	@Getter @NonNull
	@XmlElement(name = "externalUrl", required = true)
	private final String externalUrl = null;

	/**
	 * The URL the bot will be listening on for Telegram updates. This URL can
	 * include any port number, but Telegram currently only supports 443, 80, 88 and
	 * 8443.
	 */
	@Getter @NonNull
	@XmlElement(name = "internalUrl", required = true)
	private final String internalUrl = null;

	/**
	 * The path within the internal URL the bot will be listening on for Telegram
	 * updates. Telegram recommends that this path contains the bot token, so only
	 * their servers can plausibly access it.
	 */
	@Getter @NonNull
	@XmlElement(name = "botPath", required = true)
	private final String botPath = null;

	/**
	 * The path to the Java certificate store to use.
	 */
	@Getter
	@XmlElement(name = "certificateStorePath")
	@XmlJavaTypeAdapter(CertificateStorePathAdapter.class)
	private final Path certificateStorePath = null;

	/**
	 * The password of the Java certificate store to use.
	 */
	@Getter
	@XmlElement(name = "certificateStorePassword")
	@XmlJavaTypeAdapter(CertificateStorePasswordAdapter.class)
	private final String certificateStorePassword = null;

	/**
	 * The public key to send to the Telegram servers when registering the webhooks,
	 * so the TLS handshake made by Telegram servers can trust the public
	 * certificate used.
	 */
	@Getter
	@XmlElement(name = "publicKeyPath")
	@XmlJavaTypeAdapter(PublicKeyPathAdapter.class)
	private final Path publicKeyPath = null;

	/**
	 * Restricts instantiation of this class to JAXB.
	 */
	private TelegramBotWebhookUpdateReceptionMethodFactory() {}

	@Override
	public TelegramBotFrontendInterface getFrontendInterface(@NonNull final TelegramBotFrontendInterfaceFactory factory) throws FrontendCommunicationException {
		return new TelegramBotFrontendInterface(this, factory);
	}

	/**
	 * A helper class to map a certificate store path string value wrapped in a
	 * element to a string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "certificateStorePath")
	private static final class CertificateStorePathSetting {
		@XmlValue @Getter
		private final String certificateStorePathString;

		private CertificateStorePathSetting() {
			this.certificateStorePathString = null;
		}

		private CertificateStorePathSetting(@NonNull final String certificateStorePathString) {
			this.certificateStorePathString = certificateStorePathString;
		}
	}

	/**
	 * A XML type adapter to map {@link CertificateStorePathSetting}, a POJO representing
	 * a {@code <certificateStorePath>} element, to its value.
	 *
	 * @author Alejandro González García
	 */
	private static final class CertificateStorePathAdapter extends XmlAdapter<CertificateStorePathSetting, Path> {
		@Override
		public Path unmarshal(@NonNull final CertificateStorePathSetting v) throws Exception {
			final Path path = Path.of(v.getCertificateStorePathString());

			if (!Files.readAttributes(path, BasicFileAttributes.class).isRegularFile()) {
				throw new IllegalArgumentException("The provided path is not a file");
			}

			return path;
		}

		@Override
		public CertificateStorePathSetting marshal(@NonNull final Path v) throws Exception {
			return new CertificateStorePathSetting(v.toAbsolutePath().toString());
		}
	}

	/**
	 * A helper class to map a certificate store password string value wrapped in a
	 * element to a string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "certificateStorePassword")
	private static final class CertificateStorePasswordSetting {
		@XmlValue @Getter
		private final String passwordString;

		private CertificateStorePasswordSetting() {
			this.passwordString = null;
		}

		private CertificateStorePasswordSetting(@NonNull final String passwordString) {
			this.passwordString = passwordString;
		}
	}

	/**
	 * A XML type adapter to map {@link CertificateStorePasswordSetting}, a POJO
	 * representing a {@code <certificateStorePassword>} element, to its value.
	 *
	 * @author Alejandro González García
	 */
	private static final class CertificateStorePasswordAdapter extends XmlAdapter<CertificateStorePasswordSetting, String> {
		@Override
		public String unmarshal(@NonNull final CertificateStorePasswordSetting v) throws Exception {
			String actualPassword = v.getPasswordString();

			// Support specifying passwords in environment variables
			if (actualPassword.startsWith("env:")) {
				actualPassword = System.getenv(actualPassword.substring(4));

				if (actualPassword == null) {
					throw new IllegalArgumentException("The specified password environment variable doesn't have a value");
				}
			}

			return actualPassword;
		}

		@Override
		public CertificateStorePasswordSetting marshal(@NonNull final String v) throws Exception {
			// Beware: this marshals the password as-is, in plain form
			return new CertificateStorePasswordSetting(v);
		}
	}

	/**
	 * A helper class to map a public key path string value wrapped in a element to
	 * a string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "publicKeyPath")
	private static final class PublicKeyPathSetting {
		@XmlValue @Getter
		private final String publicKeyPathString;

		private PublicKeyPathSetting() {
			this.publicKeyPathString = null;
		}

		private PublicKeyPathSetting(@NonNull final String publicKeyPathString) {
			this.publicKeyPathString = publicKeyPathString;
		}
	}

	/**
	 * A XML type adapter to map {@link PublicKeyPathSetting}, a POJO representing
	 * a {@code <publicKeyPath>} element, to its value.
	 *
	 * @author Alejandro González García
	 */
	private static final class PublicKeyPathAdapter extends XmlAdapter<PublicKeyPathSetting, Path> {
		@Override
		public Path unmarshal(@NonNull final PublicKeyPathSetting v) throws Exception {
			final Path path = Path.of(v.getPublicKeyPathString());

			if (!Files.readAttributes(path, BasicFileAttributes.class).isRegularFile()) {
				throw new IllegalArgumentException("The provided path is not a file");
			}

			return path;
		}

		@Override
		public PublicKeyPathSetting marshal(@NonNull final Path v) throws Exception {
			return new PublicKeyPathSetting(v.toAbsolutePath().toString());
		}
	}
}
