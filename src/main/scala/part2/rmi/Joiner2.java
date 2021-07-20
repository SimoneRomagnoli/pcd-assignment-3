package part2.rmi;


import part2.rmi.controller.Controller;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Second player to join a game.
 *
 */
public class Joiner2 {

    public static void main(String[] args) {
        try {
            //GET REMOTE REGISTRY
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT);

            //CREATE OWN MODEL
            RemoteBoard remoteBoard = (RemoteBoard) registry.lookup("boardStatus");
            RemoteBoard remoteBoardStub = (RemoteBoard) UnicastRemoteObject.exportObject(remoteBoard, 0);
            final int id = remoteBoard.getNextId();
            final int port = Starter.REGISTRY_PORT + id - 1;

            //CREATE OWN REGISTRY
            LocateRegistry.createRegistry(port);
            LocateRegistry.getRegistry(port).rebind("boardStatus", remoteBoardStub);
            RemoteBoard myBoard = (RemoteBoard)LocateRegistry.getRegistry(port).lookup("boardStatus");

            new Controller(id, myBoard);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
