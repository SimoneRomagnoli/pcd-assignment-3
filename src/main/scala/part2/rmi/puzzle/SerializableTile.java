package part2.rmi.puzzle;

import java.awt.*;
import java.io.Serializable;

public class SerializableTile implements Comparable<SerializableTile>, Serializable {

    private int originalPosition;
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

    public int getOriginalPosition(){ return originalPosition; }

    public void setCurrentPosition(final int newPosition) {
        currentPosition = newPosition;
    }

    public void select(final int id) {
        this.selectedBy = id;
    }

    public int getSelection(){
        return this.selectedBy;
    }

    public boolean alreadySelected(){
        return this.selectedBy != 0;
    }

    public void unselect() {
        this.selectedBy = 0;
    }

    public int compareTo(SerializableTile other) {
        return this.currentPosition < other.currentPosition ? -1 : (this.currentPosition == other.currentPosition ? 0 : 1);
    }
}
