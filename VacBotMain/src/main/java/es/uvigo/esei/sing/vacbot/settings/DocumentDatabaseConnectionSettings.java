// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
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
 * Contains all the settings related to the relational document database to be
 * used with VacBot. The relational document database is assumed to be in the
 * same format as TextProc.
 *
 * @author Alejandro González García
 * @see VacBotSettings
 */
@XmlRootElement(name = "documentDatabaseConnection")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = { "entityManagerFactory", "connectionPassword" } )
public final class DocumentDatabaseConnectionSettings implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDatabaseConnectionSettings.class);

	/**
	 * The class of the JDBC driver to use.
	 */
	@Getter @NonNull
	@XmlElement(name = "driver", required = true)
	@XmlJavaTypeAdapter(JDBCDriverClassAdapter.class)
	private final Class<?> jdbcDriverClass = null;

	/**
	 * The connection URL.
	 */
	@Getter @NonNull
	@XmlElement(name = "url", required = true)
	private final String connectionUrl = null;

	/**
	 * The connection username. This element might be missing, and therefore this
	 * field be {@code null}, if no username is needed.
	 */
	@Getter
	@XmlElement(name = "username")
	private final String connectionUsername = "";

	/**
	 * The connection password. This element might be missing, and therefore this
	 * field be {@code null}, if no password is needed.
	 */
	@Getter
	@XmlElement(name = "password")
	private final String connectionPassword = "";

	/**
	 * The factory of JPA entity managers that will be used to access entities in
	 * the relational database.
	 */
	private EntityManagerFactory entityManagerFactory = null;

	/**
	 * Opens a connection to the database configured by these settings if necessary
	 * and returns a new, possibly non thread-safe JPA {@link EntityManager} for it,
	 * which the caller should close as soon as it finishes its work. If the
	 * connection was already opened, it will not be opened again, but a new
	 * {@link EntityManager} instance will be created nevertheless.
	 * <p>
	 * This method is not thread-safe when called for a first time: the connection
	 * isn't updated with happens-before semantics. However, after this first call,
	 * it is safe to execute it from different threads concurrently.
	 * </p>
	 *
	 * @return The described {@link EntityManager} object.
	 * @throws PersistenceException If any error occurs during the instantiation of
	 *                              the entity manager factory.
	 */
	public EntityManager openEntityManager() {
		if (entityManagerFactory == null) {
			LOGGER.info("Creating entity manager factory for database access...");

			entityManagerFactory = Persistence.createEntityManagerFactory(
				"VacBotPersistence", Map.of(
					// Standard properties as of JPA 2.2. See page 371 of the specification
					"javax.persistence.jdbc.driver", jdbcDriverClass.getCanonicalName(),
					"javax.persistence.jdbc.url", connectionUrl,
					"javax.persistence.jdbc.user", connectionUsername,
					"javax.persistence.jdbc.password", connectionPassword
				)
			);

			LOGGER.info(
				"Entity manager factory created, connection to the document database established"
			);
		}

		return entityManagerFactory.createEntityManager();
	}

	/**
	 * Opens the entity manager factory just after the database connection settings
	 * are unmarshalled from the configuration file.
	 * <p>
	 * Any exception thrown by this method will abort the unmarshalling process as
	 * if a parse error occurred.
	 * </p>
	 *
	 * @param unmarshaller The unmarshaller that is unmarshalling this class.
	 * @param parent       The parent object. It can be {@code null}.
	 */
	@SuppressWarnings("unused") // Called by JAXB
	private void afterUnmarshal(final Unmarshaller unmarshaller, final Object parent) {
		openEntityManager();
	}

	/**
	 * A helper class to map a JDBC driver class string value wrapped in a element
	 * to a string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "driver")
	private static final class JDBCDriverSetting {
		@XmlValue @Getter
		private final String jdbcDriverClassName;

		private JDBCDriverSetting() {
			this.jdbcDriverClassName = null;
		}

		private JDBCDriverSetting(@NonNull final String jdbcDriverClassName) {
			this.jdbcDriverClassName = jdbcDriverClassName;
		}
	}

	/**
	 * A XML type adapter to map {@link JDBCDriverSetting} to its unwrapped value.
	 *
	 * @author Alejandro González García
	 */
	private static final class JDBCDriverClassAdapter extends XmlAdapter<JDBCDriverSetting, Class<?>> {
		@Override
		public Class<?> unmarshal(@NonNull final JDBCDriverSetting v) throws Exception {
			try {
				return Class.forName(v.getJdbcDriverClassName());
			} catch (final Error err) {
				throw new Exception(err.getMessage());
			}
		}

		@Override
		public JDBCDriverSetting marshal(@NonNull final Class<?> v) throws Exception {
			return new JDBCDriverSetting(v.getName());
		}
	}

	@Override
	public void close() throws Exception {
		if (entityManagerFactory != null) {
			entityManagerFactory.close();
		}
	}
}
