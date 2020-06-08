// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.dispatchers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.frontend.TextMessage;
import es.uvigo.esei.sing.vacbot.responsegen.ResponseGenerationException;
import es.uvigo.esei.sing.vacbot.settings.VacBotSettings;
import lombok.NonNull;

/**
 * Executes the dispatching logic to respond to user text messages, according to
 * the provided user settings, in a multi-threaded fashion.
 *
 * @author Alejandro González García
 * @param <T> The concrete type of text messages that this dispatcher
 *            dispatches.
 * @param <U> The concrete type of the forthcoming response notification data.
 */
public abstract class TextMessageDispatcher<T extends TextMessage, U> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextMessageDispatcher.class);

	protected final VacBotSettings settings;

	private final ExecutorService executorService;
	private final FrontendInterface<T, U> frontendInterface;
	private Thread dispatchingThread = null;

	/**
	 * Creates a new text message dispatcher for the specified operation settings.
	 *
	 * @param frontendInterface The front-end interface that this dispatcher should
	 *                          use.
	 * @param settings          The settings that define how the dispatcher should
	 *                          work.
	 * @throws IllegalArgumentException If a parameter is {@code null}.
	 */
	protected TextMessageDispatcher(@NonNull final FrontendInterface<T, U> frontendInterface, @NonNull final VacBotSettings settings) {
		this.frontendInterface = frontendInterface;
		this.settings = settings;

		final int workerThreads = settings.getWorkerThreads();
		final int maximumQueuedTasks = (int) Math.min(
			// Consider that each task uses 4 MiB while enqueued (seems pessimistic)
			Runtime.getRuntime().freeMemory() / 4194304, Integer.MAX_VALUE
		);

		final AtomicInteger queuedTaskCount = new AtomicInteger();

		final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
			// Use at least hardwareThreads threads,
			// that can be expanded up to hardwareThreads * 32 threads
			// when load is high, for maximum throughput. We prefer that to
			// queuing because extra threads usually allows the OS scheduler
			// to better manage blocking I/O and such
			workerThreads, workerThreads * 32,
			// Excess threads get deleted after 60 seconds
			60, TimeUnit.SECONDS,
			new LinkedTransferQueue<>() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean offer(final Runnable e) {
					// When the core threads are working, prefer
					// reusing excess threads or creating new ones
					// before actual queuing
					return tryTransfer(e);
				}
			},
			// Rejected execution handler
			(final Runnable r, final ThreadPoolExecutor e) -> {
				// If we reject a task, that was because we've created
				// the maximum threads and no thread was available
				// to handle the task. Therefore, start queuing tasks
				// until maximumQueuedTasks is reached
				if (
					queuedTaskCount.incrementAndGet() <= maximumQueuedTasks && !e.isShutdown()
				) {
					try {
						e.getQueue().put(r);
					} catch (final InterruptedException ignored) {
						// Never happens according to LinkedTransferQueue implementation,
						// because it is unbounded
					}
				} else if (!e.isShutdown()) {
					// Run in the caller thread so things slow down, but no
					// extra resources are consumed
					r.run();
				} else {
					throw new RejectedExecutionException("The executor was shut down");
				}
			}
		) {
			@Override
			protected void beforeExecute(final Thread t, final Runnable r) {
				// We can queue one more task when we begin execution of a task
				queuedTaskCount.updateAndGet((final int value) -> Math.max(value - 1, 0));
			}

			@Override
			protected void terminated() {
				try {
					settings.close();
				} catch (final Exception exc) {
					LOGGER.error(
						"An exception has occurred while freeing the settings resources", exc
					);
				}
			}
		};

		// Create core threads now for less latency on the first message
		threadPoolExecutor.prestartAllCoreThreads();

		this.executorService = Executors.unconfigurableExecutorService(threadPoolExecutor);
	}

	/**
	 * Starts the user text message dispatch loop, sending the appropriate responses
	 * to user messages, until the current thread is interrupted or {@link #stop()}
	 * is called; whichever happens first. Therefore, this method doesn't return
	 * until the dispatch is stopped by other thread.
	 * <p>
	 * This method assumes that is called only once during the lifetime of the
	 * application. Failure to guarantee that precondition, especially if called
	 * from different threads, may result in broken behavior.
	 * </p>
	 */
	public final void dispatchUntilInterrupted() {
		dispatchingThread = Thread.currentThread();

		try {
			while (!Thread.interrupted()) {
				try {
					final T message = frontendInterface.awaitNextMessage();

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Received message: {}", message);
					}

					// We might have awaited for a message successfully, but stop() got called
					// before this execution, so the execution would be rejected
					executorService.execute(() -> {
						try {
							final T response = frontendInterface.isMessageForBot(message) ? computeResponse(message) : null;

							if (response != null) {
								frontendInterface.sendMessage(response);
							}
						} catch (final ResponseGenerationException | FrontendCommunicationException exc) {
							LOGGER.error(
								"An exception has occurred while sending or computing a response to a message. The response won't be sent",
								exc
							);
						}
					});
				} catch (final FrontendCommunicationException exc) {
					// Just log exception while waiting
					LOGGER.warn(
						"An exception has occurred while awaiting for new messages", exc
					);
				}
			}
		} catch (final RejectedExecutionException | InterruptedException ignored) {}
	}

	/**
	 * Stops this dispatcher from dispatching any new text message, and returns once
	 * the already dispatched text messages were processed, in a best-effort manner.
	 * Calling this method also frees resources used by the settings and other
	 * objects.
	 */
	public final void stop() {
		if (dispatchingThread != null) {
			dispatchingThread.interrupt();
		}

		// Execute remaining tasks, but do not accept new ones
		executorService.shutdown();
		try {
			// Wait for graceful shutdown to conclude
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				LOGGER.warn(
					"The remaining text messages couldn't be dispatched in a reasonable time. " +
					"Trying to abort the dispatch of all messages"
				);

				// If that didn't work, interrupt running tasks and drain
				// non-executed tasks
				executorService.shutdownNow();
				if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
					// Give up, termination unsuccessful
					LOGGER.error("The text message dispatcher couldn't be stopped safely");
				}
			}
		} catch (final InterruptedException exc) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Generates a text message response to the text message received from a user.
	 * This method can be executed in any thread in a concurrent manner. When this
	 * method is invoked, it is guaranteed that the message is meant to be addressed
	 * by the bot, without further checks needed.
	 *
	 * @param message The user text message to generate a response to. It won't be
	 *                {@code null}.
	 * @return The generated text message with the response. If {@code null}, no
	 *         response will be sent.
	 * @throws ResponseGenerationException If an error occurred that made it
	 *                                     impossible to compute a response.
	 */
	protected abstract T computeResponse(final T message) throws ResponseGenerationException;

	/**
	 * Calls the {@link FrontendInterface#notifyForthcomingResponse(U)} method on
	 * the front-end instance used by this object. The contract of this method is
	 * that of the proxied method, but any {@link FrontendCommunicationException}
	 * thrown is handled without altering the program flow.
	 *
	 * @param notificationData A front-end interface specific object which contains
	 *                         the needed data or logic to generate the
	 *                         notification.
	 * @throws IllegalArgumentException If {@code notificationData} doesn't satisfy
	 *                                  a front-end specific predicate (like, for
	 *                                  instance, not being {@code null}).
	 */
	protected final void notifyForthcomingResponse(final U notificationData) {
		try {
			frontendInterface.notifyForthcomingResponse(notificationData);
		} catch (final FrontendCommunicationException exc) {
			// Minor failure, continue as if nothing happened
			LOGGER.trace("Couldn't notify the forthcoming response", exc);
		}
	}
}
