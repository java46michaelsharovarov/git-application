package telran.git.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Menu implements Item {
	
	private static final int N_ASTERICS = 30;
	private String name;
	private ArrayList<Item> items;
	private int marginDivider;
	
	public Menu(String name, int marginDivider, ArrayList<Item> items) {
		this.name = name;
		this.items = items;
		this.marginDivider = marginDivider;
	}
	
	public Menu(String name, int marginDivider, Item... items) {
		this(name, marginDivider, new ArrayList<>(Arrays.asList(items)));
	}
	
	@Override
	public String displayName() {		
		return name;
	}

	@Override
	public void perform(InputOutput io) {
		while(true) {
			try {
				displayMenu(io);
				int itemNumber = io.readInt("Enter item number", "Wrong item number", 1, items.size());
				Item item = items.get(itemNumber - 1);
				item.perform(io);
				if(item.isExit()) {
					break;
				}
			} catch (Exception e) {
				io.writeLine(e.getMessage());
			}
		}
	}

	private void displayMenu(InputOutput io) {
		io.writeLine("*".repeat(N_ASTERICS));
		io.writeObject(" ".repeat(N_ASTERICS / marginDivider));
		io.writeLine(name);
		io.writeLine("*".repeat(N_ASTERICS));		
		IntStream.rangeClosed(1, items.size())
		.forEach(i -> io.writeLine(String.format("%d. %s" , i, items.get(i - 1).displayName())));
	}
	
	@Override
	public boolean isExit() {
		return false;
	}

}