package rocks.inspectit.agent.java.tracing.core.async.function;

import rocks.inspectit.agent.java.proxy.IProxySubject;
import rocks.inspectit.agent.java.proxy.IRuntimeLinker;
import rocks.inspectit.agent.java.proxy.ProxyFor;
import rocks.inspectit.agent.java.tracing.core.async.SpanStore;

/**
 * This class is used to substitute a {@link java.util.function.BiConsumer} object when a new unit of execution is added to a completable future.
 * Calls of {@link #accept()} will be delegated to the original BiConsumer and, in addition, a new
 * Span is started if provided.
 *
 * @author Jacob Waffle
 *
 */
@ProxyFor(implementedInterfaces = "java.util.function.BiConsumer")
public class SpanSoreBiConsumer extends SpanStore implements IProxySubject {

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
	public void proxyLinked(Object proxyObject, IRuntimeLinker linker) {
		
	}

}
