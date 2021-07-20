package part2.rmi.remotes;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Interface of an observer of the remote board.
 * It updates the players when something changes in the remote object.
 *
 */
public interface RemoteObserver extends Remote {

    /**
     * Update the controller owning the observer.
     *
     * @throws RemoteException if the node is unreachable
     */
    void update() throws RemoteException;

    /**
     * Update the controller with a new map of observers.
     *
     * @param observers, the new map of observers
     * @throws RemoteException, if the node is unreachable
     */
    void updateRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException;

    /**
     * If the starter node fails, the other nodes need to recreate the board with the actual status.
     * This method updates the controller with the port of the new owner of the board remote object.
     *
     * @param port, the new owner's port
     * @throws RemoteException, if the node is unreachable
     * @throws NotBoundException, if the registry is unreachable
     */
    void newObjectOwner(int port) throws RemoteException, NotBoundException;

    /**
     * Get the owner's id.
     *
     * @return the owner's id
     * @throws RemoteException if the node is unreachable
     */
    int getId() throws RemoteException;
}
