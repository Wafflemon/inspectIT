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
 * This class is used to substitute a {@link java.util.function.Supplier} object when a new unit of execution is added to a completable future.
 * Calls of {@link #get()} will be delegated to the original Supplier and, in addition, a new Span is started if provided.
 *
 * @author Jacob Waffle
 *
 */
@SuppressWarnings({"PMD.AvoidRethrowingException", "We want to rethrow exceptions."})
@ProxyFor(implementedInterfaces = "java.util.function.Supplier")
public class SpanStoreSupplier extends SpanStore implements IProxySubject, TagsProvidingAdapter {

	/**
	 * The original supplier object.
	 */
	private final Object supplier;

	/**
	 * Constructor.
	 *
	 * @param supplier
	 *            Original supplier which will be wrapped.
	 */
	public SpanStoreSupplier(final Object supplier) {
		Preconditions.checkNotNull(supplier);

		this.supplier = supplier;
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
		return ImmutableMap.of(ExtraTags.RUNNABLE_TYPE, supplier.getClass().getName());
	}
	
	/**
	 * Gets a result.
     * @return a result
	 */
	@ProxyMethod(parameterTypes = {})
	public Object get() {
		startSpan();

		Object result;
		// The span should be finished regardless of any errors and errors should also be propagated 
		// to the caller.
		try {
		    result = WSupplier.get.call(supplier);
		} catch (RuntimeException e) {
			throw e;
		} finally {
		    finishSpan(this);
		}
		
		return result;
	}

	/**
	 * Reflection wrapper class for {@link java.util.function.Supplier}.
	 */
	static final class WSupplier {
		/**
		 * See {@link java.util.function.Supplier#get()}.
		 */
		static CachedMethod<Void> get = new CachedMethod<Void>("java.util.function.Supplier", "get");
	}
}
