// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents the common contract to all documents.
 * <p>
 * Non-abstract subclasses of this class must define a protected no-argument
 * constructor for JPA use only.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is not thread-safe.
 */
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Document {
	@NonNull @Getter @Id
	private Integer id;
	@NonNull @Column(nullable = false) @Getter
	private String text;

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("- ");

		stringBuilder.append(getClass().getSimpleName())
			.append('\n').append("ID: ").append(id)
			.append('\n').append("Text: ").append(text);

		return stringBuilder.toString();
	}
}