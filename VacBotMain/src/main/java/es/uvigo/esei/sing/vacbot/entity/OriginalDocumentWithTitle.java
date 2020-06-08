// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The original text document with a title, unprocessed.
 *
 * @author Alejandro González García
 */
@Entity
@Table(name = "text_document_with_title")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class OriginalDocumentWithTitle extends DocumentWithTitle {}
