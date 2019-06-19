package br.inf.ufes.ppd.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.inf.ufes.ppd.Slave;

public class SlaveProcess {
	private Slave slave;
	private String slaveName;
	private boolean alive;
	private Map<Integer, ArrayList<AttackBlock>> attackNumberBlock = new HashMap<>();
	private int numberAttacks;
	
	public SlaveProcess(Slave slave, String slaveName, boolean alive) {
		this.slave = slave;
		this.slaveName = slaveName;
		this.alive = alive;
		this.numberAttacks = 0;
	}
	
	public Map<Integer, ArrayList<AttackBlock>> getAttackBlock() {
		return attackNumberBlock;
	}
	
	public CheckpointData getCheckpointData(int attackNumber) {
		return new CheckpointData(attackNumber, this.attackNumberBlock.get(attackNumberBlock));
	}
	
	public int initiateAttackBlock(int attackNumber, long initialwordindex, long finalwordindex){
		if(!this.attackNumberBlock.containsKey(attackNumber)) 
			this.attackNumberBlock.put(attackNumber, new ArrayList<>());
		this.attackNumberBlock.get(attackNumber).add(new AttackBlock(initialwordindex, finalwordindex));
		return this.attackNumberBlock.get(attackNumber).size() - 1;
	}
	
	public long getFinalwordindex(int attackNumber, int attackBlockIndex) {
		return this.attackNumberBlock.get(attackNumber).get(attackBlockIndex).getFinalwordindex();
	}
	
	public void setFinalwordindex(int attackNumber, long finalwordindex, int attackBlockIndex) {
		this.attackNumberBlock.get(attackNumber).get(attackBlockIndex).setFinalwordindex(finalwordindex);
	}
	
	public long getInitialwordindex(int attackNumber, int attackBlockIndex) {
		return this.attackNumberBlock.get(attackNumber).get(attackBlockIndex).getInitialwordindex();
	}
	
	public void setInitialwordindex(int attackNumber, long initialwordindex, int attackBlockIndex) {
		this.attackNumberBlock.get(attackNumber).get(attackBlockIndex).setInitialwordindex(initialwordindex);
	}
	
	public int getNumberAttacks() {
		return numberAttacks;
	}
	
	public String getSlaveName() {
		return slaveName;
	}
	
	public Slave getSlave() {
		return slave;
	}
	
	public void setSlave(Slave slave) {
		this.slave = slave;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	
	public boolean isFinished() {
		return this.numberAttacks == 0;
	}
	
	public void incrementNumberAttacks() {
		this.numberAttacks++;
	}
	
	public void decrementNumberAttacks() {
		this.numberAttacks--;
	}
	
	public boolean isAttackFinished(int attackNumber, int attackBlockIndex){
		return this.getInitialwordindex(attackNumber, attackBlockIndex) == this.getFinalwordindex(attackNumber, attackBlockIndex);
	}
}
