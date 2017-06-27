package boombox.android;

import boombox.android.proto.LaunchTube;

import java.util.Iterator;

public class LaunchTubeGroup  implements Iterable<LaunchTube> {

	private final LaunchTube[][] matrix;
	private final int rows;
	private final int cols;
	private final int size;

	public LaunchTubeGroup(int cols, int rows) {
		this.cols = cols;
		this.rows = rows;
		this.size = rows * cols;
		matrix = new LaunchTube[cols][rows];
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				matrix[x][y] = new LaunchTube();
				matrix[x][y].setPosition((byte) (y * cols + x));
			}
		}
	}

	public int getSize() {
		return size;
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public LaunchTube[][] getMatrix() {
		return matrix;
	}

	public LaunchTube getAt(int position) {
		int row = position / cols;
		int col = position - (row * cols);
		return matrix[col][row];
	}

	@Override
	public Iterator<LaunchTube> iterator() {
		return new Iter();
	}

	private final class Iter implements Iterator<LaunchTube> {
		private int pos;

		@Override
		public boolean hasNext() {
			return pos < getSize();
		}

		@Override
		public LaunchTube next() {
			return getAt(pos++);
		}

		@Override
		public void remove() {

		}
	}
}
