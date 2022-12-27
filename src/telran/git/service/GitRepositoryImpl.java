package telran.git.service;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;

import telran.git.model.*;

public class GitRepositoryImpl implements GitRepository {
	private String gitPath;
	private HashMap<String, Commit> commits;
	private HashMap<String, CommitFile> commitFiles;
	private HashMap<String, Branch> branches;
	private String head; // name of current branch or commit
	private String ignoreExpressions = "(\\..*)";

	private static final long serialVersionUID = 1L;
	public static final String COMMIT_PERFORMED = "Commit performed ";
	public static final String COMMIT_NO_BRANCH = "Commit is allowed only to branch";
	public static final String NO_COMMITS = "Branch may be created for existing commit";
	public static final String BRANCH_ALREADY_EXISTS = "Branch already exists";
	public static final String BRANCH_CREATED = "Branch created";
	public static final String BRANCH_NO_EXISTS = "Branch No Exists";
	public static final String BRANCH_RENAMED = "Branch renamed";
	public static final String ACTIVE_BRANCH = "Current branch cannot be removed";
	public static final String ONE_BRANCH = "At least one branch should exist";
	public static final String BRANCH_DELETED = "Branch deleted";
	public static final String NO_HEAD = " No head yet";
	public static final String BRANCH_NAME = " branch ";
	public static final String COMMIT_NAME = " commit ";
	public static final String SWITCHED = "Switched to ";
	public static final String WRONG_COMMIT_NAME = "no commit with the name ";
	public static final String SAME_AS_CURRENT = "The same commit as the current one";
	public static final String DIRECTORY_NO_COMMITTED = "Run commit before switching";
	private Instant lastCommitTimestamp;

	private GitRepositoryImpl(Path git) {
		this.gitPath = git.toString();
		commits = new HashMap<>();
		commitFiles = new HashMap<>();
		branches = new HashMap<>();
	}

	public static GitRepository init() throws Exception {
		Path path = Path.of(".").toAbsolutePath();
		Path git = path.resolve(GIT_FILE);
		GitRepository res = null;
		if (Files.exists(git)) {
			res = restoreFromFile(git);
		} else {
			res = new GitRepositoryImpl(git.normalize());

		}
		return res;
	}

