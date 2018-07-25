package rocks.inspectit.agent.java.sensor.method.special;

import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;

import rocks.inspectit.agent.java.hooking.IHook;
import rocks.inspectit.agent.java.hooking.ISpecialHook;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;
import rocks.inspectit.agent.java.sensor.method.AbstractMethodSensor;
import rocks.inspectit.agent.java.sensor.method.async.future.CompletableFutureClientHook;
import rocks.inspectit.agent.java.tracing.core.async.SpanStoreRunnable;

/**
 * The completable future intercepter sensor which initializes and returns the {@link CompletableFutureIntercepterHook} class.
 *
 * @author Jacob Waffle
 *
 */
public class CompletableFutureIntercepterSensor extends AbstractMethodSensor {

	/**
	 * Hook to use.
	 */
	ISpecialHook hook;

	/**
	 * The tracer.
	 */
	@Autowired
	private TracerImpl tracer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IHook getHook() {
		return hook;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initHook(Map<String, Object> parameters) {
		hook = new CompletableFutureIntercepterHook(tracer);
	}
}
