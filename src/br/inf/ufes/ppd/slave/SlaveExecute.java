package br.inf.ufes.ppd.slave;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.UUID;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;

public class SlaveExecute {

	public static void main(String[] args) {
		UUID slaveKey = java.util.UUID.randomUUID();
		Scanner buffer = new Scanner(System.in);
		System.out.println("[System Read] Slave name: ");
		String slaveName = buffer.nextLine();
		buffer.close();
		Registry registry;
		SlaveManager callbackinterface;
		try {
			registry = LocateRegistry.getRegistry(args[1]);
			callbackinterface = (SlaveManager) registry.lookup("mestre");
			Slave slave = new SlaveImpl(slaveKey, slaveName, args[0], callbackinterface);
			Slave slaveRef = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			CheckMasterThread slavethread = new CheckMasterThread(slaveRef, slaveName, slaveKey, callbackinterface, registry);
			Thread t = new Thread(slavethread);
			t.start();
			
			// Ctrl + C para remover o escravo automaticamente do sistema.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						Master master = (Master) registry.lookup("mestre");
						master.removeSlave(slaveKey);
						System.out.println("[Remove] Slave has been terminated!");
					} catch (RemoteException | NotBoundException e) {
						System.out.println("[Remove] Slave has been terminated!");
					}
				}
			});
		} catch (ExportException e) {
			System.out.println("[Error] ExportException in SlaveExecute.");
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println("[Error] RemoteException in SlaveExecute.");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("[Error] Exception in SlaveExecute.");
			e.printStackTrace();
		}
	}
}