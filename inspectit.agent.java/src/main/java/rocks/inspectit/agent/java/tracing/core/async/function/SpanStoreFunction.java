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
 * This class is used to substitute a {@link java.util.function.Function} object when a new unit of execution is added to a completable future.
 * Calls of {@link #apply()} will be delegated to the original Function and, in addition, a new Span is started if provided.
 *
 * @author Jacob Waffle
 *
 */
@ProxyFor(implementedInterfaces = "java.util.function.Function")
public class SpanStoreFunction extends SpanStore implements IProxySubject, TagsProvidingAdapter {
	
	/**
	 * The original function object.
	 */
	private final Object function;

	/**
	 * Constructor.
	 *
	 * @param function
	 *            original function which will be wrapped
	 */
	public SpanStoreFunction(final Object function) {
		Preconditions.checkNotNull(function);

		this.function = function;
	}

	/**
	 * {@inheritDoc}}
	 */
	@Override
	public Object[] getProxyConstructorArguments() {
		return new Object[0];
	}

	/**
	 * {@inheritDoc}}
	 */
	@Override
	public void proxyLinked(final Object proxyObject, final IRuntimeLinker linker) {
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getTags() {
		return ImmutableMap.of(ExtraTags.RUNNABLE_TYPE, function.getClass().getName());
	}

	/**
     * Performs this operation on the given argument. 
     * @param arg0 The input argument.
	 */
	@ProxyMethod(parameterTypes = { "java.lang.Object" })
	public Object apply(final Object arg0) {
		startSpan();

		Object result;
		// The span should be finished regardless of any errors and errors should also be propagated 
		// to the caller.
		try {
		    result = WFunction.apply.call(function, arg0);
		} catch(RuntimeException e) {
			throw e;
		} finally {
		    finishSpan(this);
		}
		
		return result;
	}

	/**
	 * Reflection wrapper class for {@link java.util.function.Function}.
	 */
	static final class WFunction {
		/**
		 * See {@link java.util.function.Function#accept(java.lang.Object)}.
		 */
		static CachedMethod<Void> apply = new CachedMethod<Void>("java.util.function.Function", "apply", Object.class);
	}
}
