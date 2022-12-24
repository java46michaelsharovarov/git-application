package telran.git.test;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import telran.git.service.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitRepositoryTest {
	
	GitRepository gitRepository;
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
		assertEquals(2, gitRepository.addIgnoredFileNameExp(".*\\.").split("\\|").length);
		gitRepository.save();
		gitRepository = GitRepositoryImpl.init();
		assertEquals(3, gitRepository.addIgnoredFileNameExp(".*\\.").split("\\|").length);

	}

}