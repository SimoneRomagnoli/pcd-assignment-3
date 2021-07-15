package part2.rmi;


import part2.rmi.controller.Controller;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Joiner2 {

    public static void main(String[] args) {
        try {
            //GET REMOTE REGISTRY
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT);

            //WATCH HOST LIST ON FIRST HOST
            HostList remoteHostList = (HostList)registry.lookup("hostlist");
            final int id = remoteHostList.getHostList().size();

            //CREATE LOCAL REGISTRY
            LocateRegistry.createRegistry(Starter.REGISTRY_PORT + id);

            //CREATE OWN HOST LIST
            HostList localHostList = new HostListImpl(remoteHostList);
            HostList localHostListStub = (HostList) UnicastRemoteObject.exportObject(localHostList, 0);
            LocateRegistry.getRegistry(Starter.REGISTRY_PORT+id).rebind("hostlist", localHostListStub);

            //CREATE OWN COUNTER
            Counter remoteCounter = (Counter) LocateRegistry.getRegistry(Starter.REGISTRY_PORT).lookup("countObj");
            Counter localCounter = new CounterImpl(remoteCounter.getValue());
            Counter localCounterStub = (Counter) UnicastRemoteObject.exportObject(localCounter, 0);
            LocateRegistry.getRegistry(Starter.REGISTRY_PORT+id).rebind("countObj", localCounterStub);

            //Controller controller = new Controller(id, localCounter, localHostList);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
