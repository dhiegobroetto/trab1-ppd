package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.UUID;

import br.inf.ufes.ppd.SlaveManager;

public class SlaveCheckpoint implements Runnable {

	private String slaveName;
	protected UUID slaveKey;
	private SlaveManager callbackinterface;
	private int attackNumber;
	private long guessIndex;
	private boolean finished;
	
	public SlaveCheckpoint(String slaveName, UUID slaveKey, SlaveManager callbackinterface, int attackNumber) {
		this.slaveName = slaveName;
		this.slaveKey = slaveKey;
		this.callbackinterface = callbackinterface;
		this.attackNumber = attackNumber;
		this.guessIndex = 0;
		this.finished = false;
	}
	
	private void makeCheckPoint(boolean finishThread) {
		if(finishThread){
			System.out.println("[Final Checkpoint] Sending the last attack checkpoint [" + this.getGuessIndex() + "] from attack [" + this.getAttackNumber() + "]...");
			try {
				this.getCallbackinterface().checkpoint(this.getSlaveKey(), this.getAttackNumber(), this.getGuessIndex());
				System.out.println("[Final Checkpoint] Successfully sent the last checkpoint [" + this.getGuessIndex() + "] from attack [" + this.getAttackNumber() + "]!");
			} catch (RemoteException e) {
				System.out.println("[Final Checkpoint Error] Couldn't find the master to send the last checkpoint [" + this.getGuessIndex() + "]!");
			}
		}else{
		System.out.println("[Checkpoint] Sending attack checkpoint [" + this.getGuessIndex() + "] from attack [" + this.getAttackNumber() + "]...");
			try {
				this.getCallbackinterface().checkpoint(this.getSlaveKey(), this.getAttackNumber(), this.getGuessIndex());
				System.out.println("[Checkpoint] Successfully sent a checkpoint [" + this.getGuessIndex() + "] from attack [" + this.getAttackNumber() + "]!");
			} catch (RemoteException e) {
				System.out.println("[Checkpoint Error] Couldn't find the master to send its checkpoint [" + this.getGuessIndex() + "]!");
			}
		}
	}
	
	public String getSlaveName() {
		return slaveName;
	}

	public UUID getSlaveKey() {
		return slaveKey;
	}

	public SlaveManager getCallbackinterface() {
		return callbackinterface;
	}

	public int getAttackNumber() {
		return attackNumber;
	}
	
	public long getGuessIndex() {
		return guessIndex;
	}
	
	public void setGuessIndex(long index) {
		this.guessIndex = index;
	}

	public boolean isFinished() {
		return finished;
	}

	public void finish() {
		this.finished = true;
	}

	@Override
	public void run() {
		try {
			while(true) {
				synchronized(this) { wait(10000); }
				if(this.isFinished())
					break;
				makeCheckPoint(false);
			}
			makeCheckPoint(true);
		} catch (InterruptedException e) {
			System.out.println("[Error] InterruptedException in SlaveCheckPoint.");
			e.printStackTrace();
		}
		
	}

}