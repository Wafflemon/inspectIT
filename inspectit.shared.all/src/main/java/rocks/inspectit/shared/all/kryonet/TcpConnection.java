package rocks.inspectit.shared.all.kryonet;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.debug;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.esotericsoftware.kryonet.KryoNetException;

import rocks.inspectit.shared.all.storage.nio.stream.ExtendedByteBufferOutputStream;
import rocks.inspectit.shared.all.storage.nio.stream.SocketExtendedByteBufferInputStream;
import rocks.inspectit.shared.all.storage.nio.stream.StreamProvider;

/**
 * <b>IMPORTANT:</b> The class code is copied/taken/based from
 * <a href="https://github.com/EsotericSoftware/kryonet">kryonet</a>. Original author is Nathan
 * Sweet. License info can be found
 * <a href="https://github.com/EsotericSoftware/kryonet/blob/master/license.txt">here</a>.
 *
 * @author Nathan Sweet <misc@n4te.com>
 */
@SuppressWarnings("all")
// NOCHKALL
class TcpConnection {
	static private final int IPTOS_LOWDELAY = 0x10;

	/**
	 * Amount of output streams to create in the empty queue.
	 */
	// Added by ISE
	private static final int MAX_OUTPUT_STREAMS = 3;

	/**
	 * {@link StreamProvider} for creating streams.
	 */
	// Added by ISE
	private StreamProvider streamProvider;

	/**
	 * Write lock for write synch.
	 */
	// Added by ISE
	private Lock writeReentrantLock = new ReentrantLock();

	/**
	 * Queue of {@link ExtendedByteBufferOutputStream}s to be sent to the socket channel.
	 */
	// Added by ISE
	private LinkedBlockingQueue<ExtendedByteBufferOutputStream> writeQueue = new LinkedBlockingQueue<ExtendedByteBufferOutputStream>();

	/**
	 * Queue of {@link ExtendedByteBufferOutputStream}s to be written to.
	 */
	// Added by ISE
	private LinkedBlockingQueue<ExtendedByteBufferOutputStream> idleQueue = new LinkedBlockingQueue<ExtendedByteBufferOutputStream>();

	/**
	 * {@link SocketExtendedByteBufferInputStream} to read data with.
	 */
	// Added by ISE
	private SocketExtendedByteBufferInputStream socketInputStream;

	SocketChannel socketChannel;
	int keepAliveMillis = 8000;
	final ByteBuffer readBuffer, writeBuffer;
	boolean bufferPositionFix;
	int timeoutMillis = 20000; // ISE: increased to 20s
	float idleThreshold = 0.1f;

	final IExtendedSerialization serialization; // Changed by ISE
	private SelectionKey selectionKey;
	private long lastWriteTime, lastReadTime;
	private int currentObjectLength;
	private final Object writeLock = new Object();

	// Changed by ISE: added StreamProvider
	public TcpConnection(IExtendedSerialization serialization, int writeBufferSize, int objectBufferSize, StreamProvider streamProvider) {
		this.serialization = serialization;
		this.streamProvider = streamProvider; // Added by ISE
		writeBuffer = ByteBuffer.allocate(writeBufferSize);
		readBuffer = ByteBuffer.allocate(objectBufferSize);
		readBuffer.flip();

		for (int i = 0; i < MAX_OUTPUT_STREAMS; i++) {
			try {
				idleQueue.add(streamProvider.getExtendedByteBufferOutputStream());
			} catch (IOException e) {
				throw new KryoNetException("Can not initalize the output streams for the TCP Connection.", e);
			}
		}
	}

