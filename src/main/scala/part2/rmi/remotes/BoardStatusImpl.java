package part2.rmi.remotes;

import part2.rmi.puzzle.SerializableTile;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class BoardStatusImpl implements BoardStatus {

    private List<SerializableTile> tiles;

    private Map<RemoteObserver, Integer> observers;

    public BoardStatusImpl(List<SerializableTile> tiles) {
        this.tiles = tiles;
        this.observers = new HashMap<>();
    }

    @Override
    public synchronized void select(SerializableTile selectedTile, int id) {
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

        removeUnreachableObservers();
        updateObservers();
    }

    @Override
    public synchronized void addRemoteObserver(RemoteObserver remoteObserver, int id) {
        observers.put(remoteObserver, id);
        removeUnreachableObservers();
        updateObservers();
    }

    @Override
    public List<SerializableTile> getTiles() {
        return this.tiles;
    }

    @Override
    public void setRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException {
        this.observers = new HashMap<>(observers);
    }

    @Override
    public int getNextId() throws RemoteException {
        return observers.values().stream().max(Comparator.comparingInt(a -> a)).orElse(0)+1;
    }

    @Override
    public void loadCurrentTiles(List<SerializableTile> currentTiles) {
        this.tiles = new ArrayList<>(currentTiles);
    }

    private void swap(SerializableTile t1, SerializableTile t2) {
        int pos = t1.getCurrentPosition();
        t1.setCurrentPosition(t2.getCurrentPosition());
        t2.setCurrentPosition(pos);
    }

    private void removeUnreachableObservers() {
        List<RemoteObserver> toBeRemoved = new ArrayList<>();
        for(RemoteObserver observer: observers.keySet()) {
            try {
                observer.getId();
            } catch (RemoteException e) {
                toBeRemoved.add(observer);
            }
        }
        for(RemoteObserver o:toBeRemoved) {
            int removedId = observers.get(o);
            Optional<SerializableTile> tileToUnselect = tiles.stream().filter(tile -> tile.getSelection() == removedId).findFirst();
            tileToUnselect.ifPresent(SerializableTile::unselect);
        }
        toBeRemoved.forEach(o -> observers.remove(o));
    }

    private void updateObservers() {
        for(RemoteObserver o: observers.keySet()) {
            try {
                o.update();
                o.updateRemoteObservers(Collections.unmodifiableMap(observers));
            } catch (RemoteException e) {
                System.out.println("Worst case scenario: someone crashed in a critical moment");
            }
        }
    }
}
