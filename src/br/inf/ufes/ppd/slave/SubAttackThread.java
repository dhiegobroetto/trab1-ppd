package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.UUID;

import br.inf.ufes.ppd.Decrypt;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.SlaveManager;

public class SubAttackThread implements Runnable {

	private UUID slaveKey; 
	private String slaveName; 
	private byte[] ciphertext;
	private byte[] knowntext;
	private long initialwordindex;
	private long finalwordindex;
	private int attackNumber;
	private SlaveManager callbackinterface;
	private Scanner scanner;



	public SubAttackThread(UUID slaveKey, String slaveName, byte[] ciphertext,
			byte[] knowntext, long initialwordindex, long finalwordindex, int attackNumber,
			SlaveManager callbackinterface, Scanner scanner) {
		this.slaveKey = slaveKey;
		this.slaveName = slaveName;
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.initialwordindex = initialwordindex;
		this.finalwordindex = finalwordindex;
		this.attackNumber = attackNumber;
		this.callbackinterface = callbackinterface;
		this.scanner = scanner;
	}

	@Override
	public void run() {
		try {
			String knowntext = new String(this.getKnowntext());

			SlaveCheckpoint slaveCheckpoint = new SlaveCheckpoint(this.getSlaveName(), this.getSlaveKey(), this.getCallbackinterface(), this.getAttackNumber());
			Thread slaveCheckpointThread = new Thread(slaveCheckpoint);
			slaveCheckpointThread.start();

			long i = 0;
			long j = this.getFinalwordindex() - this.getInitialwordindex();
			
			while (this.getScanner().hasNextLine()) {
				if (i >= j)
					break;
				i++;
				String key = this.getScanner().nextLine();
				Guess guess = new Guess();
				guess.setMessage(Decrypt.decryptFile(this.getCiphertext(), key));
				
				synchronized(slaveCheckpoint) {slaveCheckpoint.setGuessIndex(this.getInitialwordindex() + i - 1);}
				
				if (guess.getMessage() != null) {
					String decryptedText = new String(guess.getMessage());
					if (decryptedText.indexOf(knowntext) != -1) {
						guess.setKey(key);
						this.getCallbackinterface().foundGuess(this.getSlaveKey(), this.getAttackNumber(), this.getInitialwordindex() + i - 1, guess);
						System.out.println("[Candidate Key] AttackNumber: [" + this.getAttackNumber() + "] Index: [" + (this.getInitialwordindex() + i - 1) + "]; Key: [" + key + "]");
//						System.out.println("[Checkpoint] Successfully sent the last checkpoint [" + this.getGuessIndex() + "] from attack [" + this.getAttackNumber() + "]!");
					}
				}
			}
			
			this.getScanner().close();
			
			synchronized (slaveCheckpoint) {
				slaveCheckpoint.setGuessIndex(this.getInitialwordindex() + i);
				slaveCheckpoint.finish();
				slaveCheckpoint.notify();
			}
			System.out.println("[Attack] Attack no." + this.getAttackNumber() + " finished!");
		} catch (RemoteException e) {
			System.out.println("[Error] RemoteException in SubAttackThread");
			e.printStackTrace();
		}
	}

	public UUID getSlaveKey() {
		return slaveKey;
	}

	public String getSlaveName() {
		return slaveName;
	}

	public byte[] getCiphertext() {
		return ciphertext;
	}

	public byte[] getKnowntext() {
		return knowntext;
	}

	public long getInitialwordindex() {
		return initialwordindex;
	}

	public long getFinalwordindex() {
		return finalwordindex;
	}

	public int getAttackNumber() {
		return attackNumber;
	}
	public SlaveManager getCallbackinterface() {
		return callbackinterface;
	}

	public Scanner getScanner() {
		return scanner;
	}
	
}
