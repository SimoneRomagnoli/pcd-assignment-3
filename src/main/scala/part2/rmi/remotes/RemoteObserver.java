package part2.rmi.remotes;

import java.rmi.NotBoundException;
import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RemoteObserver extends Remote {

    void update() throws RemoteException;

    void updateRemoteObservers(Map<RemoteObserver, Integer> observers) throws RemoteException;

    void newObjectOwner(int port) throws RemoteException, NotBoundException;

    int getId() throws RemoteException;
}
