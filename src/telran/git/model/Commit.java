package telran.git.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public class Commit implements Serializable {

	private static final long serialVersionUID = 1L;
	public Instant timestamp;
	public String commitName;
	public String commitMessage;
	public Commit prev;
	public List<CommitFile> commitFiles;

}
