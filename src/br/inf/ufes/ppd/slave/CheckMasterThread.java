package br.inf.ufes.ppd.slave;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;

// Deve ser inicializada assim que o slaveExecute for inicializado.

public class CheckMasterThread implements Runnable {
	
	private Slave slave;
	private String slaveName;
	private UUID slaveKey;
	private SlaveManager callbackinterface;
	private Registry registry;

	public CheckMasterThread(Slave slaveRef, String slaveName, UUID slaveKey, SlaveManager callbackinterface, Registry registry) {
		this.slaveKey = slaveKey;
		this.slave = slaveRef;
		this.slaveName = slaveName;
		this.callbackinterface = callbackinterface;
		this.registry = registry;
	}

	@Override
	public void run() {
		try {
			try {
				this.getCallbackinterface().addSlave(this.getSlave(), this.getSlaveName(), this.getSlaveKey());
				System.out.println("[System Connection] Successfully connected in master's host.");
			} catch (RemoteException e) {
				try {
					System.out.println("[System Connection Error] Couldn't find the master and will try to reconnect again in 30 seconds.");
					this.setCallbackinterface((SlaveManager) this.getRegistry().lookup("mestre"));
				} catch (NotBoundException e1) {
					System.out.println("[System Connection Error] Couldn't find the master and will try again in 30 seconds.");
				} catch (RemoteException e1) {
					System.out.println("[Error] Can't find registry! ");
					System.exit(1);
				}
			}
			
			while(true) {
				synchronized(this) { wait(30000); }
				System.out.println("[System Connection] Attempting to reconnect in master's host...");
				try {
					this.getCallbackinterface().addSlave(this.getSlave(), this.getSlaveName(), this.getSlaveKey());
					System.out.println("[System Connection] Successfully reconnected!");
				} catch (RemoteException e) {
					try {
						System.out.println("[System Connection Error] Couldn't reconnect in master's host and will find the new master reference...");
						this.setCallbackinterface((SlaveManager) this.getRegistry().lookup("mestre"));
						System.out.println("[System Connection Error] Found the master and will try to reconnect in 30 seconds.");
					} catch (NotBoundException e1) {
						System.out.println("[System Connection Error] Couldn't find the master and will try again in 30 seconds.");
					} catch (RemoteException e1) {
						System.out.println("[Error] Can't find registry! ");
						System.exit(1);
					}
				}
			}
		} catch (InterruptedException e) {
			System.out.println("[Error] InterruptedException in SlaveCheckMasterThread");
			e.printStackTrace();
		}
	}

	public Slave getSlave() {
		return slave;
	}

	public SlaveManager getCallbackinterface() {
		return callbackinterface;
	}
	
	public void setCallbackinterface(SlaveManager slaveManager) {
		this.callbackinterface = slaveManager;
	}

	public String getSlaveName() {
		return slaveName;
	}

	public UUID getSlaveKey() {
		return slaveKey;
	}

	public Registry getRegistry() {
		return registry;
	}

}
