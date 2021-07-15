package part2.rmi.remotes;

import part2.rmi.controller.Propagator;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteObject extends Remote {
    void setPropagator(Propagator propagator) throws RemoteException;
}
