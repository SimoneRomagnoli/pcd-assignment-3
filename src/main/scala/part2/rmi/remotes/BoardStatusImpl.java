package part2.rmi.remotes;

import part2.rmi.controller.Propagator;
import part2.rmi.puzzle.SerializableTile;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BoardStatusImpl implements BoardStatus {

    private List<SerializableTile> tiles;
    private final Integer id;

    private Propagator propagator;
    private LocalObserver observer;

    private boolean mySelectionActive = false;


    public BoardStatusImpl(List<SerializableTile> tiles, Integer id) {
        this.tiles = tiles;
        this.id = id;
    }



    public void select(SerializableTile selectedTile) {
        if(mySelectionActive) {
            mySelectionActive = false;
            SerializableTile firstTile = tiles.stream()
                    .filter(t->t.getSelection() == this.id)
                    .collect(Collectors.toList())
                    .get(0);

            SerializableTile secondTile = tiles.stream()
                    .filter(t->t.getCurrentPosition() == selectedTile.getCurrentPosition())
                    .collect(Collectors.toList())
                    .get(0);
            firstTile.unselect();

            swap(firstTile, secondTile);
        } else {
            mySelectionActive = true;

            tiles.stream()
                    .filter(t->t.getCurrentPosition() == selectedTile.getCurrentPosition())
                    .collect(Collectors.toList())
                    .get(0)
                    .select(this.id);
        }
        Collections.sort(this.tiles);
        this.propagator.propagate();
        this.observer.update();
    }

    @Override
    public void remoteUpdate(List<SerializableTile> remoteTiles) throws RemoteException {
        this.tiles = new ArrayList<>(remoteTiles);
        this.observer.update();
    }

    private void swap(SerializableTile t1, SerializableTile t2) {
        int pos = t1.getCurrentPosition();
        t1.setCurrentPosition(t2.getCurrentPosition());
        t2.setCurrentPosition(pos);
    }



    @Override
    public void setLocalObserver(LocalObserver localObserver) throws RemoteException {
        this.observer = localObserver;
    }

    @Override
    public List<SerializableTile> getTiles() {
        return this.tiles;
    }

    @Override
    public void loadCurrentTiles(List<SerializableTile> currentTiles) {
        this.tiles = new ArrayList<>(currentTiles);
    }

    @Override
    public void setPropagator(Propagator propagator) throws RemoteException {
        this.propagator = propagator;
    }
}
