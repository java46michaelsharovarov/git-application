package telran.git.model;

public class CommitMessage {
	
	public String commitName;
	public String commitMessage;
	
	public CommitMessage(String commitName, String commitMessage) {
		this.commitName = commitName;
		this.commitMessage = commitMessage;
	}

	@Override
	public String toString() {
		return String.format("%s: %s", commitName, commitMessage);
	}
	
}