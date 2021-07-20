package part2.rmi.remotes;

import part2.rmi.controller.Controller;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

public class RemoteObserverImpl implements RemoteObserver {

    private final Controller controller;
    private final int id;

    public RemoteObserverImpl(Controller controller, int id) {
        this.controller = controller;
        this.id = id;
    }

    @Override
    public void update() throws RemoteException {
        this.controller.update();
    }

    @Override
    public void updateRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException {
        this.controller.updateRemoteObservers(observers);
    }

    @Override
    public void newObjectOwner(int port) throws RemoteException, NotBoundException {
        this.controller.updateRemoteObjects(port);
    }

    @Override
    public int getId() throws RemoteException {
        return this.id;
    }
}
