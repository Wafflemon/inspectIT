package rocks.inspectit.shared.cs.ci.sensor.method.impl;

import javax.xml.bind.annotation.XmlRootElement;

import rocks.inspectit.shared.cs.ci.sensor.method.AbstractRemoteSensorConfig;

/**
 * Configuration for the
 * {@link rocks.inspectit.agent.java.sensor.method.special.CompletableFutureClientSensor}.
 *
 * @author Jacob Waffle
 *
 */
@XmlRootElement(name = "completable-future-client-sensor-config")
public final class CompletableFutureClientSensorConfig extends AbstractRemoteSensorConfig {

	/**
	 * Sensor name.
	 */
	private static final String SENSOR_NAME = "CompletableFuture Client Sensor";

	/**
	 * Implementing class name.
	 */
	public static final String CLASS_NAME = "rocks.inspectit.agent.java.sensor.method.async.future.CompletableFutureClientSensor";

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
  public boolean isServerSide()
  {
    return false;
  }
}
