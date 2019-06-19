package br.inf.ufes.ppd.master;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.model.AttackBlock;
import br.inf.ufes.ppd.model.CheckpointData;
import br.inf.ufes.ppd.model.SlaveProcess;

public class MasterImpl implements Master {

	private Map<UUID, SlaveProcess> slaveMap = Collections.synchronizedMap(new HashMap<>());
	private Map<Integer, List<Guess>> guessesMap = new HashMap<>();
	private Map<Integer, AttackFinishControlThread> attackFinishControlMap = new HashMap<>();

	Vector<CheckpointData> checkpointList = new Vector<CheckpointData>();
	private int attackNumber;
	private int lineNumber;
	// Thread para verificar os checkpoints dos slaves.
	private SlaveCheckpointControlThread controlCheckpointThread;
	private byte[] ciphertext;
	private byte[] knowntext;
	private RedivisionThread redivisionThreadObject;
	private Thread tRedivision;

	public MasterImpl(String fileName) {
		this.lineNumber = readFile(fileName);
		this.attackNumber = 0;

		controlCheckpointThread = new SlaveCheckpointControlThread(this);
		Thread tMaster = new Thread(controlCheckpointThread);
		tMaster.start();

		redivisionThreadObject = new RedivisionThread();
		tRedivision = new Thread(redivisionThreadObject);
	}

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		synchronized (this.getSlaveMap()) {
			if (this.getSlaveMap().get(slavekey) == null) {
				SlaveProcess sp = new SlaveProcess(s, slaveName, true);
				this.getSlaveMap().put(slavekey, sp);
				System.out.println("[Register] Slave: " + slaveName + " successfully registered!");
			} else {
				System.out.println("[Register] Slave: " + slaveName + " succesfully checked!");
			}
		}
	}

	// Comentado para realizar os testes de otimização.
	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		String name = this.getSlaveName(slaveKey);
		synchronized (this.getRedivisionThread()) {
			this.getRedivisionThread().incrementRemovingNumber();
			if (this.getRedivisionThread().isAwake())
				tRedivision.start();
		}

		int numAttacksFinished = 0;

		synchronized (this.getSlaveMap()) {
			if (this.getSlaveMap().containsKey(slaveKey)) {
				for (Integer attackNumber : this.getSlaveMap().get(slaveKey).getAttackBlock().keySet()) {
					for (int attackBlockIndex = 0; attackBlockIndex < this.getSlaveMap().get(slaveKey).getAttackBlock()
							.get(attackNumber).size(); attackBlockIndex++) {
						if (!this.getAttackFromSlave(slaveKey, attackNumber, attackBlockIndex).isAttackDone()) {
							numAttacksFinished++;
							synchronized (this.getCheckpointList()) {
								boolean found = false;
								for (CheckpointData checkpoint : this.getCheckpointList()) {
									if (checkpoint.getAttackNumber() == attackNumber) {
										found = true;
										checkpoint.getAttackBlocks().add(new AttackBlock(
												this.getAttackFromSlave(slaveKey, attackNumber, attackBlockIndex)));
									}
								}
								if (!found) {
									List<AttackBlock> checkpointList = new ArrayList<>();
									checkpointList.add(new AttackBlock(
											this.getAttackFromSlave(slaveKey, attackNumber, attackBlockIndex)));
									this.getCheckpointList().add(new CheckpointData(attackNumber, checkpointList));
								}
							}
						}
					}
				}
				this.getSlaveMap().remove(slaveKey);
			}
			System.out.println("[Remove] Slave " + name + " has been successfully removed.");

			synchronized (this.getRedivisionThread()) {
				this.getRedivisionThread().decrementRemovingNumber();
			}

			try {
				tRedivision.join();
				this.redivision();
			} catch (InterruptedException e) {
				System.out.println("[Error] InterruptedException in MasterImpl.");
				e.printStackTrace();
			}
		}
		synchronized (this.getAttackFinishControlMap()) {
			for (int att : this.getAttackFinishControlMap().keySet()) {
				synchronized (this.getAttackFromMap(att)) {
					for (int i = 0; i < numAttacksFinished; i++)
						this.getAttackFromMap(att).removeList(slaveKey);
					if (this.getAttackFromMap(att).getListSlaveID().size() == 0) {
						this.getAttackFromMap(att).notify();
					}
				}
			}
		}
	}

	@Override
	public synchronized void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		synchronized (this.getGuessesMap()) {
			synchronized (this.getGuessesMap().get(attackNumber)) {
				this.getGuessesMap().get(attackNumber).add(currentguess);
			}
		}
		System.out.println("[Candidate Key] Slave: [" + slaveMap.get(slaveKey).getSlaveName() + "] AttackNumber: ["
				+ attackNumber + "] Index: [" + currentindex + "]; Key: [" + currentguess.getKey() + "]");

	}

	@Override
	public synchronized void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		long lastIndex = -1;
		synchronized (this.getSlaveMap()) {
			for (int attackBlockIndex = 0; attackBlockIndex < this.getSlaveMap().get(slaveKey).getAttackBlock()
					.get(attackNumber).size(); attackBlockIndex++) {
				long initialindexlocal = this.getSlaveMap().get(slaveKey).getInitialwordindex(attackNumber,
						attackBlockIndex);
				long finalindexlocal = this.getSlaveMap().get(slaveKey).getFinalwordindex(attackNumber,
						attackBlockIndex);
				if (initialindexlocal <= currentindex && currentindex <= finalindexlocal) {
					this.getSlaveMap().get(slaveKey).setInitialwordindex(attackNumber, currentindex, attackBlockIndex);
					lastIndex = finalindexlocal;
				}
			}
		}
		if (currentindex == lastIndex) {
			synchronized (this.getAttackFinishControlMap()) {
				synchronized (this.getAttackFromMap(attackNumber)) {
					this.getAttackFromMap(attackNumber).removeList(slaveKey);
					this.getAttackFromMap(attackNumber).notify();
				}
			}
			synchronized (this.getSlaveMap()) {
				this.getSlaveMap().get(slaveKey).setAlive(true);
				this.getSlaveMap().get(slaveKey).decrementNumberAttacks();
				System.out.println("[Final Checkpoint] Slave: [" + slaveMap.get(slaveKey).getSlaveName()
						+ "] AttackNumber: [" + attackNumber + "] Index: [" + currentindex + "]");
			}
		} else {
			synchronized (this.getSlaveMap()) {
				this.getSlaveMap().get(slaveKey).setAlive(true);
				System.out.println("[Checkpoint] Slave: [" + slaveMap.get(slaveKey).getSlaveName() + "] AttackNumber: ["
						+ attackNumber + "] Index: [" + currentindex + "]");
			}
		}
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		this.setCiphertext(ciphertext);
		this.setKnowntext(knowntext);
		int attackNumberLocal;
		long initialwordindex, finalwordindex, modwordindex;

		// Atribui os valores para as variáveis que serão utilizadas no ataque.
		initialwordindex = 0;
		finalwordindex = this.getLineNumber() / getMapSlaveSize();

		// Armazena o resto da divisão para acrescentar na última partição.
		modwordindex = this.getLineNumber() % getMapSlaveSize();
		attackNumberLocal = this.getAttackNumber();

		System.out.println("[System Attack] Attack no." + attackNumberLocal + " has begun!");

		synchronized (this.getAttackFinishControlMap()) {
			this.getAttackFinishControlMap().put(attackNumberLocal, new AttackFinishControlThread());
		}

		this.getGuessesMap().put(attackNumberLocal, new ArrayList<Guess>());

		int i = 1;
		Set<UUID> slaveSet;
		synchronized (this.getSlaveMap()) {
			slaveSet = this.getIdMapSlave();
		}
		long keyNumbers = finalwordindex;

		Thread masterFinishThread = new Thread(this.getAttackFromMap(attackNumberLocal));
		masterFinishThread.start();

		synchronized (this.getSlaveMap()) {
			for (UUID slaveKey : slaveSet) {
				if (i == slaveSet.size())
					finalwordindex += modwordindex;
				i++; // Verifica se é a última partição.
				synchronized (this.getAttackFinishControlMap()) {
					this.getAttackFromMap(attackNumberLocal).getListSlaveID().add(slaveKey);
				}
				try {
					// Verifica slave no Map de Slaves, caso não esteja ou não
					// esteja vivo, remove do Map.
					if (!this.removeDeadSlave(slaveKey)) {
						int attackBlockIndex = this.getSlaveMap().get(slaveKey).initiateAttackBlock(attackNumberLocal,
								initialwordindex, finalwordindex);
						this.getSlaveMap().get(slaveKey).setFinalwordindex(attackNumberLocal, finalwordindex,
								attackBlockIndex);
						this.getSlaveMap().get(slaveKey).incrementNumberAttacks();

						// Invoca o startSubAttack
						this.getSlaveMap().get(slaveKey).getSlave().startSubAttack(this.getCiphertext(),
								this.getKnowntext(), initialwordindex, finalwordindex, attackNumberLocal, this);
					}

				} catch (RemoteException e) {
					this.removeDeadSlave(slaveKey);
				}

				initialwordindex = finalwordindex + 1;
				finalwordindex += keyNumbers;
			}
		}

		this.incrementAttackNumber();

		synchronized (this.getControlCheckpointThread()) {
			this.getControlCheckpointThread().addAttack();
			this.getControlCheckpointThread().notify();
		}
		try {
			masterFinishThread.join();
		} catch (InterruptedException e) {
			System.out.println("[Error] Error to wait for Thread of MasterImpl.");
			e.printStackTrace();
		}

		System.out.println("[System Attack] Attack no." + attackNumberLocal + " is done!");

		synchronized (this.getControlCheckpointThread()) {
			this.getControlCheckpointThread().finishAttack();
		}

		Guess[] guesses = new Guess[this.getGuessesMap().get(attackNumberLocal).size()];
		int guessCount = 0;
		for (Guess g : this.getGuessesMap().get(attackNumberLocal)) {
			guesses[guessCount++] = g;
		}

		return guesses;
	}

	public Set<UUID> getIdMapSlave() {
		return this.getSlaveMap().keySet();
	}

	public int getMapSlaveSize() {
		return this.getSlaveMap().size();
	}

	public void freeAttackFinishMap(int attackNumber) {
		synchronized (this.getAttackFinishControlMap()) {
			this.getAttackFromMap(attackNumber).clearList();
			this.getAttackFinishControlMap().remove(attackNumber);
		}
	}

	public void freeGuessesMap(int attackNumber) {
		synchronized (this.getGuessesMap()) {
			this.getGuessesMap().get(attackNumber).clear();
			this.getGuessesMap().remove(attackNumber);
		}
	}

	private int readFile(String fileName) {
		int i = 0;
		try {
			Scanner scanner = new Scanner(new File(fileName));
			while (scanner.hasNextLine()) {
				scanner.nextLine();
				i++;
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("[Error] FileNotFound error in MasterImpl.");
			e.printStackTrace();
		}
		return i - 1;
	}

	public Map<UUID, SlaveProcess> getSlaveMap() {
		return slaveMap;
	}

	public Map<Integer, List<Guess>> getGuessesMap() {
		return guessesMap;
	}

	public Map<Integer, AttackFinishControlThread> getAttackFinishControlMap() {
		return attackFinishControlMap;
	}

	public AttackFinishControlThread getAttackFromMap(int attackNumber) {
		return this.attackFinishControlMap.get(attackNumber);
	}

	public List<CheckpointData> getCheckpointList() {
		return this.checkpointList;
	}

	public void removeCheckpointList(CheckpointData checkpoint) {
		synchronized (this.checkpointList) {
			this.checkpointList.remove(checkpoint);
		}
	}

	public int getAttackNumber() {
		return this.attackNumber;
	}

	public AttackBlock getAttackFromSlave(UUID slaveKey, int attackNumber, int attackBlockIndex) {
		return this.slaveMap.get(slaveKey).getAttackBlock().get(attackNumber).get(attackBlockIndex);
	}

	public void incrementAttackNumber() {
		this.attackNumber++;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	public SlaveCheckpointControlThread getControlCheckpointThread() {
		return this.controlCheckpointThread;
	}

	public byte[] getCiphertext() {
		return this.ciphertext;
	}

	public void setCiphertext(byte[] ciphertext) {
		this.ciphertext = ciphertext;
	}

	public byte[] getKnowntext() {
		return this.knowntext;
	}

	public void setKnowntext(byte[] knowntext) {
		this.knowntext = knowntext;
	}

	public String getSlaveName(UUID slaveKey) {
		return this.slaveMap.get(slaveKey).getSlaveName();
	}

	public RedivisionThread getRedivisionThread() {
		return this.redivisionThreadObject;
	}

	// Funções para a redivisão.
	private boolean removeDeadSlave(UUID slaveKey) {
		List<UUID> removeSlaveList = new ArrayList<>();
		if (!this.getSlaveMap().containsKey(slaveKey)) {
			removeSlaveList.add(slaveKey);
			System.out.println("[Remove] Slave has been removed for don't exists.");
		} else if (!this.getSlaveMap().get(slaveKey).isAlive() && !this.getSlaveMap().get(slaveKey).isFinished()) {
			removeSlaveList.add(slaveKey);
			System.out.println("[Remove] Slave has been removed for no response.");
		} else {
			return false;
		}
		for (UUID slaveKeyUUID : removeSlaveList) {
			try {
				this.removeSlave(slaveKeyUUID);
			} catch (RemoteException e) {
				System.out.println("[Error] Remote Exception in MasterImpl.");
			}
		}
		return true;
	}

	private void sendAttackSlave(Set<UUID> slaveSet, long initialwordindex, long finalwordindex, long modwordindex,
			long keyNumbers, int attackNumber, int i) {
		synchronized (this.getSlaveMap()) {
			synchronized (slaveSet) {
				for (UUID slaveKey : slaveSet) {
					synchronized (this.getAttackFinishControlMap()) {
						this.getAttackFromMap(attackNumber).getListSlaveID().add(slaveKey);
					}
					if (i == slaveSet.size()) {
						finalwordindex += modwordindex;
					}
					i++;
					try {
						int attackBlockIndex = this.getSlaveMap().get(slaveKey).initiateAttackBlock(attackNumber,
								initialwordindex, finalwordindex);
						this.getSlaveMap().get(slaveKey).setFinalwordindex(attackNumber, finalwordindex,
								attackBlockIndex);
						this.getSlaveMap().get(slaveKey).incrementNumberAttacks();
						this.getSlaveMap().get(slaveKey).getSlave().startSubAttack(this.getCiphertext(),
								this.getKnowntext(), initialwordindex, finalwordindex, attackNumber, this);
					} catch (RemoteException e) {
						System.out.println("[System Alert] Found another dead slave, removing from system...");
					}
					initialwordindex = finalwordindex + 1;
					finalwordindex += keyNumbers;
				}
			}
		}
	}

	protected void redivision() {
		List<CheckpointData> finishedCheckpoints = new ArrayList<>();
		System.out.println("[Redistribution] Redistribution has begun!");
		Set<UUID> slaveSet = null;
		List<CheckpointData> checkpointListLocal = null;
		synchronized (this.getCheckpointList()) {
			checkpointListLocal = this.getCheckpointList();
		}
		synchronized (this.getSlaveMap()) {
			slaveSet = this.getIdMapSlave();
		}
		synchronized (this.getCheckpointList()) {
			synchronized (checkpointListLocal) {
				for (CheckpointData checkpoint : checkpointListLocal) {
					for (int attackBlockIndex = 0; attackBlockIndex < checkpoint.getAttackBlocks()
							.size(); attackBlockIndex++) {
						int i = 1;
						long keyNumbers = (checkpoint.getFinalwordindex(attackBlockIndex)
								- checkpoint.getCurrentindex(attackBlockIndex)) / slaveSet.size();
						long initialwordindex = checkpoint.getCurrentindex(attackBlockIndex);
						long finalwordindex = initialwordindex + keyNumbers;
						long modwordindex = (checkpoint.getFinalwordindex(attackBlockIndex)
								- checkpoint.getCurrentindex(attackBlockIndex)) % slaveSet.size();
						int attackNumber = checkpoint.getAttackNumber();

						this.sendAttackSlave(slaveSet, initialwordindex, finalwordindex, modwordindex, keyNumbers,
								attackNumber, i);
					}
					finishedCheckpoints.add(checkpoint);
				}
			}
		}
		for (CheckpointData checkpoint : finishedCheckpoints) {
			this.removeCheckpointList(checkpoint);
		}
		synchronized (this.getControlCheckpointThread()) {
			this.getControlCheckpointThread().addAttack();
			this.getControlCheckpointThread().notify();
		}

	}

}
