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
	private HashMap<Path, CommitFile> commitFiles;
	private HashMap<String, Branch> branches;
	private String head; // name of current branch or commit
	private String ignoreExpressions = "(\\" + GIT_FILE + ")";

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
	public static final String WRONG_EXPRESSION = " Wrong regex";

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

	private String commitHeadNull(String commitMessage) {
		Commit commit = createCommit(commitMessage, null);
		createInternalBranch("master", commit.commitName);
		return COMMIT_PERFORMED;
	}

	private void createInternalBranch(String branchName, String commitName) {
		Branch branch = new Branch(branchName, commitName);
		branches.put(branchName, branch);
		head = branchName;
	}

	private String commitHeadNoNull(String commitMessage) {
		String res = null;
		Branch branch = branches.get(head);
		if (branch == null) {
			res = COMMIT_NO_BRANCH;
		} else {
			Commit commit = createCommit(commitMessage, commits.get(branch.commitName));
			branch.commitName = commit.commitName;
			res = COMMIT_PERFORMED;
		}
		return res;
	}

	private Commit createCommit(String message, Commit prev) {
		String commitName = getCommitName();
		Commit res = new Commit(commitName, message, prev, getCommitContent(commitName));
		commits.put(res.commitName, res);
		return res;
	}

	private String getCommitName() {
		String res = "";
		do {
			res = Integer.toString(ThreadLocalRandom.current().nextInt(0x1000000, 0xfffffff), 16);
		} while (commits.containsKey(res));
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
		CommitFile res = new CommitFile(fs.path, timeModified, content, commitName);
		// Assumption neither rename nor delete
		commitFiles.put(fs.path, res);
		return res;
	}

	private List<String> getFileContent(Path path) {
		try {
			return Files.lines(path).toList();
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
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
		CommitFile commitFile = commitFiles.get(p);
		return commitFile == null ? Status.UNTRACKED : getStatusFromCommitFile(commitFile, p);
	}

	private Status getStatusFromCommitFile(CommitFile commitFile, Path p) throws IOException {
		return Files.getLastModifiedTime(p).toInstant().compareTo(commitFile.modificationTime) > 0 ? Status.MODIFIED
				: Status.COMMITTED;
	}

	@Override
	public String createBranch(String branchName) {
		String res = null;
		if (commits.isEmpty()) {
			res = NO_COMMITS;
		} else if (branches.containsKey(branchName)) {
			res = BRANCH_ALREADY_EXISTS;
		} else {
			Commit commit = getCommit();
			createInternalBranch(branchName, commit.commitName);
			res = BRANCH_CREATED;
		}
		return res;
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
		String res = branchName + " " + BRANCH_NO_EXISTS;
		Branch branch = branches.get(branchName);
		if (branch != null) {
			if (branches.containsKey(newName)) {
				res = newName + " " + BRANCH_ALREADY_EXISTS;
			} else {
				branch.branchName = newName;
				branches.remove(branchName);
				branches.put(newName, branch);
				if (head.equals(branchName)) {
					head = newName;
				}
				res = BRANCH_RENAMED;
			}
		}
		return res;
	}

	@Override
	public String deleteBranch(String branchName) {
		String res = BRANCH_NO_EXISTS;
		if (branches.containsKey(branchName)) {
			if (head.equals(branchName)) {
				res = ACTIVE_BRANCH;
			} else if (branches.size() == 1) {
				res = ONE_BRANCH;
			} else {
				branches.remove(branchName);
				res = BRANCH_DELETED;
			}
		}
		return res;
	}

	@Override
	public List<CommitMessage> log() {
		List<CommitMessage> res = new ArrayList<>();
		if (head != null) {
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
		return commit.commitFiles.stream().map(cf -> cf.path).toList();
	}

	@Override
	public String switchTo(String name) {
		// Not implemented
		return null;
	}

	@Override
	public String getHead() {
		String res = NO_HEAD;
		if (head != null) {
			res = branches.containsKey(head) ? BRANCH_NAME : COMMIT_NAME;
			res += head;
		}
		return res;
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
		checkRegex(regex);
		ignoreExpressions += String.format("|(%s)", regex);
		return String.format("Regex for files ignored is %s", ignoreExpressions);
	}

	private void checkRegex(String regex) {
		try {
			"test".matches(regex);
		} catch (Exception e) {
			throw new IllegalArgumentException(regex + WRONG_EXPRESSION);
		}
	}

}