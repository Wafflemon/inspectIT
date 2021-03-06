package rocks.inspectit.shared.cs.indexing.buffer.impl;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import rocks.inspectit.shared.all.communication.DefaultData;
import rocks.inspectit.shared.cs.indexing.AbstractBranch;
import rocks.inspectit.shared.cs.indexing.ITreeComponent;
import rocks.inspectit.shared.cs.indexing.buffer.IBufferBranchIndexer;
import rocks.inspectit.shared.cs.indexing.buffer.IBufferTreeComponent;

/**
 * {@link Branch} is a {@link ITreeComponent} that holds references to other {@link ITreeComponent}
 * s, which are actually branch children.
 *
 * @author Ivan Senic
 *
 * @param <E>
 *            Element type that the branch can index (and hold).
 */
public class Branch<E extends DefaultData> extends AbstractBranch<E, E> implements IBufferTreeComponent<E> {

	/**
	 * Buffer branch indexer.
	 */
	private IBufferBranchIndexer<E> bufferBranchIndexer;

	/**
	 * Default constructor.
	 *
	 * @param bufferBranchIndexer
	 *            Indexer.
	 */
	public Branch(IBufferBranchIndexer<E> bufferBranchIndexer) {
		super(bufferBranchIndexer);
		this.bufferBranchIndexer = bufferBranchIndexer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ITreeComponent<E, E> getNextTreeComponent(E element) {
		return bufferBranchIndexer.getNextTreeComponent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void cleanWithRunnable(ExecutorService executorService) {
		for (Entry<Object, ITreeComponent<E, E>> entry : getComponentMap().entrySet()) {
			if (entry.getValue() instanceof IBufferTreeComponent) {
				((IBufferTreeComponent<E>) entry.getValue()).cleanWithRunnable(executorService);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean clean() {
		ArrayList<Object> keysToRemove = new ArrayList<>();
		for (Entry<Object, ITreeComponent<E, E>> entry : getComponentMap().entrySet()) {
			if (entry.getValue() instanceof IBufferTreeComponent) {
				boolean toClear = ((IBufferTreeComponent<E>) entry.getValue()).clean();
				if (toClear) {
					keysToRemove.add(entry.getKey());
				}
			}
		}
		for (Object key : keysToRemove) {
			getComponentMap().remove(key);
		}

		if (getComponentMap().isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean clearEmptyComponents() {
		ArrayList<Object> keysToRemove = new ArrayList<>();
		for (Entry<Object, ITreeComponent<E, E>> entry : getComponentMap().entrySet()) {
			if (entry.getValue() instanceof IBufferTreeComponent) {
				boolean toClear = ((IBufferTreeComponent<E>) entry.getValue()).clearEmptyComponents();
				if (toClear) {
					keysToRemove.add(entry.getKey());
				}
			}
		}
		for (Object key : keysToRemove) {
			getComponentMap().remove(key);
		}

		if (getComponentMap().isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getNumberOfElements() {
		long sum = 0;
		for (ITreeComponent<E, E> treeComponent : getComponentMap().values()) {
			if (treeComponent instanceof IBufferTreeComponent) {
				sum += ((IBufferTreeComponent<E>) treeComponent).getNumberOfElements();
			}
		}
		return sum;
	}

	/**
	 * @return the bufferBranchIndexer
	 */
	public IBufferBranchIndexer<E> getBufferBranchIndexer() {
		return bufferBranchIndexer;
	}
}
