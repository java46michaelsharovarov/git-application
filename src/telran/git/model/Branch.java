package telran.git.model;

import java.io.Serializable;

public class Branch implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String branchName;
	public String commitName;

	public Branch(String branchName, String commitName) {
		this.branchName = branchName;
		this.commitName = commitName;
	}
}