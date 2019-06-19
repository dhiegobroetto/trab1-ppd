package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import br.inf.ufes.ppd.Master;

public class MasterExecute {
	public static void main(String[] args) {
		try {
			Master master = new MasterImpl(args[0]);
			Master masterRef = (Master) UnicastRemoteObject.exportObject(master, 0);
			Registry registry = LocateRegistry.getRegistry("localhost");
			registry.rebind("mestre", masterRef);
			System.out.println("[System Initiation] Master ready!");
		} catch (RemoteException e) {
			System.err.println("[Error] Error to connect master to server.");
			e.printStackTrace();
		}
	}
}
