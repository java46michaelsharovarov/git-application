package telran.git.controller;

import java.util.List;

import telran.git.model.CommitMessage;
import telran.git.model.FileState;
import telran.git.model.Status;
import telran.git.service.GitRepository;
import telran.git.view.*;

public class GitMenu {
	
	private static final String NO_BRANCHES = "no branches";
	private static final String NOTHING_TO_LOG = "nothing to log";
	private static final String BRANCH_NAME = "branch name";
	private static final String BRANCH_NO_EXIST = "branch doesn't exist";
	private static final String FOLDER_IS_EMPTY = "folder is empty";
	private static GitRepository gitRepository;
	private static final String NOTHING_COMMIT = "nothing to commit";

	public static Item[] getGitRepositoryItems(GitRepository gitRepository) {
		GitMenu.gitRepository = gitRepository;
		Item[] itemsRes = { Item.of("Commit", GitMenu::commitMethod),
				Item.of("Log", GitMenu::logMethod),
				Item.of("Info", GitMenu::infoMethod),
				Item.of("Commit content", GitMenu::commitContentMethod),
				Item.of("Create branch", GitMenu::createBranchMethod),
				Item.of("Rename branch", GitMenu::renameBranchMethod),
				Item.of("Delete branch", GitMenu::deleteBranchMethod),
				Item.of("Branches list", GitMenu::branchesMethod),
				Item.of("Switch to", GitMenu::switchToMethod),
				Item.of("Add ignore regex", GitMenu::addRegexMethod),
				Item.of("Exit and save", io -> gitRepository.save(), true),
				Item.exit() 
				};
		return itemsRes;
	}

	static void commitMethod(InputOutput io) {
		List<FileState> states = gitRepository.info();
		if(states.isEmpty() || states.stream().allMatch(fs -> fs.status == Status.COMMITTED)) {
			io.writeLine(NOTHING_COMMIT);
			return;
		}
		String commitMessage = io.readString("Enter commit message");
		io.writeLine(gitRepository.commit(commitMessage));
	}

	static void addRegexMethod(InputOutput io) {
		String regex = io.readPredicate("Enter regular expression", "Wrong regular expression", e -> {
			try {
				"test".matches(e);
			} catch (Exception e1) {
				return false;
			}
			return true;
		});
		io.writeLine(gitRepository.addIgnoredFileNameExp(regex));
	}

	static void commitContentMethod(InputOutput io) {
		String commitName = io.readString("Enter commit name");
		io.writeLine(gitRepository.commitContent(commitName));
	}

	static void switchToMethod(InputOutput io) {
		String commitBranch = io.readString("Enter either branch or commit name for switching to");
		io.writeLine(gitRepository.switchTo(commitBranch));
	}

	static void infoMethod(InputOutput io) {
		List<FileState> list = gitRepository.info();
		printingList(io, list, FOLDER_IS_EMPTY);
	}

	static void createBranchMethod(InputOutput io) {
		String branchName = enterBranchName(io, BRANCH_NAME);
		io.writeLine(gitRepository.createBranch(branchName));
	}

	static void renameBranchMethod(InputOutput io) {
		String branchName = enterBranchName(io, "old branch name");
		if(isExistBranch(io, branchName)) {
			String newBranchName = enterBranchName(io, "new branch name");
			io.writeLine(gitRepository.renameBranch(branchName, newBranchName));
		}
	}

	private static boolean isExistBranch(InputOutput io, String branchName) {
		if (!isExist(branchName)) {
			io.writeLine(BRANCH_NO_EXIST);
			return false;
		}
		return true;
	}

	static void deleteBranchMethod(InputOutput io) {
		String branchName = enterBranchName(io, BRANCH_NAME);
		isExistBranch(io, branchName);
		io.writeLine(gitRepository.deleteBranch(branchName));
	}

	private static boolean isExist(String branchName) {
		return gitRepository.branches().stream().anyMatch(name -> name.contains(branchName));
	}

	private static String enterBranchName(InputOutput io, String prompt) {
		return io.readPredicate("Enter " + prompt, "Wrong Branch name", t -> t.matches("\\w{4,}"));
	}

	static void logMethod(InputOutput io) {
		List<CommitMessage> list = gitRepository.log();
		printingList(io, list, NOTHING_TO_LOG);
	}

	static void branchesMethod(InputOutput io) {
		List<String> list = gitRepository.branches();
		printingList(io, list, NO_BRANCHES);
	}

	private static void printingList(InputOutput io, List<?> list, String message) {
		if(list.isEmpty()) {
			io.writeLine(message);
			return;
		}
		list.forEach(io::writeLine);
	}
}
