package br.inf.ufes.ppd.master;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttackFinishControlThread implements Runnable {

	private List<UUID> listSlaveID;
	
	public AttackFinishControlThread() {
		this.listSlaveID = new ArrayList<UUID>();
	}

	public List<UUID> getListSlaveID() {
		return listSlaveID;
	}

	public void setList(List<UUID> listSlaveID) {
		this.listSlaveID = listSlaveID;
	}
	
	public void removeList(UUID uuid){
		synchronized(this.listSlaveID){
			this.listSlaveID.remove(uuid);
		}
	}
	
	public void clearList() {
		this.listSlaveID.clear();
	}

	@Override
	public void run() {
		try {
			while(true) {
				synchronized(this) { wait(); }
				if(this.getListSlaveID().isEmpty()) break;
			}
		} catch (InterruptedException e) {
			System.out.println("[Error] InterruptedException in AttackFinishControlThread");
			e.printStackTrace();
		}
		
	}

}
