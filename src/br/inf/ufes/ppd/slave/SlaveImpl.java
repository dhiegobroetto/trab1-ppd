package br.inf.ufes.ppd.slave;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.UUID;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;

public class SlaveImpl implements Slave {

	protected UUID slaveKey;
	protected String fileName;
	protected String slaveName;
	protected SlaveManager master;

	public SlaveImpl(UUID slaveKey, String slaveName, String fileName, SlaveManager master) {
		this.slaveKey = slaveKey;
		this.slaveName = slaveName;
		this.fileName = fileName;
		this.master = master;
	}

	@Override
	public void startSubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
			int attackNumber, SlaveManager callbackinterface) {
		try {
			Scanner scanner = new Scanner(new File(getFileName()));
			for (int i = 0; i < initialwordindex && scanner.hasNextLine(); i++) {
				scanner.nextLine();
			}
			System.out.println("[System Attack] Attack no." + attackNumber + " has begun!");
			System.out.println("[Slave Index] Attack: [" + attackNumber + "] Index: [" + initialwordindex + ";" + finalwordindex + "]");
			
			SubAttackThread subAttack = new SubAttackThread(this.getSlaveKey(), this.getSlaveName(),
					ciphertext, knowntext, initialwordindex, finalwordindex, attackNumber, callbackinterface, scanner);
			Thread subAttackThread = new Thread(subAttack);
			subAttackThread.start();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public UUID getSlaveKey() {
		return slaveKey;
	}

	public String getFileName() {
		return fileName;
	}

	public String getSlaveName() {
		return slaveName;
	}

	public SlaveManager getMaster() {
		return master;
	}

}
