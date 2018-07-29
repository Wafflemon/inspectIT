package rocks.inspectit.agent.java.sensor.method.async.future;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.References;
import io.opentracing.tag.Tags;
import rocks.inspectit.agent.java.config.impl.RegisteredSensorConfig;
import rocks.inspectit.agent.java.config.impl.SpecialSensorConfig;
import rocks.inspectit.agent.java.core.ICoreService;
import rocks.inspectit.agent.java.hooking.IMethodHook;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.SpanBuilderImpl;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.SpanImpl;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;
import rocks.inspectit.agent.java.sensor.method.http.StartEndMarker;
import rocks.inspectit.agent.java.tracing.core.async.SpanStore;
import rocks.inspectit.agent.java.tracing.core.async.executor.SpanStoreRunnable;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreBiConsumer;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreBiFunction;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreConsumer;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreFunction;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreSupplier;
import rocks.inspectit.agent.java.tracing.core.listener.IAsyncSpanContextListener;
import rocks.inspectit.shared.all.tracing.constants.ExtraTags;
import rocks.inspectit.shared.all.tracing.data.PropagationType;

/**
 * The completable future client hook which injects the current span context into a method parameter
 * that is a {@link SpanStore}.
 *
 * @author Jacob Waffle
 *
 */
public class CompletableFutureClientHook implements IMethodHook {

	/**
	 * The logger of this class. Initialized manually.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CompletableFutureClientHook.class);
	
	/**
	 * The fully qualified name for {@link Runnable}s.
	 */
	private static final String RUNNABLE_FQN    = "java.lang.Runnable";
	
	/**
	 * A map of the supported functional interfaces that contain units of execution for the hooked {@link java.util.concurrent.CompletableFuture}.
	 */
	private static final Map<String, Class<? extends SpanStore>> FUNCTIONAL_FQNS = new HashMap<String, Class<? extends SpanStore>>();

	/**
	 * Sets up the fully qualified names for the functional interfaces defined in Java 8.
	 */
	static {
		FUNCTIONAL_FQNS.put("java.util.function.BiFunction", SpanStoreBiFunction.class);
		FUNCTIONAL_FQNS.put("java.util.function.BiConsumer", SpanStoreBiConsumer.class);
		FUNCTIONAL_FQNS.put("java.util.function.Consumer", SpanStoreConsumer.class);
		FUNCTIONAL_FQNS.put("java.util.function.Function", SpanStoreFunction.class);
		FUNCTIONAL_FQNS.put("java.util.function.Supplier", SpanStoreSupplier.class);
	}
	
	/**
	 * Helps us to ensure that we only execute one remote client hook for each client request on all
	 * remote client sensor implementations.
	 * <p>
	 * Static on purpose.
	 */
	private static final StartEndMarker REF_MARKER = new StartEndMarker();

	/**
	 * Listener for firing async spans.
	 */
	private IAsyncSpanContextListener asyncSpanContextListener;

	/**
	 * The tracer.
	 */
	private TracerImpl tracer;

