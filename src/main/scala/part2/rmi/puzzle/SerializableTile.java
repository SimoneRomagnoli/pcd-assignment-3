package part2.rmi.puzzle;

import java.io.Serializable;

/**
 * Represents a tile of the puzzle board that can be read and updated by a remote object.
 *
 */
public class SerializableTile implements Comparable<SerializableTile>, Serializable {

    private final int originalPosition;
    private int currentPosition;
    private int selectedBy;

    public SerializableTile(final int originalPosition, final int currentPosition, int selectedBy) {
        this.originalPosition = originalPosition;
        this.currentPosition = currentPosition;
        this.selectedBy  = selectedBy;
    }

    public boolean isInRightPlace() {
        return currentPosition == originalPosition;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getOriginalPosition() {
        return originalPosition; }

    public void setCurrentPosition(final int newPosition) {
        currentPosition = newPosition;
    }

    public void select(final int id) {
        this.selectedBy = id;
    }

    public int getSelection(){
        return this.selectedBy;
    }

    public boolean alreadySelected() {
        return this.selectedBy != 0;
    }

    public void unselect() {
        this.selectedBy = 0;
    }

    public int compareTo(SerializableTile other) {
        return this.currentPosition < other.currentPosition ? -1 : (this.currentPosition == other.currentPosition ? 0 : 1);
    }

    @Override
    public String toString() {
        return "[ORIGINAL: "+originalPosition+", CURRENT: "+currentPosition+", SELECTED-BY: "+selectedBy+"]";
    }
}
