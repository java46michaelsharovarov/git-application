package telran.git.controller;

import telran.git.service.GitRepositoryImpl;
import telran.git.view.*;

public class GitRepositoryAppl {

	public static void main(String[] args) {
		InputOutput io = new ConsoleInputOutput();
		try {
			Item [] items = GitMenu.getGitRepositoryItems(GitRepositoryImpl.init());
			Menu menu = new Menu("Git Console Commands Menu", 10, items);
			menu.perform(io);
		} catch (Exception e) {
			io.writeLine("Error: " + e.getMessage());
		}
	}

}
