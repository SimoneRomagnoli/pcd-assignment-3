package part2.rmi.remotes;

import part2.rmi.puzzle.SerializableTile;

import java.rmi.Remote;
import java.util.List;
import java.rmi.RemoteException;
import java.util.Map;

public interface BoardStatus extends Remote {

    void select(SerializableTile tile, int id) throws RemoteException;

    void addRemoteObserver(RemoteObserver remoteObserver, int id) throws RemoteException;

    void loadCurrentTiles(List<SerializableTile> currentTiles) throws RemoteException;

    List<SerializableTile> getTiles() throws RemoteException;

    void setRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException;

    int getNextId() throws RemoteException;
}
