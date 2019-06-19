package br.inf.ufes.ppd.model;

import java.util.List;

public class CheckpointData {
	private int attackNumber;
	private List<AttackBlock> attackBlocks;
	
	public CheckpointData(int attackNumber, List<AttackBlock> attackBlocks) {
		this.attackNumber = attackNumber;
		this.attackBlocks = attackBlocks;
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}
	
	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
	}
	
	public List<AttackBlock> getAttackBlocks() {
		return attackBlocks;
	}

	public long getFinalwordindex(int attackBlockIndex) {
		return this.attackBlocks.get(attackBlockIndex).getFinalwordindex();
	}
	
	public void setFinalwordindex(long finalwordindex, int attackBlockIndex) {
		this.attackBlocks.get(attackBlockIndex).setFinalwordindex(finalwordindex);
	}
	
	public long getCurrentindex(int attackBlockIndex) {
		return this.attackBlocks.get(attackBlockIndex).getInitialwordindex();
	}
	
	public void setCurrentindex(long currentindex, int attackBlockIndex) {
		this.attackBlocks.get(attackBlockIndex).setInitialwordindex(currentindex);
	}	
}
