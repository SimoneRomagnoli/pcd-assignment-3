package part2.rmi.puzzle;

import java.awt.Image;

class Tile extends SerializableTile{
	private Image image;

    public Tile(Image imagePortion, int position, int integer, int selectBy ) {
        super(position, integer, selectBy);
        this.image = imagePortion;
    }

    public Image getImage() {
    	return image;
    }

    public SerializableTile getSerializableTile() {
        return new SerializableTile(this.getOriginalPosition(), this.getCurrentPosition(), this.getSelection());
    }
}
