package rocks.inspectit.shared.cs.ci.sensor.method.special.impl;

import javax.xml.bind.annotation.XmlTransient;

import rocks.inspectit.shared.all.instrumentation.config.impl.SubstitutionDescriptor;
import rocks.inspectit.shared.cs.ci.sensor.method.special.AbstractSpecialMethodSensorConfig;

/**
 * Configuration for the
 * {@link rocks.inspectit.agent.java.sensor.method.special.CompletableFutureInterceptorSensor}.
 *
 * @author Jacob Waffle
 *
 */
@XmlTransient
public final class CompletableFutureIntercepterSensorConfig extends AbstractSpecialMethodSensorConfig {

	/**
	 * Sensor name.
	 */
	private static final String SENSOR_NAME = "CompletableFuture Interceptor Sensor";

	/**
	 * Implementing class name.
	 */
	public static final String CLASS_NAME = "rocks.inspectit.agent.java.sensor.method.special.CompletableFutureIntercepterSensor";

	/**
	 * Singleton instance to use.
	 */
	public static final CompletableFutureIntercepterSensorConfig INSTANCE = new CompletableFutureIntercepterSensorConfig();

	/**
	 * Private constructor, use {@link #INSTANCE}.
	 */
	private CompletableFutureIntercepterSensorConfig() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return SENSOR_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SubstitutionDescriptor getSubstitutionDescriptor() {
		return new SubstitutionDescriptor(false, true);
	}
}