	public SelectionKey accept(Selector selector, SocketChannel socketChannel) throws IOException {
		// writeBuffer.clear(); Commented out by ISE
		readBuffer.clear();
		readBuffer.flip();
		currentObjectLength = 0;
		try {
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);

			// Added by ISE
			socketInputStream = streamProvider.getSocketExtendedByteBufferInputStream(socketChannel);

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

			if (DEBUG) {
				debug("kryonet", "Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: " + socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();

			return selectionKey;
		} catch (IOException ex) {
			close();
			throw ex;
		}
	}

	public void connect(Selector selector, SocketAddress remoteAddress, int timeout) throws IOException {
		close();
		// writeBuffer.clear(); Commented out by ISE
		readBuffer.clear();
		readBuffer.flip();
		currentObjectLength = 0;
		try {
			SocketChannel socketChannel = selector.provider().openSocketChannel();
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);
			// socket.setTrafficClass(IPTOS_LOWDELAY);
			socket.connect(remoteAddress, timeout); // Connect using blocking mode for simplicity.
			socketChannel.configureBlocking(false);
			this.socketChannel = socketChannel;

			// Added by ISE
			socketInputStream = streamProvider.getSocketExtendedByteBufferInputStream(socketChannel);

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
			selectionKey.attach(this);

			if (DEBUG) {
				debug("kryonet", "Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: " + socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();
		} catch (IOException ex) {
			close();
			IOException ioEx = new IOException("Unable to connect to: " + remoteAddress);
			ioEx.initCause(ex);
			throw ioEx;
		}
	}

	public Object readObject(Connection connection) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) {
			throw new SocketException("Connection is closed.");
		}

		// Change by ISE from here to end of method

		// we use the read buffer to read the size of the length
		if (currentObjectLength == 0) {
			// Read the length of the next object from the socket.
			int lengthLength = serialization.getLengthLength();
			if (readBuffer.remaining() < lengthLength) {
				readBuffer.compact();
				int bytesRead = socketChannel.read(readBuffer);
				readBuffer.flip();
				if (bytesRead == -1) {
					throw new SocketException("Connection is closed.");
				}
				lastReadTime = System.currentTimeMillis();

				if (readBuffer.remaining() < lengthLength) {
					return null;
				}
			}
			currentObjectLength = serialization.readLength(readBuffer);

			if (currentObjectLength <= 0) {
				throw new KryoNetException("Invalid object length: " + currentObjectLength);
			}
		}

		int length = currentObjectLength;
		// reset stream
		socketInputStream.reset(length);

		currentObjectLength = 0;

		// read object
		Object object;
		try {
			object = serialization.read(connection, socketInputStream);
		} catch (Exception ex) {
			throw new KryoNetException("Error during deserialization.", ex);
		}

		lastReadTime = System.currentTimeMillis();
		return object;
	}

	// Changed completely by ISE
	public void writeOperation() throws IOException {
		writeReentrantLock.lock();
		try {
			if (writeToSocket()) {
				// Write successful, clear OP_WRITE.
				selectionKey.interestOps(SelectionKey.OP_READ);
			}
			lastWriteTime = System.currentTimeMillis();
		} finally {
			writeReentrantLock.unlock();
		}
	}

