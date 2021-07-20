package part2.rmi.puzzle;

import java.awt.Image;

/**
 * Represents a tile of the board.
 *
 */
class Tile extends SerializableTile {
	private final Image image;

    public Tile(Image imagePortion, int position, int integer, int selectBy ) {
        super(position, integer, selectBy);
        this.image = imagePortion;
    }

    public Image getImage() {
    	return image;
    }

    /**
     * Get a serializable version of this tile.
     * @return a serializable copy of the tile.
     */
    public SerializableTile getSerializableTile() {
        return new SerializableTile(this.getOriginalPosition(), this.getCurrentPosition(), this.getSelection());
    }
}
