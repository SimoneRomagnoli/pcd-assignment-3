package part2.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteObservable extends Remote {

    void update(int value) throws RemoteException;
}