	private boolean writeToSocket() throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) {
			throw new SocketException("Connection is closed.");
		}

		// Change by ISE from here to end of method

		outerloop: while (!writeQueue.isEmpty()) {
			ExtendedByteBufferOutputStream outputStream = writeQueue.peek();
			if (null == outputStream) {
				break;
			}

			// calculate how much we have already written
			try {
				long written = 0;
				List<ByteBuffer> buffers = outputStream.getAllByteBuffers();
				for (ByteBuffer buffer : buffers) {
					written += buffer.position();
				}

				// then try to write until the end
				while (written < outputStream.getTotalWriteSize()) {
					long writeSize = socketChannel.write(buffers.toArray(new ByteBuffer[buffers.size()]));
					if (0 == writeSize) {
						// if we can not write any more we go out
						break outerloop;
					}
					written += writeSize;
				}
			} finally {
				// here we have done with this output stream
				// remove it from the write queue, prepare for new usage and return to the idle
				// queue
				writeQueue.remove(outputStream);
				outputStream.prepare();
				idleQueue.offer(outputStream);
			}
		}

		return writeQueue.isEmpty();
	}

	/** This method is thread safe. */
	public int send(Connection connection, Object object) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) {
			throw new SocketException("Connection is closed.");
		}

		// Change by ISE from here to end of method

		// acquire the output stream to write from the queue
		ExtendedByteBufferOutputStream outputStream = null;
		try {
			outputStream = idleQueue.poll(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Sending was interrupted.");
		}

		if (null == outputStream) {
			throw new IOException("Sending failed. Timeout acquiring output stream for writing.");
		}

		// we are locking here as the serialization is not thread-safe
		writeReentrantLock.lock();
		boolean streamAddedToWriteQueue = false;
		try {
			int lengthLength = serialization.getLengthLength();
			// make space for the length
			// just write empty byte array in correct size
			outputStream.write(new byte[lengthLength]);

			// Write data and flush when done
			try {
				serialization.write(connection, outputStream, object);
			} catch (KryoNetException ex) { // NOPMD
				throw new KryoNetException("Error serializing object of type: " + object.getClass().getName(), ex);
			}
			outputStream.flush(false);

			// rewrite the size to the first buffer
			long writeSize = outputStream.getTotalWriteSize() - lengthLength;
			ByteBuffer buffer = outputStream.getAllByteBuffers().iterator().next();
			int position = buffer.position();
			buffer.position(0);
			serialization.writeLength(buffer, (int) writeSize);
			buffer.position(position);

			// Write to socket if no data was queued.
			boolean hasQueuedData = hasQueuedData();
			writeQueue.add(outputStream);

			// mark as added to write queue
			streamAddedToWriteQueue = true;

			if (!hasQueuedData && !writeToSocket()) {
				// A partial write, set OP_WRITE to be notified when more writing can occur.
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} else {
				// Full write, wake up selector so idle event will be fired.
				selectionKey.selector().wakeup();
			}

			lastWriteTime = System.currentTimeMillis();
			return (int) writeSize;
		} finally {
			if (!streamAddedToWriteQueue) {
				outputStream.prepare();
				idleQueue.offer(outputStream);
			}
			writeReentrantLock.unlock();
		}
	}

	/**
	 * @return Returns if any data is queued for writing.
	 */
	// Added by ISE
	private boolean hasQueuedData() {
		return !writeQueue.isEmpty();
	}

	public void close() {
		try {
			if (socketChannel != null) {
				socketChannel.close();
				socketChannel = null;
				if (selectionKey != null) {
					selectionKey.selector().wakeup();
				}
			}

			// Added by ISE Start

			// close input stream
			if (null != socketInputStream) {
				socketInputStream.close();
			}

			// and all output streams
			while (!writeQueue.isEmpty()) {
				ExtendedByteBufferOutputStream outputStream = writeQueue.poll();
				if (null != outputStream) {
					outputStream.close();
				}
			}

			// Added by ISE End
		} catch (IOException ex) {
			if (DEBUG) {
				debug("kryonet", "Unable to close TCP connection.", ex);
			}
		}
	}

	public boolean needsKeepAlive(long time) {
		return (socketChannel != null) && (keepAliveMillis > 0) && ((time - lastWriteTime) > keepAliveMillis);
	}

	public boolean isTimedOut(long time) {
		return (socketChannel != null) && (timeoutMillis > 0) && ((time - lastReadTime) > timeoutMillis);
	}

	/**
	 * Returns current size to be written.
	 *
	 * @return Current size to be written.
	 */
	// Added by ISE
	public int getWriteBuffersSize() {
		if (writeQueue.isEmpty()) {
			return 0;
		} else {
			int size = 0;
			for (ExtendedByteBufferOutputStream stream : writeQueue) {
				size += stream.getTotalWriteSize();
			}
			return size;
		}
	}
}
