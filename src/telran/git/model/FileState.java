package telran.git.model;

import java.nio.file.Path;
import java.util.Objects;

public class FileState {
	
	public Path path;
	public Status status;

	@Override
	public String toString() {
		return String.format("%s: %s", path, status);
	}

	public FileState(Path path, Status status) {
		this.path = path;
		this.status = status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, status);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileState other = (FileState) obj;
		return Objects.equals(path, other.path) && status == other.status;
	}
}
