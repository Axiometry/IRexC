package me.axiometry.irexc.event;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.*;

import org.junit.*;

public class ConcurrentEventBusTest {
	private ConcurrentEventBus eventBus;

	@Before
	public void setUp() throws Exception {
		eventBus = new ConcurrentEventBus();
	}

	@Test
	public void testRegistration() {
		// @formatter:off
		class TestEvent extends AbstractEvent {}
		class TestEvent1 extends TestEvent {}
		class TestEvent2 extends TestEvent {}
		EventListener listener1 = new EventListener() {
			@EventHandler
			public void onTest1(TestEvent1 event) {}
		};
		EventListener listener2 = new EventListener() {
			@EventHandler
			public void onTest2(TestEvent2 event) {}
		};
		// @formatter:on

		eventBus.register(listener1);
		assertEquals(1, eventBus.getListeners(TestEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent2.class).length);

		eventBus.register(listener2);
		assertEquals(2, eventBus.getListeners(TestEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent2.class).length);

		eventBus.unregister(listener1);
		assertEquals(1, eventBus.getListeners(TestEvent.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent2.class).length);

		eventBus.unregister(listener2);
		assertEquals(0, eventBus.getListeners(TestEvent.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent2.class).length);

		eventBus.register(listener1);
		eventBus.register(listener2);
		assertEquals(2, eventBus.getListeners(TestEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(1, eventBus.getListeners(TestEvent2.class).length);

		eventBus.clearListeners();
		assertEquals(0, eventBus.getListeners(TestEvent.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent1.class).length);
		assertEquals(0, eventBus.getListeners(TestEvent2.class).length);
	}

	@Test
	public void testEvents() {
		// @formatter:off
		@SuppressWarnings("serial")
		class TestException extends RuntimeException {
			public TestException(String message) { super(message); }
		}
		class TestEvent extends AbstractEvent {}
		class TestAsyncEvent extends TestEvent {}
		class TestNoErrorEvent extends TestEvent {}
		class TestWithErrorEvent extends TestEvent {}
		// @formatter:on

		final Thread thread = Thread.currentThread();
		final AtomicInteger eventCounter = new AtomicInteger();
		final AtomicInteger asyncCounter = new AtomicInteger();
		final AtomicInteger noErrorCounter = new AtomicInteger();
		final AtomicInteger withErrorCounter = new AtomicInteger();
		final AtomicReference<Thread> asyncThread = new AtomicReference<>();

		EventListener listener = new EventListener() {
			@EventHandler
			public void onTest(TestEvent event) {
				eventCounter.incrementAndGet();
			}
			@EventHandler
			public void onTestAsync(TestAsyncEvent event) {
				assertTrue(asyncThread.compareAndSet(null, Thread.currentThread()));
				asyncCounter.incrementAndGet();
			}
			@EventHandler
			public void onTestNoError(TestNoErrorEvent event) {
				noErrorCounter.incrementAndGet();
				throw new TestException("This should not be seen");
			}
			@EventHandler
			public void onTestWithError(TestWithErrorEvent event) {
				withErrorCounter.incrementAndGet();
				throw new TestException("This should be seen");
			}
		};
		eventBus.register(listener);

		assertEquals(1, eventBus.getListeners(TestEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestAsyncEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestNoErrorEvent.class).length);
		assertEquals(1, eventBus.getListeners(TestWithErrorEvent.class).length);

		assertEquals(listener, eventBus.getListeners(TestEvent.class)[0]);
		assertEquals(listener, eventBus.getListeners(TestAsyncEvent.class)[0]);
		assertEquals(listener, eventBus.getListeners(TestNoErrorEvent.class)[0]);
		assertEquals(listener, eventBus.getListeners(TestWithErrorEvent.class)[0]);

		eventBus.fire(new TestEvent());

		assertEquals(1, eventCounter.get());
		assertEquals(0, asyncCounter.get());
		assertEquals(0, noErrorCounter.get());
		assertEquals(0, withErrorCounter.get());

		eventBus.fire(new TestNoErrorEvent());

		assertEquals(2, eventCounter.get());
		assertEquals(0, asyncCounter.get());
		assertEquals(1, noErrorCounter.get());
		assertEquals(0, withErrorCounter.get());

		eventBus.fireAsync(new TestAsyncEvent());

		for(int i = 0; asyncCounter.get() == 0 && i < 3; i++) {
			try {
				Thread.sleep(500);
			} catch(InterruptedException exception) {
				fail("Interrupted");
			}
		}

		assertEquals(3, eventCounter.get());
		assertEquals(1, asyncCounter.get());
		assertEquals(1, noErrorCounter.get());
		assertEquals(0, withErrorCounter.get());

		assertNotNull(asyncThread.get());
		assertNotEquals(thread, asyncThread.get());

		try {
			eventBus.fireWithError(new TestWithErrorEvent());
			fail("Error should have occurred");
		} catch(EventFireException exception) {
			assertEquals(1, exception.getExceptions().length);
			assertEquals(TestWithErrorEvent.class, exception.getExceptions()[0].getEvent().getClass());
			assertEquals(InvocationTargetException.class, exception.getExceptions()[0].getCause().getClass());
			assertEquals(TestException.class, exception.getExceptions()[0].getCause().getCause().getClass());
		}

		assertEquals(4, eventCounter.get());
		assertEquals(1, asyncCounter.get());
		assertEquals(1, noErrorCounter.get());
		assertEquals(1, withErrorCounter.get());
	}

	@After
	public void tearDown() throws Exception {
		eventBus.clearListeners();
		eventBus = null;
	}
}
