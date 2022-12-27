package telran.git.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class CommitFile implements Serializable {

	private static final long serialVersionUID = 1L;
	public String path;
	public Instant modificationTime;
	public List<String> content;
	public String commitName;

	public CommitFile(String path, Instant modificationTime, List<String> content, String commitName) {
		this.path = path;
		this.modificationTime = modificationTime;
		this.content = content;
		this.commitName = commitName;
	}

}
