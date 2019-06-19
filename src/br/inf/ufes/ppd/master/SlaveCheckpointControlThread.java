package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class SlaveCheckpointControlThread implements Runnable {

	private MasterImpl master;
	private int attacks;

	public SlaveCheckpointControlThread(MasterImpl master) {
		this.master = master;
		this.attacks = 0;
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (this) {
					wait();
				}

				while (true) {
					Thread.sleep(20000);
					synchronized (this.getMaster().getSlaveMap()) {
						Set<UUID> slaveSet = this.getMaster().getIdMapSlave();
						ArrayList<UUID> removeSlaveList = new ArrayList<UUID>();
						if (this.getAttacks() == 0) {
							System.out.println("[System Sleep] No more attacks, going to sleep...");
							for (UUID id : slaveSet)
								this.getMaster().getSlaveMap().get(id).setAlive(true);
							break;
						}

						for (UUID id : slaveSet) {
							if (!this.getMaster().getSlaveMap().get(id).isFinished())
								if (this.getMaster().getSlaveMap().get(id).isAlive()) {
									System.out.println("[System Heartbeat] Slave " + this.getMaster().getSlaveMap().get(id).getSlaveName() + " is alive!");
									this.getMaster().getSlaveMap().get(id).setAlive(false);
								} else {
									System.out.println("[System Heartbeat] Slave " + this.getMaster().getSlaveMap().get(id).getSlaveName()
											+ " is not alive...");
									removeSlaveList.add(id);
								}
						}
						try {
							for (UUID id : removeSlaveList) {
								this.getMaster().removeSlave(id);
							}
						} catch (RemoteException e) {
							System.out.println("[Error] Remote Exception in SlaveCheckpointControlThread");
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
				System.out.println("[Error] Interrupted Exception in SlaveCheckpointControlThread");
				e.printStackTrace();
			}
		}
	}

	public MasterImpl getMaster() {
		return master;
	}

	public void addAttack() {
		this.attacks++;
	}

	public void finishAttack() {
		this.attacks--;
	}

	public int getAttacks() {
		return attacks;
	}
}
