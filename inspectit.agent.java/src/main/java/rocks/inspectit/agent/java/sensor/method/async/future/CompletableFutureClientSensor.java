package rocks.inspectit.agent.java.sensor.method.async.future;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import rocks.inspectit.agent.java.hooking.IHook;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;
import rocks.inspectit.agent.java.sensor.method.AbstractMethodSensor;
import rocks.inspectit.agent.java.sensor.method.IMethodSensor;
import rocks.inspectit.agent.java.tracing.core.listener.IAsyncSpanContextListener;

/**
 * The completable future client sensor which initializes and returns the {@link CompletableFutureClientHook} class.
 *
 * @author Jacob Waffle
 *
 */
public class CompletableFutureClientSensor extends AbstractMethodSensor implements IMethodSensor {

	/**
	 * The hook.
	 */
	private CompletableFutureClientHook hook;

	/**
	 * Listener for firing async spans.
	 */
	@Autowired
	private IAsyncSpanContextListener asyncSpanContextListener;

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
		hook = new CompletableFutureClientHook(asyncSpanContextListener, tracer);
	}
}
