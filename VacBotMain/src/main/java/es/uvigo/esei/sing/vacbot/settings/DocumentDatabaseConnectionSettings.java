// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

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
@ToString
public final class DocumentDatabaseConnectionSettings {
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
	@XmlElement(name = "username", required = true)
	private final String connectionUsername = null;

	/**
	 * The connection password. This element might be missing, and therefore this
	 * field be {@code null}, if no password is needed.
	 */
	@Getter
	@XmlElement(name = "password", required = true)
	private final String connectionPassword = null;

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
}