	/**
	 * Constructor.
	 *
	 * @param asyncSpanContextListener
	 *            the listener for async spans
	 * @param tracer
	 *            the tracer
	 */
	public CompletableFutureClientHook(IAsyncSpanContextListener asyncSpanContextListener, TracerImpl tracer) {
		this.asyncSpanContextListener = asyncSpanContextListener;
		this.tracer = tracer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeBody(long methodId, long sensorTypeId, Object object, Object[] parameters, RegisteredSensorConfig rsc) {
		if (!REF_MARKER.isMarkerSet()) {
			if (parameters.length > 0) {

				final Runnable runnable   = (Runnable) getParameter(parameters, rsc, RUNNABLE_FQN);

				SpanStore spanStore = null;
				String spanStoreFQN = null;
				if (null != runnable) {
					spanStore = new SpanStoreRunnable(runnable);
					spanStoreFQN = RUNNABLE_FQN;
				} else {
					for (Map.Entry<String, Class<? extends SpanStore>> entry : FUNCTIONAL_FQNS.entrySet()) {
						final Object functional = getParameter(parameters, rsc, entry.getKey());
						
						if (null != functional) {
							try {
								spanStore = entry.getValue().getConstructor(Object.class).newInstance(functional);
							} catch (RuntimeException e) {
								LOG.error(toStracktrace(e));
								return;
							} catch (InstantiationException e) {
								LOG.error(toStracktrace(e));
								return;
							} catch (IllegalAccessException e) {
								LOG.error(toStracktrace(e));
								return;
							} catch (InvocationTargetException e) {
								LOG.error(toStracktrace(e));
								return;
							} catch (NoSuchMethodException e) {
								LOG.error(toStracktrace(e));
								return;
							}
							spanStoreFQN = entry.getKey();
						}
					}
					
					if (null == spanStore) {
						// If we reach this case, then we are instrumenting a method that does not consume a supported parameter type.
						// So we don't do any instrumenting.
						return;
					}
				}
				
				// Substitutes a parameter of interest for a version that extends {@link SpanStore}
				setParameter(parameters, rsc, spanStoreFQN, spanStore); 
				
				// Sets information about the span that is going to track the execution of the function parameter.
				SpanBuilderImpl builder = tracer.buildSpan(null, References.FOLLOWS_FROM, true);
				builder.withTag(ExtraTags.PROPAGATION_TYPE, PropagationType.PROCESS.toString());
				builder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
				builder.withTag(ExtraTags.INSPECTT_METHOD_ID, methodId);
				builder.withTag(ExtraTags.INSPECTT_SENSOR_ID, sensorTypeId);

				// Build the span that will be injected into the SpanStore parameter.
				// Note: The contained span will be started and stopped when the function parameter is started and finished.
				SpanImpl span = builder.build();
				spanStore.storeSpan(span);
				
				asyncSpanContextListener.asyncSpanContextCreated(span.context());
			}
		}
		REF_MARKER.markCall();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void firstAfterBody(long methodId, long sensorTypeId, Object object, Object[] parameters, Object result, boolean exception, RegisteredSensorConfig rsc) {
		REF_MARKER.markEndCall();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void secondAfterBody(ICoreService coreService, long methodId, long sensorTypeId, Object object, Object[] parameters, Object result, boolean exception, RegisteredSensorConfig rsc) { // NOCHK:8-params
		// check if in the right(first) invocation
		if (REF_MARKER.isMarkerSet() && REF_MARKER.matchesFirst()) {
			// call ended, remove the marker.
			REF_MARKER.remove();
			// nothing else to do here
		}
	}

	/**
	 * Gets the specified parameter object from the parameters. This method will consult the
	 * {@link SpecialSensorConfig} in order to find parameter index with the FQN of
     * the parameter type of interest.
	 *
	 * @param parameters
	 *            Parameters of method invocation.
	 * @param rsc
	 *            {@link RegisteredSensorConfig}
	 * @param typeFQN
	 * 			  The fully qualified name of the parameter type of interest.
	 * @return Parameter object or <code>null</code> if one can not be located.
	 */
	private Object getParameter(Object[] parameters, RegisteredSensorConfig rsc, String typeFQN) {
		int index = rsc.getParameterTypes().indexOf(typeFQN);
		if (index >= 0) {
			return parameters[index];
		}
		return null;
	}

	/**
	 * Sets the specified parameter object in the given parameters array. This method will consult the
	 * {@link SpecialSensorConfig} in order to find parameter index with the FQN of
     * the parameter type of interest.
	 *
	 * @param parameters
	 *            Parameters of method invocation.
	 * @param rsc
	 *            {@link RegisteredSensorConfig}
	 * @param typeFQN
	 * 			  The fully qualified name of the parameter type of interest.
	 * @param replacementParam
	 * 			  The replacement parameter that is to replace the specified parameter type.
	 */
	private void setParameter(Object[] parameters, RegisteredSensorConfig rsc, String typeFQN, SpanStore replacementParam) {
		int index = rsc.getParameterTypes().indexOf(typeFQN);
		if (index >= 0) {
			parameters[index] = replacementParam;
		}
	}
	
	/**
	 * Extracts a stracktrace out of the given throwable.
	 * @param t 
	 * 			The throwable containing the stacktrace. 
	 * @return The throwable's stacktrace.
	 */
	private String toStracktrace(final Throwable t) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return pw.toString();
	}
}
