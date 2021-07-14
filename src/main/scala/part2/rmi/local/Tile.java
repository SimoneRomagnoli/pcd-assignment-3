package part2.rmi.local;

import java.awt.Image;

class Tile implements Comparable<Tile>{
	private Image image;
	private int originalPosition;
	private int currentPosition;
	private int selectedBy;

    public Tile(final Image image, final int originalPosition, final int currentPosition) {
        this.image = image;
        this.originalPosition = originalPosition;
        this.currentPosition = currentPosition;
        this.selectedBy = 0;
    }
    
    public Image getImage() {
    	return image;
    }
    
    public boolean isInRightPlace() {
    	return currentPosition == originalPosition;
    }
    
    public int getCurrentPosition() {
    	return currentPosition;
    }
    
    public void setCurrentPosition(final int newPosition) {
    	currentPosition = newPosition;
    }

    public void select(final int id) {
        this.selectedBy = id;
    }

    public void unselect() {
        this.selectedBy = 0;
    }

	@Override
	public int compareTo(Tile other) {
		return this.currentPosition < other.currentPosition ? -1 
				: (this.currentPosition == other.currentPosition ? 0 : 1);
	}
}
