// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.responsegen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.UriBuilder;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spell.LevenshteinDistance;

import com.google.common.collect.Lists;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.DependencyParseAnnotator;
import edu.stanford.nlp.pipeline.MorphaAnnotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.ParserAnnotator;
import edu.stanford.nlp.pipeline.TokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.ud.UniversalGrammaticalRelations;
import edu.stanford.nlp.util.CoreMap;
import es.uvigo.esei.sing.vacbot.entity.Document;
import es.uvigo.esei.sing.vacbot.entity.OriginalDocument;
import es.uvigo.esei.sing.vacbot.entity.OriginalDocumentWithTitle;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import edu.stanford.nlp.ling.SentenceUtils;

/**
 * Entry point for clients interested in generating a non-canned response to a
 * natural-language text string.
 *
 * @author Alejandro González García
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResponseGenerator {
	private static final String DOCUMENT_ID_FIELD = "id";
	private static final Set<String> DOCUMENT_ID_FIELD_SET = Set.of(DOCUMENT_ID_FIELD);

	/**
	 * The non-commital responses the bot will say, in case a more appropriate
	 * response was not found.
	 */
	private static final String[] NON_COMMITAL_RESPONSES = new String[] {
		"Keep talking",
		"Keep talking.",
		"Go on.",
		"Go on",
		"Hmm?",
		"How about we talk about another topic?",
		"Hmmm... May I suggest we talk about another thing?",
		"Tell me more",
		"Tell me more.",
		"How so?",
		"Say what?",
		"Well, that's one way of putting it",
		"That's one way of putting it."
	};

	/**
	 * The responses the bot will say to a greeting.
	 */
	private static final String[] GREETING_RESPONSES = new String[] {
		"Hello!",
		"Hello",
		"Hey!",
		"Hey",
		"Let's talk!",
		"Let's talk",
		"👋",
		"Hello there!",
		"Hey there!",
		"Hey! What do you want to ask?",
		"Welcome to my chat 🙂"
	};

	/**
	 * The responses the bot will say to a goodbye.
	 */
	private static final String[] GOODBYE_RESPONSES = new String[] {
		"Goodbye",
		"Goodbye!",
		"👋",
		"See you",
		"See you!",
		"See you later",
		"See you later!",
		"Bye bye",
		"Bye now",
		"Cya!",
		"Cya",
		"Let's talk again soon!",
		"Take care",
		"Take care!",
		"Later",
		"Stay in touch",
		"Stay in touch!"
	};

	/**
	 * Informal greetings that the user may say to the bot.
	 */
	private static final Pattern GREETING_PATTERN = Pattern.compile(
		"Hi|Hey|Hello|Hiya|Greetings|Howdy|Good (morning|mornin|evening|evenin|afternoon|night|nigh)|" +
		"Wassup|Wh?at('s|s| is)? (up|goin(g|'|) on|new)|(Good|Nice) (to )?see (you|u)|Yo[!,]*$|" +
		"Yo[!,]* ( Wassup| Wh?at)|Long time no see|Salutation|I salute",
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
	);

	/**
	 * Informal goodbyes that the user may say to the bot.
	 */
	private static final Pattern GOODBYE_PATTERN = Pattern.compile(
		"Bye|Goodbye|See (u|you)|Cya!*?|Later|Farewell|" +
		"Have a (good|nice|wonderful) (day|morning|mornin|evening|evenin|afternoon|night|nigh)|So long!*$",
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
	);

	/**
	 * Precompiled pattern that matches one or more spaces.
	 */
	private static final Pattern ONE_OR_MORE_SPACES = Pattern.compile("\\s+");

	/**
	 * The pattern used to check whether a sentence is a direct question.
	 * A list of constituency tags is available at
	 * <a href="https://gist.github.com/nlothian/9240750">https://gist.github.com/nlothian/9240750</a>.
	 */
	private static final TregexPattern DIRECT_QUESTION_PATTERN = TregexPattern.compile("@SBARQ");

	/**
	 * The pattern used to match wh-tags in direct questions.
	 */
	private static final Pattern WH_TAG_PATTERN = Pattern.compile("WP|WP\\$|WDT|WRB");

	/**
	 * Maps document types in a KB URI to their corresponding JPA document classes.
	 */
	private static final Map<String, Class<? extends Document>> URI_DOCUMENT_TYPE_TO_DOCUMENT_CLASS = Map.of(
		"untitled docs", OriginalDocument.class,
		"titled docs", OriginalDocumentWithTitle.class
	);

	private static AnnotationPipeline tokenizationAndPosPipeline = null;
	private static AnnotationPipeline sentenceSplitPipeline = null;
	private static AnnotationPipeline parserPipeline = null;

	static {
		if (NON_COMMITAL_RESPONSES.length < 1) {
			throw new ExceptionInInitializerError("At least one non-commital response is needed");
		}

		if (GREETING_RESPONSES.length < 1) {
			throw new ExceptionInInitializerError("At least one greeting response is needed");
		}
	}

	/**
	 * Initializes the response generator with the provided settings.
	 * <p>
	 * This method is designed to be called before another threads which call
	 * {@link #generateResponseTo(String, VacBotSettings)} are created. If this
	 * method is called after creating those threads, visibility and race condition
	 * problems may occur.
	 * </p>
	 *
	 * @param settings The settings of the application.
	 * @throws IllegalArgumentException If {@code settings} is {@code null}.
	 */
	public static void initialize(@NonNull final VacBotSettings settings) {
		// Create and initialize the annotator properties
		final Properties parserProperties = new Properties();
		parserProperties.setProperty("parser.nthreads", "1");
		parserProperties.setProperty(
			"parser.model",
			settings.getNaturalLanguageProcessingSettings().getParserModel()
		);

		final Properties dependencyParseProperties = new Properties();
		dependencyParseProperties.setProperty("nthreads", "1");
		dependencyParseProperties.setProperty(
			"model",
			settings.getNaturalLanguageProcessingSettings().getDependencyParseModel()
		);

		// Create the pipelines
		tokenizationAndPosPipeline = new AnnotationPipeline();
		tokenizationAndPosPipeline.addAnnotator(
			new TokenizerAnnotator(
				false,
				settings.getNaturalLanguageProcessingSettings().getLanguage(),
				settings.getNaturalLanguageProcessingSettings().getTokenizerOptions()
			)
		);
		tokenizationAndPosPipeline.addAnnotator(new WordsToSentencesAnnotator(false));
		tokenizationAndPosPipeline.addAnnotator(
			new POSTaggerAnnotator(
				settings.getNaturalLanguageProcessingSettings().getPosModel(),
				false, Integer.MAX_VALUE, 1
			)
		);

		sentenceSplitPipeline = new AnnotationPipeline();
		sentenceSplitPipeline.addAnnotator(
			new TokenizerAnnotator(
				false,
				settings.getNaturalLanguageProcessingSettings().getLanguage(),
				settings.getNaturalLanguageProcessingSettings().getTokenizerOptions()
			)
		);
		sentenceSplitPipeline.addAnnotator(new WordsToSentencesAnnotator(false));

		parserPipeline = new AnnotationPipeline();
		// These three are required
		parserPipeline.addAnnotator(
			new TokenizerAnnotator(
				false,
				settings.getNaturalLanguageProcessingSettings().getLanguage(),
				settings.getNaturalLanguageProcessingSettings().getTokenizerOptions()
			)
		);
		parserPipeline.addAnnotator(new WordsToSentencesAnnotator(false));
		parserPipeline.addAnnotator(
			// I'm not really sure I can reuse the previous annotator due to
			// concurrency not being so well documented, so err in the safe side
			// and create it again
			new POSTaggerAnnotator(
				settings.getNaturalLanguageProcessingSettings().getPosModel(),
				false, Integer.MAX_VALUE, 1
			)
		);
		parserPipeline.addAnnotator(new MorphaAnnotator(false));
		parserPipeline.addAnnotator(new ParserAnnotator("parser", parserProperties));
		parserPipeline.addAnnotator(new DependencyParseAnnotator(dependencyParseProperties));
	}

	/**
	 * Generates a response to the specified utterance. The execution of this method
	 * may be costly in terms of time and/or memory.
	 * <p>
	 * Before generating responses, the {@link #initialize(VacBotSettings)} method
	 * need to be called.
	 * </p>
	 * <p>
	 * This method is safe to be executed by concurrent threads.
	 * </p>
	 *
	 * @param text     The utterance text to respond to. It need not be
	 *                 {@code null}.
	 * @param settings The settings of the bot, which may affect response
	 *                 generation. It need not be {@code null}.
	 * @return The response text. If no response is generated, {@code null} will be
	 *         returned.
	 * @throws ResponseGenerationException If the response couldn't be generated
	 *                                     because an error occurred.
	 */
	public static String generateResponseTo(
		final String text, final VacBotSettings settings
	) throws ResponseGenerationException {
		String response;

		if (text == null) {
			throw new ResponseGenerationException("The utterance text can't be null");
		}

		if (settings == null) {
			throw new ResponseGenerationException("The bot settings can't be null");
		}

		if (tokenizationAndPosPipeline == null) {
			throw new ResponseGenerationException("The response generator was not initialized properly");
		}

		if (hasResponseTo(text)) {
			response = analyzeUtteranceAndGenerateResponse(text, settings);
		} else {
			response = null;
		}

		return response;
	}

	/**
	 * Checks whether {@link #generateResponseTo(String)} would generate a response
	 * for the specified text, without actually generating the response. This method
	 * is cheap to execute.
	 *
	 * @param text The utterance text to check if a response would be generated to
	 *             it.
	 * @return True if and only if {@link #generateResponseTo(String)} would return
	 *         a non-null value, false otherwise.
	 */
	public static boolean hasResponseTo(final String text) throws ResponseGenerationException {
		return text != null && !text.isBlank();
	}

	/**
	 * Executes the actual response generation logic, analyzing the user utterance
	 * text and deciding the most appropriate way to respond to it.
	 *
	 * @param text     The utterance text to generate a response to.
	 * @param settings The settings of the bot.
	 * @return The generated response text.
	 * @throws ResponseGenerationException If an error occurs while generating the
	 *                                     response, or the parameters are
	 *                                     {@code null}.
	 */
	private static String analyzeUtteranceAndGenerateResponse(
		final String text, final VacBotSettings settings
	) throws ResponseGenerationException {
		final String response;

		if (text == null || settings == null) {
			throw new ResponseGenerationException("The text or settings can't be null");
		}

		try {
			if (GREETING_PATTERN.matcher(text).lookingAt() && ONE_OR_MORE_SPACES.split(text).length < 8) {
				// Greetings canned responses
				response = generateGreetingResponse();
			} else if (GOODBYE_PATTERN.matcher(text).lookingAt() && ONE_OR_MORE_SPACES.split(text).length < 8) {
				// Goodbyes canned responses
				response = generateGoodbyeResponse();
			} else {
				final Annotation textAnnotation = new Annotation(text);
				parserPipeline.annotate(textAnnotation);

				final List<CoreMap> textSentences = textAnnotation.get(SentencesAnnotation.class);
				if (!textSentences.isEmpty()) {
					String questionAnswer = null;

					// Search for questions in the user utterance
					final Iterator<CoreMap> sentencesIter = textSentences.iterator();
					while (questionAnswer == null && sentencesIter.hasNext()) {
						final CoreMap sentence = sentencesIter.next();
						final Tree constituencyTree = sentence.get(TreeAnnotation.class);

						final TregexMatcher questionConstituencyTreeMatcher = DIRECT_QUESTION_PATTERN
							.matcher(constituencyTree);

						// Does the constituency tree matches a direct question tree pattern?
						if (questionConstituencyTreeMatcher.find()) {
							final Tree sbarqTree = questionConstituencyTreeMatcher.getMatch();
							questionAnswer = generateResponseUsingKnowledgeBase(
								sbarqTree,
								SemanticGraphFactory.makeFromTree(
									sbarqTree, SemanticGraphFactory.Mode.BASIC, GrammaticalStructure.Extras.NONE
								),
								settings
							);
						}
					}

					// Update the response to send accordingly
					if (questionAnswer == null) {
						// Fallback to IR if a question was not detected
						response = generateResponseUsingIndex(text, settings);
					} else {
						response = questionAnswer;
					}
				} else {
					// This shouldn't happen, but fallback sensibly to IR
					response = generateResponseUsingIndex(text, settings);
				}
			}
		} catch (final Exception exc) {
			throw new ResponseGenerationException(exc);
		}

		return response;
	}

	/**
	 * Uses the corpus document index to generate a response to the provided user
	 * utterance text. If that's not possible, a fallback non-commital canned
	 * response is returned.
	 *
	 * @param text     The utterance text to generate a response to. It must not be
	 *                 empty, and this method assumes that without checking.
	 * @param settings The settings of the bot, which influence how it generates the
	 *                 response.
	 * @return The generated response.
	 * @throws IOException              If an I/O error occurs reading the index.
	 * @throws PersistenceException     If an error occurs while accessing the
	 *                                  relational document database.
	 * @throws IllegalArgumentException If some parameter is {@code null}.
	 */
	private static String generateResponseUsingIndex(
		@NonNull final String text, @NonNull final VacBotSettings settings
	) throws IOException {
		final String response;

		final IndexSearcher indexSearcher = settings.getLuceneIndexSettings()
			.openIndex().getIndexSearcher();

		// Start by tokenizing and filtering the utterance tokens.
		// The utterance tokens will be used to do a span fuzzy query
		// on the corpus documents
		final Annotation utteranceTextAnnotation = new Annotation(text);

		tokenizationAndPosPipeline.annotate(utteranceTextAnnotation);

		final List<CoreLabel> coreNlpTokens = utteranceTextAnnotation.get(TokensAnnotation.class);
		final List<String> tokens = new ArrayList<>(coreNlpTokens.size());
		final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

		for (final CoreLabel token : coreNlpTokens) {
			final String posTag = token.get(PartOfSpeechAnnotation.class);

			// Do not take symbols or punctuation into account.
			// A updated list of POS tags is here:
			// https://web.stanford.edu/class/cs124/lec/postagging.pdf
			if (!posTag.equals("LS") && !posTag.equals("SYM") && posTag.length() > 1) {
				final String tokenText = token.get(TextAnnotation.class).toLowerCase(Locale.ROOT);
				tokens.add(tokenText);

				queryBuilder.add(
					fuzzyTokenToQuery(tokenText, settings),
					BooleanClause.Occur.SHOULD
				);
			}
		}

		// Do the search using the index
		final TopDocs results = indexSearcher.search(
			queryBuilder.setMinimumNumberShouldMatch(1).build(),
			settings.getLuceneIndexSettings().getMaxResults()
		);

		// The document retrieval was only successful if we have at least one result
		if (results.scoreDocs.length > 0) {
			// Get the full document text
			final String documentText = getDocumentText(
				indexSearcher.doc(
					results.scoreDocs[ThreadLocalRandom.current().nextInt(results.scoreDocs.length)].doc,
					DOCUMENT_ID_FIELD_SET
				).getField(DOCUMENT_ID_FIELD).numericValue().intValue(), // DOCUMENT_ID_FIELD is present so no NPE
				OriginalDocument.class, settings
			);

			// Now split it in sentences
			final Annotation documentTextAnnotation = new Annotation(documentText);

			sentenceSplitPipeline.annotate(documentTextAnnotation);

			// Get the most relevant sentence in the document for the utterance
			float bestSentenceScore = Float.NEGATIVE_INFINITY;
			String bestSentenceText = documentText;
			for (final CoreMap sentence : documentTextAnnotation.get(SentencesAnnotation.class)) {
				float sentenceScore = 0;

				for (final CoreLabel documentToken : sentence.get(TokensAnnotation.class)) {
					float maximumTokenScore = Float.NEGATIVE_INFINITY;

					// The score for this document token is the maximum score
					// of similarity with any utterance token, as giving extra
					// score in the case that several tokens are similar is not
					// desired
					for (final String utteranceToken : tokens) {
						maximumTokenScore = Math.max(
							new LevenshteinDistance().getDistance(
								documentToken.get(TextAnnotation.class).toLowerCase(Locale.ROOT),
								utteranceToken
							), maximumTokenScore
						);
					}

					sentenceScore += maximumTokenScore;
				}

				if (bestSentenceScore < sentenceScore) {
					bestSentenceScore = sentenceScore;
					bestSentenceText = sentence.get(TextAnnotation.class);
				}
			}

			// The most relevant sentence is our response
			response = bestSentenceText;
		} else {
			// Fallback to non-commital response
			response = generateNonCommitalResponse();
		}

		return response;
	}

	/**
	 * Uses the knowledge base to generate a response to the provided user direct
	 * question. If the knowledge base doesn't contain the appropriate facts for
	 * generating a response, the response will be generated by searching the
	 * document index instead, as if invoking
	 * {@link #generateResponseUsingIndex(String, VacBotSettings)} instead.
	 *
	 * @param sbarqTree The constituency subtree of the direct question.
	 * @param settings  The bot settings to use to generate responses.
	 * @return The generated response.
	 * @throws IOException              If the response generation via the fallback
	 *                                  index failed.
	 * @throws QueryException           If some error occurred while creating the
	 *                                  Jena query.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	private static String generateResponseUsingKnowledgeBase(
		@NonNull final Tree sbarqTree, @NonNull final SemanticGraph dependencyGraph,
		@NonNull final VacBotSettings settings
	) throws IOException, QueryException {
		final String response;

		// Get the root word and its constituency tag
		final IndexedWord root = dependencyGraph.getFirstRoot();
		final String rootTag = Objects.requireNonNullElse(
			root.backingLabel().tag(), ""
		);

		if (rootTag.startsWith("VB")) {
			// It is a verb, so try obtaining its nominal subject
			// (nsubj or nsubjpass, for passive voice).
			// We do not handle strange predicate subjects.
			// For now, we just handle subject questions, too
			IndexedWord subject = null;
			if (dependencyGraph.hasChildWithReln(root, UniversalGrammaticalRelations.NOMINAL_SUBJECT)) {
				subject = dependencyGraph.getChildWithReln(
					root, UniversalGrammaticalRelations.NOMINAL_SUBJECT
				);
			} else if (
				dependencyGraph.hasChildWithReln(root, UniversalGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT)
			) {
				subject = dependencyGraph.getChildWithReln(
					root, UniversalGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT
				);
			}

			if (subject != null) {
				final String subjectTag = Objects.requireNonNullElse(
					subject.backingLabel().tag(), ""
				);

				if (WH_TAG_PATTERN.matcher(subjectTag).matches()) {
					// Get the relevant object governed by the root
					final IndexedWord directObject = dependencyGraph.getChildWithReln(
						root, UniversalGrammaticalRelations.DIRECT_OBJECT
					);

					// Get different ways of expressing the direct object,
					// in preference order (first ones should be better)
					final LinkedList<String> objectCombinationsDeque = new LinkedList<>();
					generateOrderedDependentCombinations(
						dependencyGraph, directObject,
						directObject, objectCombinationsDeque
					);

					// Generate alternative ways of saying the verb, too
					final List<String> predicateCombinationsList = generateOrderedPredicateCombinations(
						dependencyGraph, root
					);

					// Do the ordered cartesian product with the lists of combinations,
					// so we try all possible combinations of combinations exhaustively in order
					final List<List<String>> predicateObjectCombinations = Lists.cartesianProduct(
						List.of(predicateCombinationsList, objectCombinationsDeque)
					);

					// Get some base URIs that we will use for querying the KB
					final UriBuilder basePropertyUriBuilder = UriBuilder.fromUri(
						settings.getKnowledgeBaseSettings()
							.getBaseModelUri().resolve("property")
					);

					final UriBuilder baseEntityUriBuilder = UriBuilder.fromUri(
						settings.getKnowledgeBaseSettings()
							.getBaseModelUri().resolve("entity")
					);

					// TODO: conform these 4 URI formats to the current version of TextProc,
					// not the older one we are using here for compatibility with an
					// already generated KB. These formats make Jena complain that we use
					// bad IRIs (and he's right!), although that doesn't affect functionality

					final String documentTypePropertyUri = basePropertyUriBuilder.fragment(
						"document-type"
					).build().toASCIIString();

					final String documentIdPropertyUri = basePropertyUriBuilder.fragment(
						"document-id"
					).build().toASCIIString();

					final String documentSentenceNumberUri = basePropertyUriBuilder.fragment(
						"document-sentence-number"
					).build().toASCIIString();

					final String sentimentPropertyUri = basePropertyUriBuilder.fragment(
						"sentiment-class"
					).build().toASCIIString();

					final String confidencePropertyUri = basePropertyUriBuilder.fragment(
						"confidence"
					).build().toASCIIString();

					final String sentimentFilterExpression = settings.getBehaviorSettings()
						.getResponseBias().toSparqlConditionExpression("sentiment");

					// We will likely submit queries to the KB, so start a transaction
					final Dataset jenaDataset = settings.getKnowledgeBaseSettings().connect().getDataset();
					String knowledgeBaseResponse = null;

					jenaDataset.begin(ReadWrite.READ);
					try {
						// Generate and execute queries using the computed
						// predicate-object combinations
						final Iterator<List<String>> predicateObjectCombinationsIter = predicateObjectCombinations.iterator();

						while (knowledgeBaseResponse == null && predicateObjectCombinationsIter.hasNext()) {
							final Iterator<String> predicateObjectCombinationIter =
								predicateObjectCombinationsIter.next().iterator();

							final String predicateUri = basePropertyUriBuilder.fragment(
								predicateObjectCombinationIter.next().toLowerCase(Locale.ROOT)
							).build().toASCIIString();

							final String objectUri = baseEntityUriBuilder.fragment(
								predicateObjectCombinationIter.next().toLowerCase(Locale.ROOT)
							).build().toASCIIString();

							// "While this class was in part designed to prevent SPARQL injection it is by no
							// means foolproof because it works purely at the textual level. The current version
							// of the code addresses some possible attack vectors that the developers have identified
							// but we do not claim to be sufficiently devious to have thought of and prevented every
							// possible attack vector."
							// However, as we previously sanitized the URI inputs, things should be fine...
							final ParameterizedSparqlString queryString = new ParameterizedSparqlString();

							queryString.setCommandText(
								// Most of these concatenations can be done at compile time
								"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
								"SELECT ?type ?id ?sentence\n" +
								"WHERE {\n" +
								"        ?t rdf:predicate ?predicateUri .\n" +
								"        ?t rdf:object ?objectUri .\n" +
								"        ?t ?documentTypePropertyUri ?type .\n" +
								"        ?t ?documentIdPropertyUri ?id .\n" +
								"        ?t ?documentSentenceNumberUri ?sentence .\n" +
								"        ?t ?sentimentPropertyUri ?sentiment .\n" +
								"        ?t ?confidencePropertyUri ?confidence .\n" +
								"        FILTER(" + sentimentFilterExpression + ")\n" +
								"}\n" +
								"ORDER BY DESC(?confidence)\n" +
								"LIMIT 1"
							);

							queryString.setIri("predicateUri", predicateUri);
							queryString.setIri("objectUri", objectUri);
							queryString.setIri("documentTypePropertyUri", documentTypePropertyUri);
							queryString.setIri("documentIdPropertyUri", documentIdPropertyUri);
							queryString.setIri("documentSentenceNumberUri", documentSentenceNumberUri);
							queryString.setIri("sentimentPropertyUri", sentimentPropertyUri);
							queryString.setIri("confidencePropertyUri", confidencePropertyUri);

							// Parse the generated SPARQL and execute it
							try (final QueryExecution queryExecution = QueryExecutionFactory.create(
								queryString.asQuery(),
								settings.getKnowledgeBaseSettings().connect().getModel()
							)) {
								final ResultSet hits = queryExecution.execSelect();

								// Because of the LIMIT 1 before, we get at most one result
								if (hits.hasNext()) {
									final QuerySolution solution = hits.next();

									try {
										final Class<? extends Document> documentType = URI_DOCUMENT_TYPE_TO_DOCUMENT_CLASS.get(
											new URI(
												solution.getResource("type").getURI()
											).getFragment()
										);
										final int documentId = solution.getLiteral("id").getInt();
										final int sentenceNumber = solution.getLiteral("sentence").getInt();

										// Finally, we have material to answer!
										knowledgeBaseResponse = getDocumentSentence(
											getDocumentText(documentId, documentType, settings),
											sentenceNumber
										);
									} catch (final NullPointerException | URISyntaxException ignored) {
										// We can retry the next possibility or fallback to IR
										// if the document type is invalid
									}
								}
							}
						}
					} finally {
						jenaDataset.commit();
					}

					if (knowledgeBaseResponse != null) {
						response = knowledgeBaseResponse;
					} else {
						// Fallback to IR if the knowledge base queries
						// were not successful
						response = generateResponseUsingIndex(
							SentenceUtils.listToOriginalTextString(
								sbarqTree.taggedLabeledYield()
							).trim(), settings
						);
					}
				} else {
					// We don't handle object questions for now
					response = generateResponseUsingIndex(
						SentenceUtils.listToOriginalTextString(
							sbarqTree.taggedLabeledYield()
						).trim(), settings
					);
				}
			} else {
				// We don't know how to handle this, fallback to IR
				response = generateResponseUsingIndex(
					SentenceUtils.listToOriginalTextString(
						sbarqTree.taggedLabeledYield()
					).trim(), settings
				);
			}
		} else {
			// It is not a verb.
			// If the root is a WP, we have a copulative verb,
			// but we do not handle that for now
			// (asking for what "is" something is prone to vague
			// responses).
			// Retrieve the original response text and use it with IR
			response = generateResponseUsingIndex(
				SentenceUtils.listToOriginalTextString(
					sbarqTree.taggedLabeledYield()
				).trim(), settings
			);
		}

		return response;
	}

	/**
	 * Generates combinations of the specified root word with other words it
	 * governs, in manners that elicits a more or less precise meaning about the
	 * root word, and are suitable for querying the knowledge base.
	 * <p>
	 * The algorithm implemented here is relatively simple and may not capture the
	 * most appropriate candidates for some complex grammatical graphs, which are
	 * hopefully unlikely to occur in practice.
	 * </p>
	 *
	 * @param dependencyGraph   The dependency graph of the sentence where the root
	 *                          word belongs to.
	 * @param currentRoot       The current root word, used for recursion.
	 *                          Initially, the value of this parameter should be
	 *                          equal to {@code root}.
	 * @param root              The root word to generate its ordered combinations
	 *                          with its dependents.
	 * @param combinationsDeque The stack where the generated combinations will be
	 *                          stored.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	private static void generateOrderedDependentCombinations(
		@NonNull final SemanticGraph dependencyGraph, @NonNull final IndexedWord currentRoot,
		@NonNull final IndexedWord root, @NonNull final Deque<String> combinationsDeque
	) {
		// Potential for improvement:
		// Go back in the dependency path and somehow add to each node
		// its dependents, without things exploding
		final List<IndexedWord> pathToRoot = dependencyGraph
			.getShortestDirectedPathNodes(root, currentRoot);

		// Add the path to root, which includes ourselves
		final List<String> pathToRootCombinations = indexedWordPathToCombinations(pathToRoot);
		final ListIterator<String> pathToRootCombinationsIter = pathToRootCombinations
			.listIterator(pathToRootCombinations.size());

		while (pathToRootCombinationsIter.hasPrevious()) {
			combinationsDeque.push(pathToRootCombinationsIter.previous());
		}

		// Do the same for the dependencies of the current root
		for (final SemanticGraphEdge dependentsOfCurrentRoot : dependencyGraph.getOutEdgesSorted(currentRoot)) {
			generateOrderedDependentCombinations(
				dependencyGraph, dependentsOfCurrentRoot.getTarget(), root, combinationsDeque
			);
		}
	}

	/**
	 * Creates a list which contains different ways to express the provided word
	 * path.
	 *
	 * @param wordPath The word path whose different ways to be expressed should be
	 *                 calculated.
	 * @return The described list.
	 * @throws IllegalArgumentException If the argument is {@code null}.
	 */
	private static List<String> indexedWordPathToCombinations(
		@NonNull final List<IndexedWord> wordPath
	) {
		final List<String> result = new ArrayList<>();
		final StringBuilder stringBuilder = new StringBuilder();
		ListIterator<IndexedWord> wordPathIter = wordPath.listIterator(wordPath.size());

		// First add the path with the original texts
		while (wordPathIter.hasPrevious()) {
			final IndexedWord word = wordPathIter.previous();

			// The path is just the concatenation of words
			stringBuilder
				.append(word.originalText())
				.append(" ");
		}

		if (stringBuilder.length() > 0) {
			// Delete trailing space
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
			result.add(stringBuilder.toString());
		}

		// Discard string builder contents, but preserve its reserved buffer
		// to reuse it efficiently
		stringBuilder.setLength(0);

		// Reset the iterator position
		wordPathIter = wordPath.listIterator(wordPath.size());

		// Then add the path with the lemmatized text
		while (wordPathIter.hasPrevious()) {
			final IndexedWord word = wordPathIter.previous();

			stringBuilder
				.append(word.lemma())
				.append(" ");
		}

		if (stringBuilder.length() > 0) {
			// Delete trailing space
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);

			// Do not add the string to the result if it is the same
			final String pathString = stringBuilder.toString();
			if (!result.get(0).equalsIgnoreCase(pathString)) {
				result.add(pathString);
			}
		}

		return result;
	}

	/**
	 * Generates different ways of expressing the predicate of a RDF triple from the
	 * main verb of a sentence.
	 *
	 * @param dependencyGraph The dependency graph of the sentence.
	 * @param verb            The main verb of the sentence.
	 * @return A list with the different ways of expressing the predicate of a RDF
	 *         triple, in preference order.
	 * @throws IllegalArgumentException If some parameter is {@code null}.
	 */
	private static List<String> generateOrderedPredicateCombinations(
		@NonNull final SemanticGraph dependencyGraph, @NonNull final IndexedWord verb
	) {
		final String rootString = verb.originalText();
		final String lemmaRootString = verb.lemma();

		// The verb dependents (indexed words) are ordered
		// according to their sentence position. This is necessary
		// so the generated predicates are natural
		final SortedSet<IndexedWord> verbDependents = new TreeSet<>();
		final List<String> alternativePredicates = new ArrayList<>(4);

		addDependentWithGrammaticalRelationToCollection(
			dependencyGraph, verb, UniversalGrammaticalRelations.MARKER,
			verbDependents
		);

		addDependentWithGrammaticalRelationToCollection(
			dependencyGraph, verb, UniversalGrammaticalRelations.ADVERBIAL_MODIFIER,
			verbDependents
		);

		addDependentWithGrammaticalRelationToCollection(
			dependencyGraph, verb, UniversalGrammaticalRelations.AUX_MODIFIER,
			verbDependents
		);

		addDependentWithGrammaticalRelationToCollection(
			dependencyGraph, verb, UniversalGrammaticalRelations.EXPLETIVE,
			verbDependents
		);

		// If we have dependents, generate candidate predicates with them
		if (!verbDependents.isEmpty()) {
			IndexedWord currentStartDependent = verbDependents.first();
			final StringBuilder combinationStringBuilder = new StringBuilder();
			final StringBuilder combinationLemmaStringBuilder = new StringBuilder();

			while (currentStartDependent != null) {
				final SortedSet<IndexedWord> currentDependentsSet = verbDependents.tailSet(
					currentStartDependent
				);
				IndexedWord firstDifferentDependent = null;

				// Discard string builder contents, but not the memory they allocated
				combinationStringBuilder.setLength(0);
				combinationLemmaStringBuilder.setLength(0);

				for (final IndexedWord dependent : currentDependentsSet) {
					// Update the first different dependent if applicable
					if (
						!dependent.equals(currentStartDependent) &&
						firstDifferentDependent == null
					) {
						firstDifferentDependent = dependent;
					}

					// Join the dependents of this set in sentence order
					combinationStringBuilder
						.append(dependent.originalText())
						.append(" ");

					combinationLemmaStringBuilder
						.append(dependent.lemma())
						.append(" ");
				}

				combinationStringBuilder
					.append(rootString)
					.append(" ");

				combinationStringBuilder.deleteCharAt(
					combinationStringBuilder.length() - 1
				);

				final String combinationString = combinationStringBuilder.toString();
				alternativePredicates.add(combinationString);

				// Add the lemmatized version too, but later so it's less prioritary
				combinationLemmaStringBuilder
					.append(lemmaRootString)
					.append(" ");

				combinationLemmaStringBuilder.deleteCharAt(
					combinationLemmaStringBuilder.length() - 1
				);

				final String combinationLemmaString = combinationLemmaStringBuilder.toString();
				if (!combinationLemmaString.equalsIgnoreCase(combinationString)) {
					alternativePredicates.add(combinationLemmaStringBuilder.toString());
				}

				currentStartDependent = firstDifferentDependent;
			}
		}

		// Add just the original verb
		alternativePredicates.add(rootString);

		if (!rootString.equalsIgnoreCase(lemmaRootString)) {
			alternativePredicates.add(lemmaRootString);
		}

		return alternativePredicates;
	}

	/**
	 * Tries to add the first dependent that has the specified grammatical
	 * relationship with the specified word to the provided collection.
	 *
	 * @param dependencyGraph     The dependency graph of the sentence where the
	 *                            word is.
	 * @param governor            The word whose dependent will try to be added.
	 * @param grammaticalRelation The grammatical relation between the desired
	 *                            dependent and the governor.
	 * @param dependentCollection The collection to add the dependent to.
	 * @return True if and only if an appropriate dependent was found and added to
	 *         the collection; false otherwise.
	 * @throws IllegalArgumentException If some parameter is {@code null}.
	 */
	private static final boolean addDependentWithGrammaticalRelationToCollection(
		@NonNull final SemanticGraph dependencyGraph, @NonNull final IndexedWord governor,
		@NonNull final GrammaticalRelation grammaticalRelation,
		@NonNull final Collection<IndexedWord> dependentCollection
	) {
		final IndexedWord dependent = dependencyGraph.getChildWithReln(
			governor, grammaticalRelation
		);

		boolean inserted = false;

		if (dependent != null) {
			try {
				dependentCollection.add(dependent);
				inserted = true;
			} catch (
				final UnsupportedOperationException | ClassCastException | NullPointerException |
				IllegalArgumentException | IllegalStateException ignored
			) {}
		}

		return inserted;
	}

	/**
	 * Retrieves the specified sentence number from the specified document. If the
	 * sentence number is greater than or equal to zero but doesn't correspond to a
	 * valid sentence in the document, the entire input document will be returned,
	 * as a fallback.
	 *
	 * @param document       The document from which to extract a sentence.
	 * @param sentenceNumber The sentence number within the document. The first
	 *                       sentence in a document is zero.
	 * @return The sentence of the document, or the entire document if the sentence
	 *         number was not found.
	 * @throws IllegalArgumentException If {@code document} is {@code null}, or
	 *                                  {@code sentenceNumber} is less than zero.
	 */
	private static final String getDocumentSentence(
		@NonNull final String document, final int sentenceNumber
	) {
		final String documentSentence;
		final Annotation documentAnnotation;

		if (sentenceNumber < 0) {
			throw new IllegalArgumentException("The sentence number can't be negative");
		}

		documentAnnotation = new Annotation(document);
		sentenceSplitPipeline.annotate(documentAnnotation);

		final List<CoreMap> sentences = documentAnnotation.get(SentencesAnnotation.class);
		if (sentenceNumber < sentences.size()) {
			documentSentence = sentences.get(sentenceNumber).get(TextAnnotation.class);
		} else {
			// Fallback to the entire document
			documentSentence = document;
		}

		return documentSentence;
	}

	/**
	 * Generates a canned response to a greeting.
	 *
	 * @return The generated greeting response.
	 */
	private static String generateGreetingResponse() {
		return GREETING_RESPONSES[ThreadLocalRandom.current().nextInt(GREETING_RESPONSES.length)];
	}

	/**
	 * Generates a canned response to a goodbye.
	 *
	 * @return The generated goodbye response.
	 */
	private static String generateGoodbyeResponse() {
		return GOODBYE_RESPONSES[ThreadLocalRandom.current().nextInt(GOODBYE_RESPONSES.length)];
	}

	/**
	 * Generates a non-commital response, hopefully kind of appropriate for any
	 * text.
	 *
	 * @return The generated non-commital response.
	 */
	private static String generateNonCommitalResponse() {
		return NON_COMMITAL_RESPONSES[ThreadLocalRandom.current().nextInt(NON_COMMITAL_RESPONSES.length)];
	}

	/**
	 * Wraps the specified term in a {@link FuzzyQuery}.
	 *
	 * @param token    The token to wrap.
	 * @param settings The settings of the bot, used to customize how the wrapping
	 *                 is done.
	 * @return The wrapped token in a {@link FuzzyQuery}.
	 * @throws IllegalArgumentException If some parameter is {@code null}.
	 */
	private static FuzzyQuery fuzzyTokenToQuery(@NonNull final String token, @NonNull final VacBotSettings settings) {
		return new FuzzyQuery(
			// To lower case to ignore case differences
			new Term("text", token.toLowerCase(Locale.ROOT)),
			settings.getLuceneIndexSettings().getTermFuzzyQueryDistance()
		);
	}

	/**
	 * Retrieves the document text for the document with the specified identifier.
	 *
	 * @param <T>          The type of documents to return the text of.
	 * @param id           The identifier of the document to return its text.
	 * @param documentType The class object that represent the type of documents to
	 *                     return the text of.
	 * @param settings     The settings of the bot.
	 * @return The text of the document whose identifier matches the provided
	 *         identifier.
	 * @throws PersistenceException     If some error occurred while accessing the
	 *                                  database.
	 * @throws NoResultException        If a document with the specified identifier
	 *                                  and type does not exist.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	private static <T extends Document> String getDocumentText(
		final int id, @NonNull final Class<T> documentType, @NonNull final VacBotSettings settings
	) {
		final EntityManager documentEntityManager = settings.getDocumentDatabaseSettings().openEntityManager();

		// Construct the query using the criteria builder to avoid parsing overhead
		final CriteriaBuilder criteriaBuilder = documentEntityManager.getCriteriaBuilder();
		final CriteriaQuery<String> resultTextQuery = criteriaBuilder.createQuery(String.class);
		final Root<T> documentTable = resultTextQuery.from(documentType);

		resultTextQuery
			.select(documentTable.get("text"))
			.where(
				criteriaBuilder.equal(
					documentTable.get("id"), id
				)
			);

		final EntityTransaction documentEntityTx = documentEntityManager.getTransaction();
		documentEntityTx.begin();

		try {
			return documentEntityManager.createQuery(resultTextQuery).getSingleResult();
		} finally {
			assert documentEntityTx.isActive();

			if (documentEntityTx.getRollbackOnly()) {
				documentEntityTx.rollback();
			} else {
				documentEntityTx.commit();
			}
		}
	}
}
