package br.inf.ufes.ppd.master;

public class RedivisionThread implements Runnable{
	
	private int removingNumber;
	private boolean awake;

	public RedivisionThread() {
		this.removingNumber = 0;
	}

	@Override
	public void run() {
		while(true) {
			try {
				this.setAwake(true);
				while(true) {
					synchronized(this) {wait();}
					if(this.removingNumber == 0) {
						System.out.println("[Remove] Remove is finished, thread will sleep...");
						break;
					}
				}
				this.setAwake(false);
			} catch (InterruptedException e) {
				System.out.println("[Error] InterruptedException in RedivisionThread.");
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void incrementRemovingNumber() {
		this.removingNumber++;
	}
	
	public void decrementRemovingNumber() {
		this.removingNumber--;
	}
	
	public boolean isAwake() {
		return this.awake;
	}

	public void setAwake(boolean awake) {
		this.awake = awake;
	}
	

}
