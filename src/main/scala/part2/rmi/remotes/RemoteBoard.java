package part2.rmi.remotes;

import part2.rmi.puzzle.SerializableTile;

import java.rmi.Remote;
import java.util.List;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Interface of a remote object representing a board, distributed among several nodes
 *
 */
public interface RemoteBoard extends Remote {

    /**
     * Select a tile in the board:
     * the method is synchronized as the board can be clicked by one player at a time.
     *
     * @param selectedTile, the selected tile in the board
     * @param id, the id of the player that clicked it
     */
    void select(SerializableTile selectedTile, int id) throws RemoteException;

    /**
     * Updates the map of observers of the remote board
     * and notifies all the other players.
     *
     * @param remoteObserver, the observer of the new player
     * @param id, the id of the new player
     */
    void addRemoteObserver(RemoteObserver remoteObserver, int id) throws RemoteException;

    /**
     * This method has to be synchronized to grant the exact global status of the distributed board.
     *
     * @return the list of tiles of the board
     */
    List<SerializableTile> getTiles() throws RemoteException;

    /**
     * Set the new map of observers if the old board's node crashed.
     *
     * @param observers, the old map of observers
     * @throws RemoteException, in case of unreachable board
     */
    void setRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException;

    /**
     * Load tiles if the starter creates a game.
     *
     * @param currentTiles, the tiles to build the board on
     */
    void loadCurrentTiles(List<SerializableTile> currentTiles) throws RemoteException;

    /**
     * Takes the next id if a node wants to join the game.
     *
     * @return the next id in the game
     * @throws RemoteException, if the board is unreachable
     */
    int getNextId() throws RemoteException;
}
