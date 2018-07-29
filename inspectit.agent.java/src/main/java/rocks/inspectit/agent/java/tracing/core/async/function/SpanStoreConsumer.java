package rocks.inspectit.agent.java.tracing.core.async.function;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import rocks.inspectit.agent.java.eum.reflection.CachedMethod;
import rocks.inspectit.agent.java.proxy.IProxySubject;
import rocks.inspectit.agent.java.proxy.IRuntimeLinker;
import rocks.inspectit.agent.java.proxy.ProxyFor;
import rocks.inspectit.agent.java.proxy.ProxyMethod;
import rocks.inspectit.agent.java.tracing.core.adapter.TagsProvidingAdapter;
import rocks.inspectit.agent.java.tracing.core.async.SpanStore;
import rocks.inspectit.shared.all.tracing.constants.ExtraTags;

/**
 * This class is used to substitute a {@link java.util.function.Consumer} object when a new unit of execution is added to a completable future.
 * Calls of {@link #accept()} will be delegated to the original Consumer and, in addition, a new Span is started if provided.
 *
 * @author Jacob Waffle
 *
 */
@SuppressWarnings({"PMD.AvoidRethrowingException", "We want to rethrow exceptions."})
@ProxyFor(implementedInterfaces = "java.util.function.Consumer")
public class SpanStoreConsumer extends SpanStore implements IProxySubject, TagsProvidingAdapter {

	/**
	 * The original consumer object.
	 */
	private final Object consumer;

	/**
	 * Constructor.
	 *
	 * @param consumer
	 *            Original consumer which will be wrapped.
	 */
	public SpanStoreConsumer(final Object consumer) {
		Preconditions.checkNotNull(consumer);

		this.consumer = consumer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getProxyConstructorArguments() {
		return new Object[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void proxyLinked(final Object proxyObject, final IRuntimeLinker linker) {
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getTags() {
		return ImmutableMap.of(ExtraTags.RUNNABLE_TYPE, consumer.getClass().getName());
	}

	/**
     * Performs this operation on the given argument. 
     * @param arg0 The input argument.
	 */
	@ProxyMethod(parameterTypes = { "java.lang.Object" })
	public void accept(final Object arg0) {
		startSpan();

		// The span should be finished regardless of any errors and errors should also be propagated 
		// to the caller.
		try {
		    WConsumer.accept.call(this.consumer, arg0);
		} catch (RuntimeException e) {
			throw e;
		} finally {
		    finishSpan(this);
		}
	}

	/**
	 * Reflection wrapper class for {@link java.util.function.Consumer}.
	 */
	static final class WConsumer {
		/**
		 * See {@link java.util.function.Consumer#accept(java.lang.Object}.
		 */
		static CachedMethod<Void> accept = new CachedMethod<Void>("java.util.function.Consumer", "accept", Object.class);
	}
}
