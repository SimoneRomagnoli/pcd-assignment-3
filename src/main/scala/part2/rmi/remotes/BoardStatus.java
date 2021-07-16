package part2.rmi.remotes;

import part2.rmi.puzzle.SerializableTile;

import java.util.List;
import java.rmi.RemoteException;

public interface BoardStatus extends RemoteObject {

    void select(SerializableTile tile) throws RemoteException;

    void remoteUpdate(List<SerializableTile> tiles) throws RemoteException;

    void setLocalObserver(LocalObserver localObserver) throws RemoteException;

    void loadCurrentTiles(List<SerializableTile> currentTiles) throws RemoteException;

    List<SerializableTile> getTiles() throws RemoteException;
}
