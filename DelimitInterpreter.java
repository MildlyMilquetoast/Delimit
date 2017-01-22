/*
 * Input is via a file called "delimitInput"
 * The code is given through a file called "delimitCode"
 *
 * Output is through stdOut
 *
 * The starting delimiter is given as the first command line argument
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Stack;


public class DelimitInterpreter {
	
	//** Constants **//
	// These should be a enums, but I'm tired and lazy
	
	// Used for state
	public static final int EXECUTE = 0; 
	public static final int SKIP = 1; // used for command 29
	public static final int PUSH = 2; // used for command 26
	
	//used for operation(int type)
	public static final int ADD = 0;
	public static final int SUB = 1;
	public static final int MUL = 2;
	public static final int DIV = 3;
	public static final int MOD = 4;
	
	//** Variables **//
	
	// Countdown timers used for certain commands
	public static int skipCountdown = 0;
	public static int pushCountdown = 0;
	
	// Scanner used for parsing the code
	public static Scanner parser;
	
	// Scanner for User input
	public static Scanner stdIn;
	
	// Data storage spaces (used with get/put)
	public static int[] data = new int[5];
	
	// The stack
	public static Stack<Integer> stack = new Stack<Integer>();
	
	// String representing the code
	public static String code = "";
	
	// Delimiter for the code
	public static String delimiter = " ";
	
	//**  Main Method **//
	
	public static void main(String[] args) {
		
		// Sets starting delimiter
		if(args.length > 0) delimiter = args[0];
		
		String inputFileName = "delimitInput";
		
		// Creates a new Scanner for user input using the given fileName
		try {
			stdIn = new Scanner(new File(inputFileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String codeFileName = "delimitCode";
		
		// Creates an InputStreamReader for the code using the given fileName
		InputStreamReader reader = readerInit(codeFileName);
		
		// Populates the code string using the reader
		boolean inputLeft = true;
		while(inputLeft){
			
			try {
				
				int next = reader.read();
				
				if(next < 0){
					inputLeft = false;
				} else {
					
					code += (char) next;
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		// A scanner to parse the code
		resetParser(code);
		
		if(!parser.hasNext()) System.exit(0); // If there isn't any code, just leave
		
		// used to track whether to execute, skip, or push the token onto the stack
		int state = EXECUTE;
		
		// Main code parsing/execution loop
		while(true){
			
			String token = getNextToken();
			
			if(skipCountdown > 0) state = SKIP;
			if(pushCountdown > 0) state = PUSH;
			
			switch(state){
			case(EXECUTE):
				
				executeCommand(getCommand(token));
				break;
			
			case(PUSH):
				
				while(pushCountdown > 0){
					
					pushCountdown--;
					
					for(int i = token.length() - 1; i >= 0; i--){
						
						push((int) token.charAt(i));
						
					}
					
				}
				
				state = EXECUTE;
				
				break;
			
			case(SKIP):
				
				skipCountdown--;
				
				if(skipCountdown <= 0) state = EXECUTE;
				
				break;
			
			}
			
		}
		
		
		
	}
	
	//** Other Methods **//
	
	// Executes a given command
	public static void executeCommand(int command){
		
		switch(command){
		case(0):case(1):case(2):case(3):case(4):
		case(5):case(6):case(7):case(8):case(9): push(command); break;
		
		case(10): operate(ADD); break;
		case(11): operate(SUB); break;
		case(12): operate(MUL); break;
		case(13): operate(DIV); break;
		case(14): operate(MOD); break;
		
		case(15): push(push(pop())); break;
		case(16): pop(); break;
		case(17): int a = pop(); int b = pop(); push(a); push(b); break;
		
		case(18): push(pop() == 0 ? 1 : 0); break;
		case(19): push(pop() < pop() ? 1 : 0); break;
		
		case(20): get(); break;
		case(21): put(); break;
		
		case(22): push(getIntInput()); break;
		case(23): push(getCharInput()); break;
		case(24): System.out.print(pop() + " "); break;
		case(25): System.out.print((char) pop()); break;
		
		case(26): pushCountdown++; break;
		
		case(27): pushDelimiter(); break;
		case(28): setDelimiter(); break;
		
		case(29): skipCountdown += pop(); break;
		case(30): repeatCommand(); break;
		
		case(31): System.exit(0); break;
		
		}
		
	}
	
	// Command 28
	public static void setDelimiter(){
		
		if(!stack.isEmpty()){
			
			delimiter = "";
			
			while(!stack.isEmpty() && push(pop()) >= 0){
				
				delimiter += (char) pop();
				
			}
			
			pop();
			
		}
		
	}
	
	// What it says on the tin - command 27
	public static void pushDelimiter(){
		
		push(-1);
		
		for(int i = delimiter.length() - 1; i >= 0; i--){
			
			push((int) delimiter.charAt(i));
			
		}
		
	}
	
	// Command 20 - get
	public static void get(){
		
		int x = pop();
		
		if(x < 0){ // access data
			
			try{
				
				push(data[-x - 1]);
				
			} catch(IndexOutOfBoundsException e){
				push(0);
			}
			
		} else { // access code
			
			try{
				
				push((int) code.charAt(x));
				
			} catch(IndexOutOfBoundsException e){
				push(0);
			}
			
		}
		
	}
	
	// Command 21 - put
	public static void put(){
		
		int x = pop();
		int v = pop();
		
		if(x < 0){ // Modify data
			
			x = -x - 1;
			
			if(x >= data.length){
				
				data = Arrays.copyOf(data, x + 1);
				
			}
			
			data[x] = v;
			
		} else { // Modify code
			
			String notProcessed = parser.findInLine(".*");
			
			char[] s = code.toCharArray();
			
			int lengthDiff = code.length() - notProcessed.length();
			
			if(x < lengthDiff){
				
				s[x] = (char) v;
				code = String.valueOf(s);
				
				resetParser(notProcessed);
				
				
			} else { // Modifying code that needs to be changed in the parser as well.
				
				if(x < code.length()){ // In bounds
					
					s[x] = (char) v;
					code = String.valueOf(s);
					
					s = notProcessed.toCharArray();
					s[x - lengthDiff] = (char) v;
					notProcessed = String.valueOf(s);
					
					resetParser(notProcessed);
					
				} else { // Out of bounds - make code longer, pad w/ 0s
					
					while(x > code.length()){
						
						code += (char) 0;
						notProcessed += (char) 0;
						
					}
					
					code += (char) v;
					notProcessed += (char) v;
					
				}
				
			}
			
		}
		
	}
	
	// Command 30 - repeat command
	public static void repeatCommand(){
		
		int countdown = pop();
		
		int command = getCommand(getNextToken());
		
		while(countdown > 0){
			
			executeCommand(command);
			countdown--;
			
		}
		
	}
	
	// Used in conjunction with executeCommand(...)
	public static void operate(int type){
		
		int a = pop();
		int b = pop();
		int x = 0;
		
		try {
			switch(type){
			case(ADD): x = b + a; break;
			case(SUB): x = b - a; break;
			case(MUL): x = b * a; break;
			case(DIV): x = b / a; break;
			case(MOD): x = b % a; break;
			}
		} catch (ArithmeticException e) {
			System.exit(1);
		}
		
		push(x);
		
	}
	
	// Gets user input as a character
	public static int getCharInput(){
		
		if(!stdIn.hasNext()) return -1;
		
		char c = stdIn.findInLine(".").charAt(0);
		
		System.out.print("char found: " + c);
		
		return (int) c;
		
	}
	
	// Gets user input as an integer
	public static int getIntInput(){
		
		if(!stdIn.hasNextInt()) return -1;
		
		return stdIn.nextInt();
		
	}
	
	// Returns the command # of a given token
	public static int getCommand(String token){
		
		int sum = 0;
		
		for(char c : token.toCharArray()){
			
			sum += (int) c;
			
		}
		
		return sum % 32;
		
	}
	
	// Gets the next token from a Scanner - glorified .next() method (because I feel like it!)
	public static String getNextToken(){
		
		if(!parser.hasNext()) resetParser(code);
		
		return parser.next();
		
	}
	
	// Resets the Scanner used for parsing the code
	public static void resetParser(String s){
		
		parser = new Scanner(s);
		
		try {
			parser.useDelimiter(delimiter);
		} catch (Exception e) {
			System.exit(2);
		}
		
	}
	
	// Creates an InputStreamReader using a given file name
	public static InputStreamReader readerInit(String fileName){
		
		try {
			
			FileInputStream stream = new FileInputStream(fileName);
			InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
			return reader;
			
		} catch (FileNotFoundException e) { // No file found
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) { // Should never happen
			e.printStackTrace();
		}
		
		return null; //no file to read from
		
	}
	
	// A simple push method for easier pushing (similar to pop method)
	public static int push(int a){
		
		return stack.push(a);
		
	}
	
	// A special pop method to return 0 if the stack is empty
	public static int pop(){
		
		try{
			return stack.pop();
		}catch(Exception e){ //Error b/c of empty stack
			return 0;
		}
		
	}
	
}