	private static GitRepository restoreFromFile(Path gitPath) throws Exception {
		try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(gitPath))) {
			return (GitRepository) input.readObject();
		}
	}

	@Override
	public String commit(String commitMessage) {
		return head == null ? commitHeadNull(commitMessage) : commitHeadNoNull(commitMessage);
	}

	private String commitHeadNoNull(String commitMessage) {
		Branch branch = branches.get(head);
		if (branch == null) {
			return COMMIT_NO_BRANCH;
		}
		Commit commit = createCommit(commitMessage, commits.get(branch.commitName));
		branch.commitName = commit.commitName;
		return COMMIT_PERFORMED;
	}

	private Commit createCommit(String message, Commit prev) {
		Commit res = new Commit();
		res.commitName = getCommitName();
		res.commitMessage = message;
		res.prev = prev;
		res.timestamp = Instant.now();
		res.commitFiles = getCommitContent(res.commitName);
		commits.put(res.commitName, res);
		lastCommitTimestamp = res.timestamp;
		return res;
	}

	private List<CommitFile> getCommitContent(String commitName) {
		List<FileState> files = info();
		return files.stream().filter(fs -> fs.status == Status.UNTRACKED || fs.status == Status.MODIFIED)
				.map(fs -> toCommitFile(fs, commitName)).toList();
	}

	private CommitFile toCommitFile(FileState fs, String commitName) {
		Instant timeModified;
		try {
			timeModified = Files.getLastModifiedTime(fs.path).toInstant();
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
		List<String> content = getFileContent(fs.path);
		CommitFile res = new CommitFile(fs.path.toString(), timeModified, content, commitName);
		// Assumption neither rename nor delete
		commitFiles.put(fs.path.toString(), res);
		return res;
	}

	private List<String> getFileContent(Path path) {
		try {
			return Files.lines(path).toList();
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
	}

	private String getCommitName() {
		String res = "";
		do {
			res = Integer.toString(ThreadLocalRandom.current().nextInt(0x1000000, 0xfffffff), 16);
		} while (commits.containsKey(res));
		return res;
	}

	private String commitHeadNull(String commitMessage) {
		Commit commit = createCommit(commitMessage, null);
		createInternalBranch("master", commit);
		return COMMIT_PERFORMED;
	}

	private void createInternalBranch(String branchName, Commit commit) {
		if (branches.containsKey(branchName)) {
			throw new IllegalStateException(String.format("Branch %s already exists", branchName));
		}
		Branch branch = new Branch();
		branch.branchName = branchName;
		branch.commitName = commit.commitName;
		branches.put(branchName, branch);
		head = branchName;
	}

	@Override
	public List<FileState> info() {
		Path directoryPath = Path.of(".");
		try {
			return Files.list(directoryPath).map(p -> p.normalize()).filter(p -> !ignoreFilter(p)).map(p -> {
				try {
					return new FileState(p, getStatus(p));
				} catch (IOException e) {
					throw new RuntimeException(e.toString());
				}
			}).toList();
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	private boolean ignoreFilter(Path p) {
		// Assumption no nested directories
		return !Files.isRegularFile(p) || p.toString().matches(ignoreExpressions);
	}

	private Status getStatus(Path p) throws IOException {
		CommitFile commitFile = commitFiles.get(p.toString());
		return commitFile == null ? Status.UNTRACKED : getStatusFromCommitFile(commitFile, p);
	}

	private Status getStatusFromCommitFile(CommitFile commitFile, Path p) throws IOException {
		return Files.getLastModifiedTime(p).toInstant().compareTo(lastCommitTimestamp) > 0 ? Status.MODIFIED
				: Status.COMMITTED;
	}

	@Override
	public String createBranch(String branchName) {
		if (commits.isEmpty()) {
			return NO_COMMITS;
		} 
		if (branches.containsKey(branchName)) {
			return BRANCH_ALREADY_EXISTS;
		} 
		Commit commit = getCommit();
		createInternalBranch(branchName, commit);
		return BRANCH_CREATED;
	}

	private Commit getCommit() {
		Branch branch = branches.get(head);
		String commitName = branch != null ? branch.commitName : head;
		Commit res = commits.get(commitName);
		if (res == null) {
			throw new IllegalStateException("no commit with the name " + commitName);
		}
		return res;
	}

	@Override
	public String renameBranch(String branchName, String newName) {
		if (branches.containsKey(newName)) {
			return newName + " " + BRANCH_ALREADY_EXISTS;
		}
		Branch branch = branches.get(branchName);
		if (branch == null) {
			return branchName + " " + BRANCH_NO_EXISTS;
		}
		branch.branchName = newName;
		branches.remove(branchName);
		branches.put(newName, branch);
		if (head.equals(branchName)) {
			head = newName;
		}
		return BRANCH_RENAMED;
	}

	@Override
	public String deleteBranch(String branchName) {
		if (!branches.containsKey(branchName)) {
			return BRANCH_NO_EXISTS;
		}
		if (head.equals(branchName)) {
			return ACTIVE_BRANCH;
		}
		if (branches.size() == 1) {
			return ONE_BRANCH;
		}		
		branches.remove(branchName);
		return BRANCH_DELETED;
	}

	@Override
	public List<CommitMessage> log() {
		List<CommitMessage> res = new ArrayList<>();
		if (head == null) {
			return res;
		}
		Branch branch = branches.get(head);
		String commitName = branch != null ? branch.commitName : head;
		Commit commit = commits.get(commitName);
		if (commit == null) {
			throw new IllegalStateException("no commit with name " + commitName);
		}
		while (commit != null) {
			res.add(new CommitMessage(commit.commitName, commit.commitMessage));
			commit = commit.prev;
		}
		return res;
	}

	@Override
	public List<String> branches() {
		return branches.values().stream().map(b -> {
			String res = b.branchName;
			if (head.equals(res)) {
				res += "*";
			}
			return res;
		}).toList();
	}

	@Override
	public List<Path> commitContent(String commitName) {
		Commit commit = commits.get(commitName);
		if (commit == null) {
			throw new IllegalArgumentException(commitName + " doesn't exist");
		}
		return commit.commitFiles.stream().map(cf -> Path.of(cf.path)).toList();
	}

	@Override
	public String switchTo(String name) {
		List<FileState> fileStates = info();
		String res = SWITCHED + name;
		Commit commitTo = commitSwitched(name);
		Commit commitHead = getCommit();
		if (commitTo != null) {
			if (head.equals(name) || commitTo.commitName.equals(commitHead.commitName)) {
				res = name + SAME_AS_CURRENT;
			} else if (fileStates.stream().anyMatch(fs -> fs.status != Status.COMMITTED)) {
				res = DIRECTORY_NO_COMMITTED;
			} else {
				info().stream().forEach(fs -> {
					try {
						Files.delete(fs.path);
					} catch (IOException e) {
						throw new IllegalStateException("error in deleting files " + e.getMessage());
					}
				});
				switchProcess(commitTo);
				head = name;
				lastCommitTimestamp = Instant.now();
			}
		}
		return res;
	}

	private void writeFile(CommitFile cf) {
		try (PrintWriter writer = new PrintWriter(cf.path)) {
			cf.content.stream().forEach(writer::println);
		} catch (Exception e) {
			throw new IllegalStateException(e.toString());
		}
	}

	private void switchProcess(Commit commitTo) {
		// With assumption files are not removed from working directory
		Set<String> restoredFiles = new HashSet<>();
		try {
			while (commitTo != null) {
				commitTo.commitFiles.stream().forEach(cf -> {
					if (!restoredFiles.contains(cf.path)) {
						writeFile(cf);
						restoredFiles.add(cf.path);
					}
				});
				commitTo = commitTo.prev;
			}
		} catch (Exception e) {
			throw new IllegalStateException("error in switchForward functionality ");
		}
	}

	private Commit commitSwitched(String name) {
		Commit res = null;
		String commitName = name;
		Branch branch = branches.get(name);
		if (branch != null) {
			commitName = branch.commitName;
		}
		res = commits.get(commitName);
		if (res == null) {
			throw new IllegalArgumentException(WRONG_COMMIT_NAME + commitName);
		}
		return res;
	}

	@Override
	public String getHead() {
		if (head == null) {
			return NO_HEAD;
		}
		return (branches.containsKey(head) ? BRANCH_NAME : COMMIT_NAME) + head;
	}

	@Override
	public void save() {
		try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(Path.of(gitPath)))) {
			output.writeObject(this);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String addIgnoredFileNameExp(String regex) {
		ignoreExpressions += String.format("|(%s)", regex);
		return String.format("Regex for files ignored is %s", ignoreExpressions);
	}

}
