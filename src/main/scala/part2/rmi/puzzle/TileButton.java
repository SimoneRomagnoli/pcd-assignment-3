package part2.rmi.puzzle;

import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * A button overlapped to the tile in order to create a listener for the tile.
 *
 */
public class TileButton extends JButton{

	public TileButton(final Tile tile) {
		super(new ImageIcon(tile.getImage()));
	}
}
