package part2.rmi.remotes;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteObserver extends Remote {

    void update() throws RemoteException;

    void updateRemoteObservers(List<RemoteObserver> observers) throws RemoteException;

    void newObjectOwner(int port) throws RemoteException;
}
