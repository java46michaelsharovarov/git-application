package telran.git.model;

import java.io.Serializable;
import java.util.*;

public class Commit implements Serializable {

	private static final long serialVersionUID = 1L;
	public String commitName;
	public String commitMessage;
	public Commit prev;
	public List<CommitFile> commitFiles;


	public Commit(String commitName, String commitMessage, Commit prev, List<CommitFile> commitFiles) {
		this.commitName = commitName;
		this.commitMessage = commitMessage;
		this.prev = prev;
		this.commitFiles = commitFiles;
	}
}
