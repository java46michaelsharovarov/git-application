package telran.git.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface InputOutput {
	
	String readString(String prompt);

	void writeObject(Object obj);
	
	default void close() {}

	default void writeLine(Object obj) {
		String str = obj + "\n";
		writeObject(str);		
	}

	default <R> R readObject(String prompt, String errorPrompt, Function<String, R> mapper) {
		R result = null;
		while (true) {
			String str = readString(prompt);
			try {
				result = mapper.apply(str);
				break;
			} catch (Exception e) {
				String message = e.getMessage();
				if (message == null) {
					message = "";
				}
				writeLine(errorPrompt + " " + message);
			}
		}
		return result;

	}
	
	default Integer readInt(String prompt, String errorPrompt) {
		return readObject(prompt, errorPrompt, Integer::parseInt);
	}
	
	default Integer readInt(String prompt, String errorPrompt, int min, int max) {
		return readObject(prompt, errorPrompt, s -> {
			int num = Integer.parseInt(s);
			if (num < min) {
				throw new RuntimeException("less than " + min);
			}
			if (num > max) {
				throw new RuntimeException("greater than " + max);
			}
			return num;			
		});
	}
	
	default long readLong(String prompt, String errorPrompt) {
		return readObject(prompt, errorPrompt, Long::parseLong);
	}
	
	default double readDouble(String prompt, String errorPrompt) {
		return readObject(prompt, errorPrompt, Double::parseDouble);
	}
	
	default String readPredicate(String prompt, String errorPrompt, Predicate<String> predicate) {
		return readObject(prompt, errorPrompt, s -> {
			if (!predicate.test(s)) {
				throw new RuntimeException();
			}
			return s;
		});
	}
	
	default String readOption (String prompt, String errorPrompt, List<String> options ) {
		return readPredicate(prompt, errorPrompt, options::contains);
	}
	
	default LocalDate readDate(String prompt, String errorPrompt) {
		return readObject(prompt, errorPrompt, LocalDate::parse);
	}
	
	default LocalDate readDate(String prompt, String errorPrompt, String format) {
		return readObject(prompt, errorPrompt, s -> 
			LocalDate.parse(s, DateTimeFormatter.ofPattern(format)));
	}

}