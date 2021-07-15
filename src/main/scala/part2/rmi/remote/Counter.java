package part2.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Counter extends Remote {
	void inc() throws RemoteException;
	int getValue() throws RemoteException;
	void addObservable(RemoteObservable obs) throws RemoteException;
}
