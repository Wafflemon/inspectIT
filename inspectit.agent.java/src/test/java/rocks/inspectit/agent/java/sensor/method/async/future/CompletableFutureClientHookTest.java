package rocks.inspectit.agent.java.sensor.method.async.future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Test;

import io.opentracing.References;
import io.opentracing.tag.Tags;
import rocks.inspectit.agent.java.config.impl.RegisteredSensorConfig;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.SpanBuilderImpl;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.SpanContextImpl;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.SpanImpl;
import rocks.inspectit.agent.java.sdk.opentracing.internal.impl.TracerImpl;
import rocks.inspectit.agent.java.tracing.core.async.executor.SpanStoreRunnable;
import rocks.inspectit.agent.java.tracing.core.listener.IAsyncSpanContextListener;
import rocks.inspectit.shared.all.testbase.TestBase;
import rocks.inspectit.shared.all.tracing.constants.ExtraTags;
import rocks.inspectit.shared.all.tracing.data.PropagationType;

/**
 * Tests the {@link CompletableFutureClientHook] class.
 *
 * @author Jacob Waffle
 *
 */
@SuppressWarnings({"unchecked"})
public class CompletableFutureClientHookTest extends TestBase {

	@InjectMocks
	CompletableFutureClientHook hook;

	@Mock
	RegisteredSensorConfig rsc;

	@Mock
	Object targetObject;

	@Mock
	Object result;

	@Mock
	TracerImpl tracer;

	@Mock
	IAsyncSpanContextListener asyncListener;

	/**
	 * Tests the
	 * {@link CompletableFutureClientHook#beforeBody(long, long, Object, Object[], rocks.inspectit.agent.java.config.impl.RegisteredSensorConfig)}
	 * method.
	 *
	 */
	public static class BeforeBody extends CompletableFutureClientHookTest {

		@Test
		public void happyPath() throws Exception {
			Runnable runnable = mock(Runnable.class);
			Runnable[] parameters = new Runnable[] { runnable };
			when(rsc.getParameterTypes()).thenReturn(Collections.singletonList("java.lang.Runnable"));
			SpanBuilderImpl builder = mock(SpanBuilderImpl.class);
			SpanImpl span = mock(SpanImpl.class);
			SpanContextImpl spanContext = mock(SpanContextImpl.class);
			when(span.context()).thenReturn(spanContext);
			when(tracer.buildSpan(null, References.FOLLOWS_FROM, true)).thenReturn(builder);
			when(builder.build()).thenReturn(span);
			when(tracer.isCurrentContextExisting()).thenReturn(true);

			hook.beforeBody(1L, 2L, targetObject, parameters, rsc);
			parameters[0].run();
			hook.firstAfterBody(1L, 2L, targetObject, parameters, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters, spanContext, false, rsc);

			verify(tracer).buildSpan(null, References.FOLLOWS_FROM, true);
			verify(builder).withTag(ExtraTags.PROPAGATION_TYPE, PropagationType.PROCESS.toString());
			verify(builder).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
			verify(builder).withTag(ExtraTags.INSPECTT_METHOD_ID, 1L);
			verify(builder).withTag(ExtraTags.INSPECTT_SENSOR_ID, 2L);
			verify(builder).build();
			assertThat(parameters[0], isA((Class) SpanStoreRunnable.class));
			verify(span, times(1)).start();
			verify(span, times(1)).finish();
			verify(span, times(1)).context();
			verify(span, times(1)).isStarted();
			verify(span, times(1)).isFinished();
			verify(asyncListener).asyncSpanContextCreated(spanContext);
			verifyNoMoreInteractions(tracer, builder, asyncListener);
		}

		@Test
		public void noParameters() {
			Object[] parameters = new Object[] {};

			hook.beforeBody(0, 0, targetObject, parameters, rsc);

			hook.firstAfterBody(1L, 2L, targetObject, parameters, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters, null, false, rsc);

			verifyZeroInteractions(targetObject, rsc, asyncListener);
		}

