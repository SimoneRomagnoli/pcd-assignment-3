package part2.rmi.remotes;

import javafx.beans.Observable;
import part2.rmi.puzzle.SerializableTile;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BoardStatusImpl implements BoardStatus {

    private List<SerializableTile> tiles;
    private List<RemoteObserver> observers;

    public BoardStatusImpl(List<SerializableTile> tiles) {
        this.tiles = tiles;
        this.observers = new ArrayList<>();
    }

    public void select(SerializableTile selectedTile, int id) {
        if(tiles.stream().anyMatch(tile -> tile.getSelection() == id)) {
            SerializableTile firstTile = tiles.stream()
                    .filter(t->t.getSelection() == id)
                    .collect(Collectors.toList())
                    .get(0);

            SerializableTile secondTile = tiles.stream()
                    .filter(t->t.getCurrentPosition() == selectedTile.getCurrentPosition())
                    .collect(Collectors.toList())
                    .get(0);

            firstTile.unselect();
            swap(firstTile, secondTile);
        } else {
            tiles.stream()
                    .filter(t->t.getCurrentPosition() == selectedTile.getCurrentPosition())
                    .collect(Collectors.toList())
                    .get(0)
                    .select(id);
        }

        Collections.sort(this.tiles);
        for(RemoteObserver ro: observers) {
            try {
                ro.update();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void swap(SerializableTile t1, SerializableTile t2) {
        int pos = t1.getCurrentPosition();
        t1.setCurrentPosition(t2.getCurrentPosition());
        t2.setCurrentPosition(pos);
    }

    @Override
    public void addRemoteObserver(RemoteObserver remoteObserver) throws RemoteException {
        observers.add(remoteObserver);
        for(RemoteObserver ro: observers) {
            ro.updateRemoteObservers(observers);
        }
    }

    @Override
    public List<SerializableTile> getTiles() {
        return this.tiles;
    }

    @Override
    public void loadCurrentTiles(List<SerializableTile> currentTiles) {
        this.tiles = new ArrayList<>(currentTiles);
    }

}
