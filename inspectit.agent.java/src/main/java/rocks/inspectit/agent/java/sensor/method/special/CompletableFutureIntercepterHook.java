package rocks.inspectit.agent.java.sensor.method.special;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import rocks.inspectit.agent.java.config.impl.SpecialSensorConfig;
import rocks.inspectit.agent.java.hooking.ISpecialHook;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;
import rocks.inspectit.agent.java.tracing.core.async.SpanStore;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreBiConsumer;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreBiFunction;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreConsumer;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreFunction;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreRunnable;
import rocks.inspectit.agent.java.tracing.core.async.function.SpanStoreSupplier;

/**
 * Hook that intercepts {@link CompletableFuture} methods to substitute the given {@link Runnable} variants 
 * with {@link SpanStore}s.
 *
 * @author Jacob Waffle
 *
 */
@SuppressWarnings({"rawtypes", "We don't care about generics."})
public class CompletableFutureIntercepterHook implements ISpecialHook {

	/**
	 * The fully qualified name for {@link Runnable}s.
	 */
	private static final String RUNNABLE_FQN    = "java.lang.Runnable";

	/**
	 * The fully qualified name for {@link BiFunction}s.
	 */
	private static final String BI_FUNCTION_FQN = "java.util.function.BiFunction";
	
	/**
	 * The fully qualified name for {@link BiConsumer}s.
	 */
	private static final String BI_CONSUMER_FQN = "java.util.function.BiConsumer";

	/**
	 * The fully qualified name for {@link Consumer}s.
	 */
	private static final String CONSUMER_FQN    = "java.util.function.Consumer";

	/**
	 * The fully qualified name for {@link Function}s.
	 */
	private static final String FUNCTION_FQN    = "java.util.function.Function";

	/**
	 * The fully qualified name for {@link Supplier}s.
	 */
	private static final String SUPPLIER_FQN    = "java.util.function.Supplier";
	
	/**
	 * The tracer.
	 */
	private TracerImpl tracer;

	/**
	 * Constructor.
	 *
	 * @param tracer
	 *            the trader to use
	 */
	public CompletableFutureIntercepterHook(TracerImpl tracer) {
		this.tracer = tracer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object beforeBody(long methodId, Object object, Object[] parameters, SpecialSensorConfig ssc) {
		if (!tracer.isCurrentContextExisting()) {
			return null;
		}
		
		Runnable   runnable   = (Runnable) getParameter(parameters, ssc, RUNNABLE_FQN);
		BiFunction biFunction = (BiFunction) getParameter(parameters, ssc, BI_FUNCTION_FQN);
		BiConsumer biConsumer = (BiConsumer) getParameter(parameters, ssc, BI_CONSUMER_FQN);
		Consumer   consumer   = (Consumer) getParameter(parameters, ssc, CONSUMER_FQN);
		Function   function   = (Function) getParameter(parameters, ssc, FUNCTION_FQN);
		Supplier   supplier   = (Supplier) getParameter(parameters, ssc, SUPPLIER_FQN);
		
		if (null != runnable) {
			setParameter(parameters, ssc, RUNNABLE_FQN, new SpanStoreRunnable(runnable)); 
		} else if (null != biFunction) {
			setParameter(parameters, ssc, BI_FUNCTION_FQN, new SpanStoreBiFunction(biFunction));
		} else if (null != biConsumer) {
			setParameter(parameters, ssc, BI_CONSUMER_FQN, new SpanStoreBiConsumer(biConsumer));
		} else if (null != consumer) {
			setParameter(parameters, ssc, CONSUMER_FQN, new SpanStoreConsumer(consumer));
		} else if (null != function) {
			setParameter(parameters, ssc, FUNCTION_FQN, new SpanStoreFunction(function));
		} else if (null != supplier) {
			setParameter(parameters, ssc, SUPPLIER_FQN, new SpanStoreSupplier(supplier));
		}
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object afterBody(long methodId, Object object, Object[] parameters, Object result, SpecialSensorConfig ssc) {
		return null;
	}

	/**
	 * Gets the specified parameter object from the parameters. This method will consult the
	 * {@link SpecialSensorConfig} in order to find parameter index with the FQN of
     * the parameter type of interest.
	 *
	 * @param parameters
	 *            Parameters of method invocation.
	 * @param ssc
	 *            {@link SpecialSensorConfig}
	 * @param typeFQN
	 * 			  The fully qualified name of the parameter type of interest.
	 * @return Parameter object or <code>null</code> if one can not be located.
	 */
	private Object getParameter(Object[] parameters, SpecialSensorConfig ssc, String typeFQN) {
		int index = ssc.getParameterTypes().indexOf(typeFQN);
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
	 * @param ssc
	 *            {@link SpecialSensorConfig}
	 * @param typeFQN
	 * 			  The fully qualified name of the parameter type of interest.
	 * @param replacementParam
	 * 			  The replacement parameter that is to replace the specified parameter type.
	 */
	private void setParameter(Object[] parameters, SpecialSensorConfig ssc, String typeFQN, SpanStore replacementParam) {
		int index = ssc.getParameterTypes().indexOf(typeFQN);
		if (index >= 0) {
			parameters[index] = replacementParam;
		}
	}
}
