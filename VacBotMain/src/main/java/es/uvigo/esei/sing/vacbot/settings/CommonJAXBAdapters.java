// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.Getter;
import lombok.NonNull;

/**
 * Contains JAXB adapters used by several settings of VacBot.
 *
 * @author Alejandro González García
 */
final class CommonJAXBAdapters {
	/**
	 * A XML type adapter to map {@link DirectorySetting}, a POJO representing a
	 * {@code <directory>} element, to its value.
	 *
	 * @author Alejandro González García
	 */
	static final class DirectoryAdapter extends XmlAdapter<DirectorySetting, Path> {
		@Override
		public Path unmarshal(@NonNull final DirectorySetting v) throws Exception {
			final Path path = Path.of(v.getDirectoryPathString());

			if (!Files.readAttributes(path, BasicFileAttributes.class).isDirectory()) {
				throw new IllegalArgumentException("The provided path is not a directory");
			}

			return path;
		}

		@Override
		public DirectorySetting marshal(@NonNull final Path v) throws Exception {
			return new DirectorySetting(v.toAbsolutePath().toString());
		}
	}

	/**
	 * A helper class to map a directory path string value wrapped in a element to a
	 * string.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "directory")
	private static final class DirectorySetting {
		@XmlValue @Getter
		private final String directoryPathString;

		private DirectorySetting() {
			this.directoryPathString = null;
		}

		private DirectorySetting(@NonNull final String directoryPathString) {
			this.directoryPathString = directoryPathString;
		}
	}
}
