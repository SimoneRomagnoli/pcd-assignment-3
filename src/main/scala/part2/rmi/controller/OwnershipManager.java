package part2.rmi.controller;

import part2.rmi.Starter;
import part2.rmi.remotes.BoardStatus;
import part2.rmi.remotes.RemoteObserver;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class OwnershipManager implements Serializable {

    private static final int BASE_PORT = 1098;
    private static final String BOARD = "boardStatus";

    private final int id;
    private final Controller controller;

    private Map<RemoteObserver, Integer> observers;

    public OwnershipManager(int id, Controller controller) {
        this.observers = new HashMap<>();
        this.controller = controller;
        this.id = id;
    }

    public void propagate() {
        for(RemoteObserver observer:observers.keySet()) {
            try {
                observer.newObjectOwner(BASE_PORT + this.id);
            } catch (RemoteException | NotBoundException e) {
                System.out.println("Node down: removing it from the game");
            }
        }
    }


    public void updateRemoteObservers(Map<RemoteObserver, Integer> observers) {
        this.observers = observers;
    }

    public void updateRemoteObjects(int remotePort) throws RemoteException, NotBoundException {
        Registry remoteRegistry = LocateRegistry.getRegistry(remotePort);
        final int localPort = Starter.REGISTRY_PORT + this.id - 1;
        Registry localRegistry = LocateRegistry.getRegistry(localPort);

        BoardStatus remoteBoard = (BoardStatus) remoteRegistry.lookup(BOARD);
        BoardStatus remoteBoardStub = (BoardStatus) UnicastRemoteObject.exportObject(remoteBoard, 0);
        localRegistry.rebind(BOARD, remoteBoardStub);

        this.controller.reset();
    }

    public Map<RemoteObserver, Integer> getObservers() {
        return this.observers;
    }
}