		@Test
		public void nestedCalls() throws Exception {
			Runnable runnable1 = mock(Runnable.class);
			Runnable runnable2 = mock(Runnable.class);
			Runnable[] parameters1 = new Runnable[] { runnable1 };
			Runnable[] parameters2 = new Runnable[] { runnable2 };
			when(rsc.getParameterTypes()).thenReturn(Collections.singletonList("java.lang.Runnable")).thenReturn(Collections.singletonList("java.lang.Runnable"));
			SpanBuilderImpl builder = mock(SpanBuilderImpl.class);
			SpanImpl span = mock(SpanImpl.class);
			SpanContextImpl spanContext = mock(SpanContextImpl.class);
			when(span.context()).thenReturn(spanContext);
			when(tracer.buildSpan(null, References.FOLLOWS_FROM, true)).thenReturn(builder);
			when(builder.build()).thenReturn(span);
			when(tracer.isCurrentContextExisting()).thenReturn(true);

			hook.beforeBody(1L, 2L, targetObject, parameters1, rsc);

			hook.beforeBody(1L, 2L, targetObject, parameters2, rsc);
			parameters2[0].run();
			hook.firstAfterBody(1L, 2L, targetObject, parameters2, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters2, spanContext, false, rsc);
			
			parameters1[0].run();
			hook.firstAfterBody(1L, 2L, targetObject, parameters1, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters1, spanContext, false, rsc);

			verify(tracer).buildSpan(null, References.FOLLOWS_FROM, true);
			verify(builder).withTag(ExtraTags.PROPAGATION_TYPE, PropagationType.PROCESS.toString());
			verify(builder).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
			verify(builder).withTag(ExtraTags.INSPECTT_METHOD_ID, 1L);
			verify(builder).withTag(ExtraTags.INSPECTT_SENSOR_ID, 2L);
			verify(builder).build();
			assertThat(parameters1[0], isA((Class) SpanStoreRunnable.class));
			assertThat(parameters2[0], isA((Class) Runnable.class));
			verify(span, times(1)).start();
			verify(span, times(1)).finish();
			verify(span, times(1)).context();
			verify(span, times(1)).isStarted();
			verify(span, times(1)).isFinished();
			verify(asyncListener).asyncSpanContextCreated(spanContext);
			verifyNoMoreInteractions(tracer, builder, asyncListener);
		}

		@Test
		public void consecutiveCalls() throws Exception {
			Runnable runnable1 = mock(Runnable.class);
			Runnable runnable2 = mock(Runnable.class);
			Runnable[] parameters1 = new Runnable[] { runnable1 };
			Runnable[] parameters2 = new Runnable[] { runnable2 };
			when(rsc.getParameterTypes()).thenReturn(Collections.singletonList("java.lang.Runnable")).thenReturn(Collections.singletonList("java.lang.Runnable"));
			SpanBuilderImpl builder = mock(SpanBuilderImpl.class);
			SpanImpl span = mock(SpanImpl.class);
			SpanContextImpl spanContext = mock(SpanContextImpl.class);
			when(span.context()).thenReturn(spanContext);
			when(tracer.buildSpan(null, References.FOLLOWS_FROM, true)).thenReturn(builder);
			when(builder.build()).thenReturn(span);
			when(tracer.isCurrentContextExisting()).thenReturn(true);

			hook.beforeBody(1L, 2L, targetObject, parameters1, rsc);
			parameters1[0].run();
			hook.firstAfterBody(1L, 2L, targetObject, parameters1, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters1, spanContext, false, rsc);

			hook.beforeBody(1L, 2L, targetObject, parameters2, rsc);
			parameters2[0].run();
			hook.firstAfterBody(1L, 2L, targetObject, parameters2, result, false, rsc);
			hook.secondAfterBody(null, 1L, 2L, targetObject, parameters2, spanContext, false, rsc);

			verify(tracer, times(2)).buildSpan(null, References.FOLLOWS_FROM, true);
			verify(builder, times(2)).withTag(ExtraTags.PROPAGATION_TYPE, PropagationType.PROCESS.toString());
			verify(builder, times(2)).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
			verify(builder, times(2)).withTag(ExtraTags.INSPECTT_METHOD_ID, 1L);
			verify(builder, times(2)).withTag(ExtraTags.INSPECTT_SENSOR_ID, 2L);
			verify(builder, times(2)).build();
			assertThat(parameters1[0], isA((Class) SpanStoreRunnable.class));
			assertThat(parameters2[0], isA((Class) SpanStoreRunnable.class));
			verify(span, times(2)).start();
			verify(span, times(2)).finish();
			verify(span, times(2)).context();
			verify(span, times(2)).isStarted();
			verify(span, times(2)).isFinished();
			verify(asyncListener, times(2)).asyncSpanContextCreated(spanContext);
			verifyNoMoreInteractions(tracer, builder, asyncListener);
		}
	}
}
