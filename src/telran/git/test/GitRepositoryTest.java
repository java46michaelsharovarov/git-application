package telran.git.test;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import telran.git.model.Status;
import telran.git.service.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitRepositoryTest {

	private static final String FOURTH_COMMIT = "fourth commit";
	private static final String THIRD_COMMIT = "third commit";
	private static final String SECOND_COMMIT = "second commit";
	private static final String FIRST_COMMIT = "first commit";
	private static final String HEW_CURRENT_BRANCH = "new_my_branch";
	private static final String MASTER = "master";
	private static final String MY_BRANCH = "my_branch";
	private static final String NO_EXISTS = "no_exists";
	static GitRepository gitRepository;
	static final String FILE1 = "file1.txt";
	static final String FILE2 = "file2.txt";
	static final String FILE3 = "file3.txt";

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Files.deleteIfExists(Path.of(GitRepository.GIT_FILE));
		Files.deleteIfExists(Path.of(FILE1));
		Files.deleteIfExists(Path.of(FILE2));
		Files.deleteIfExists(Path.of(FILE3));
	}

	@Test
	@Order(1)
	@DisplayName("testing methods init, save, adding regex")
	void initSaveTest() throws Exception {
		gitRepository = GitRepositoryImpl.init();
		assertEquals(2, gitRepository.addIgnoredFileNameExp("\\.project").split("\\|").length);
		gitRepository.save();
		gitRepository = GitRepositoryImpl.init();
		assertEquals(3, gitRepository.addIgnoredFileNameExp("\\.classpath").split("\\|").length);
		gitRepository.save();
		gitRepository = GitRepositoryImpl.init();
		assertEquals(4, gitRepository.addIgnoredFileNameExp("\\.gitignore").split("\\|").length);
		gitRepository.save();
	}
	
	@Test
	@Order(2)
	void createBranch_NO_COMMITS_Test() throws Exception {
		assertEquals(GitRepositoryImpl.NO_COMMITS, gitRepository.createBranch(MY_BRANCH));
	}
	
	@Test
	@Order(3)
	void getHead_NO_HEAD_Test() throws Exception {
		assertEquals(GitRepositoryImpl.NO_HEAD, gitRepository.getHead());
	}

	@Test
	@Order(4)
	@DisplayName("testing methods commit, commitContent")
	void commitAndCommitContentTest() throws Exception {
		Files.createFile(Path.of(FILE1));
		Files.createFile(Path.of(FILE2));
		assertEquals(GitRepositoryImpl.COMMIT_PERFORMED, gitRepository.commit(FIRST_COMMIT));
		
		List<String> expected = List.of(FILE1, FILE2);
		String commitName = gitRepository.log().stream().map(e -> e.commitName).collect(Collectors.toList()).get(0);
		List<String> actual = gitRepository.commitContent(commitName).stream().map(p -> p.toString()).collect(Collectors.toList());
		actual.sort(Comparator.naturalOrder());
		assertIterableEquals(expected, actual);
		
		Files.writeString(Path.of(FILE1), "Hello");
		assertEquals(GitRepositoryImpl.COMMIT_PERFORMED, gitRepository.commit(SECOND_COMMIT));
	}
	
	@Test
	@Order(5)
	void getHead_BRANCH_NAME_Test() throws Exception {
		assertEquals(GitRepositoryImpl.BRANCH_NAME + MASTER, gitRepository.getHead());
	}
	
	@Test
	@Order(6)
	void infoTest() throws Exception {
		Map<Path, Status> actual = gitRepository.info().stream().collect(Collectors.toMap(e -> e.path, e -> e.status));
		assertEquals(Status.COMMITTED, actual.get(Path.of(FILE1))); 
		assertEquals(Status.COMMITTED, actual.get(Path.of(FILE2))); 
		Files.writeString(Path.of(FILE1), "Hello File1");
		Files.createFile(Path.of(FILE3));
		actual = gitRepository.info().stream().collect(Collectors.toMap(e -> e.path, e -> e.status));
		assertEquals(Status.MODIFIED, actual.get(Path.of(FILE1))); 
		assertEquals(Status.COMMITTED, actual.get(Path.of(FILE2))); 
		assertEquals(Status.UNTRACKED, actual.get(Path.of(FILE3))); 
	}
	
	@Test
	@Order(7)
	void createBranchTest() throws Exception {
		assertEquals(GitRepositoryImpl.BRANCH_CREATED, gitRepository.createBranch(MY_BRANCH));
		assertEquals(GitRepositoryImpl.BRANCH_ALREADY_EXISTS, gitRepository.createBranch(MASTER));
	}
	
	@Test
	@Order(8)
	@DisplayName("testing methods renameBranch, branches")
	void renameBranchAndBranchesTest() throws Exception {
		assertEquals(NO_EXISTS + " " + GitRepositoryImpl.BRANCH_NO_EXISTS, gitRepository.renameBranch(NO_EXISTS, HEW_CURRENT_BRANCH));
		assertEquals(MASTER + " " + GitRepositoryImpl.BRANCH_ALREADY_EXISTS, gitRepository.renameBranch(MY_BRANCH, MASTER));
		
		List<String> expected = List.of(MY_BRANCH + "*", MASTER);
		assertEquals(expected, gitRepository.branches());
		
		assertEquals(GitRepositoryImpl.BRANCH_RENAMED, gitRepository.renameBranch(MY_BRANCH, HEW_CURRENT_BRANCH));
		
		expected = List.of(HEW_CURRENT_BRANCH + "*", MASTER);
		assertEquals(expected, gitRepository.branches());
	}
	
	@Test
	@Order(9)
	void deleteBranchTest() throws Exception {
		assertEquals(GitRepositoryImpl.BRANCH_NO_EXISTS, gitRepository.deleteBranch(NO_EXISTS));
		assertEquals(GitRepositoryImpl.ACTIVE_BRANCH, gitRepository.deleteBranch(HEW_CURRENT_BRANCH));
		assertEquals(GitRepositoryImpl.BRANCH_DELETED, gitRepository.deleteBranch(MASTER));
		List<String> expected = List.of(HEW_CURRENT_BRANCH + "*");
		assertEquals(expected, gitRepository.branches());
	}
	
	@Test
	@Order(10)
	void logTest() throws Exception {
		List<String> expected = List.of(FIRST_COMMIT, SECOND_COMMIT);
		List<String> actual = gitRepository.log().stream().map(e -> e.commitMessage).collect(Collectors.toList());
		actual.sort(Comparator.naturalOrder());
		assertIterableEquals(expected, actual);
		gitRepository.commit(THIRD_COMMIT);
		expected = List.of(FIRST_COMMIT, SECOND_COMMIT, THIRD_COMMIT);
		actual = gitRepository.log().stream().map(e -> e.commitMessage).collect(Collectors.toList());
		actual.sort(Comparator.naturalOrder());
		assertIterableEquals(expected, actual);
	}
	
	@Test
	@Order(11)
	void switchToTest() throws Exception {
		gitRepository.createBranch(MASTER);
		Files.writeString(Path.of(FILE3), "Hello File3");
		gitRepository.commit(FOURTH_COMMIT);
		String actual = gitRepository.switchTo(HEW_CURRENT_BRANCH);
		System.out.println(actual);
		gitRepository.save();
	}

}