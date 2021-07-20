package part2.rmi.remotes;

import part2.rmi.controller.Controller;

import java.util.List;
import java.rmi.RemoteException;

public class RemoteObserverImpl implements RemoteObserver {

    private final Controller controller;

    public RemoteObserverImpl(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void update() {
        this.controller.update();
    }

    @Override
    public void updateRemoteObservers(List<RemoteObserver> observers) throws RemoteException {
        this.controller.updateRemoteObservers(observers);
    }

    @Override
    public void newObjectOwner(int port) {
        this.controller.updateRemoteObjects(port);
    }
}
