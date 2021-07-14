package part2.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Notifier extends Remote {

    void notifyChanges() throws RemoteException;

    String getChanges() throws RemoteException;
}
