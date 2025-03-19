package Banking_System;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class App {
	 private static final String URL = "jdbc:postgresql://localhost:5432/bank";
	    private static final String USER = "postgres";
	    private static final String PASSWORD = "123";
	    
	    private static final Map<Integer, Double> accountCache = Collections.synchronizedMap(new HashMap<>());

	    static {
	        createDatabaseAndTable();
	    }
public static void main(String[] args) {
	 Scanner scanner = new Scanner(System.in);
	    System.out.println("*************Welcome to the Banking System**********************");
	        
	    while (true) {
	        System.out.println("1. Register\n2. Login\n3. Exit");
	        int choice = scanner.nextInt();
	        scanner.nextLine();
	            
	   switch (choice) {
	    case 1:
	        registerUser(scanner);
	        break;
	        case 2:
	        loginUser(scanner);
	        break;
	        case 3:
	        System.out.println("Exiting...");
	                    
	        return;
	            default:
	              System.out.println("Invalid choice. Try again.");
	            }
	        }
	    }
	    
	private static void createDatabaseAndTable() {
	    // Ensure PostgreSQL JDBC driver is loaded
	    try {
	        Class.forName("org.postgresql.Driver");
	    } catch (ClassNotFoundException e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	    // Step 1: Check if the database exists before creating
	 try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", USER, PASSWORD);
	         Statement stmt = conn.createStatement();
	         ResultSet rs = conn.getMetaData().getCatalogs()) {
	        
	        boolean databaseExists = false;
	        while (rs.next()) {
	            String dbName = rs.getString(1);
	            if ("Bank".equalsIgnoreCase(dbName)) {
	                databaseExists = true;
	                break;
	            }
	        }
	        
	        if (!databaseExists) {
	            stmt.executeUpdate("CREATE DATABASE Bank");
	            System.out.println("Database 'Bank' created successfully.");
	        } else {
	            System.out.println("Database 'Bank' already exists.");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    // Step 2: Connect to the Bank database and create the table
	    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
	         Statement stmt = conn.createStatement()) {
	        
	        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
	                "id SERIAL PRIMARY KEY, " +
	                "email VARCHAR(255) UNIQUE NOT NULL, " +
	                "user_password VARCHAR(255) NOT NULL, " +
	                "account_number INT UNIQUE NOT NULL, " +
	                "balance DOUBLE PRECISION DEFAULT 0)";
	        stmt.executeUpdate(createTableSQL);
	        System.out.println("Table 'users' is ready.");
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	   /* try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
	    	     Statement stm = conn.createStatement()) {  // <- Move this inside try()
	    	    
	    	    String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
	    	            "id SERIAL PRIMARY KEY, " +
	    	            "email VARCHAR(255) UNIQUE NOT NULL, " +
	    	            "password VARCHAR(255) NOT NULL, " +
	    	            "account_number INT UNIQUE NOT NULL, " +
	    	            "balance DOUBLE PRECISION DEFAULT 0)";
	    	    
	    	    stm.executeUpdate(createTableSQL);  // Use executeUpdate(), not executeLargeUpdate()
	    	    System.out.println("Table 'users' Created Successfully...");

	    	} catch (SQLException e) {
	    	    e.printStackTrace();
	    	}*/
	}
	
	    
	    private static void registerUser(Scanner scanner) {
	        System.out.print("Enter email: ");
	        String email = scanner.nextLine();
	        System.out.print("Enter password: ");
	        String user_password = scanner.nextLine();
	        
	      
	            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
	                String getMaxAccQuery = "SELECT COALESCE(MAX(account_number), 1000000) FROM users";
	                Statement stmt = conn.createStatement();
	                ResultSet rs = stmt.executeQuery(getMaxAccQuery);
	                int accountNumber = 1000001;
	                if (rs.next()) {
	                    accountNumber = rs.getInt(1) + 1;
	                }
	                
	                String insertQuery = "INSERT INTO users (email, user_password, account_number, balance) VALUES (?, ?, ?, 0)";
	                PreparedStatement pstmt = conn.prepareStatement(insertQuery);
	                pstmt.setString(1, email);
	                pstmt.setString(2,user_password);
	                pstmt.setInt(3, accountNumber);
	                pstmt.executeUpdate();
	                
	                System.out.println("🎉🎉Registration successful! Your account number is: " + accountNumber);
	            } catch (SQLException e) {
	            	if (e.getMessage().contains("duplicate key value violates unique constraint")) {
	                    System.out.println("❗❗Error: Email already exists. Registration unsuccessful.");
	                } else {
	                    e.printStackTrace();
	                }
	            }
	        };
	    
	    
	    private static void loginUser(Scanner scanner) {
	        System.out.print("Enter email: ");
	        String email = scanner.nextLine();
	        System.out.print("Enter password: ");
	        String user_password = scanner.nextLine();
	        
	      
	            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
	                String query = "SELECT account_number, balance FROM users WHERE email = ? AND user_password = ?";
	                PreparedStatement pstmt = conn.prepareStatement(query);
	                pstmt.setString(1, email);
	                pstmt.setString(2, user_password);
	                ResultSet rs = pstmt.executeQuery();
	                
	                if (rs.next()) {
	                    int accountNumber = rs.getInt("account_number");
	                    double balance = rs.getDouble("balance");
	                    accountCache.put(accountNumber, balance);
	                    System.out.println("🎉🎉Login successful! Your account number: " + accountNumber);
	                    performOperations(scanner, conn, accountNumber);
	                } else {
	                    System.out.println("❗❗Invalid email or password.");
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	        };
	    
	    private static synchronized int getIntInput(Scanner scanner) {
	        while (true) {
	            try {
	                return scanner.nextInt();
	            } catch (InputMismatchException e) {
	                scanner.nextLine(); // Clear invalid input
	                System.out.print("❗❗Invalid input. Enter a number: ");
	            }
	        }
	    }
	    
	    private static void performOperations(Scanner scanner, Connection conn, int accountNumber) {
	        while (true) {
	            System.out.println("1. Check Balance\n2. Deposit\n3. Withdraw\n4. Logout");
	            int choice = getIntInput(scanner); // Use the safe input method

	            switch (choice) {
	                case 1:
	                    System.out.println("Current balance: " + accountCache.getOrDefault(accountNumber, 0.0));
	                    break;
	                case 2:
	                    depositMoney(scanner, conn, accountNumber);
	                    break;
	                case 3:
	                    withdrawMoney(scanner, conn, accountNumber);
	                    break;
	                case 4:
	                    System.out.println("Logging out...");
	                    return;
	                default:
	                    System.out.println("Invalid choice. Try again.");
	            }
	        }
	    }

	    private static void depositMoney(Scanner scanner, Connection conn, int accountNumber) {
	        System.out.print("Enter amount to deposit: ");
	        double amount = scanner.nextDouble();
	        accountCache.put(accountNumber, accountCache.getOrDefault(accountNumber, 0.0) + amount);
	         updateBalance(conn, accountNumber, amount);
	        System.out.println("💵💵Deposit successful!");
	    }
	    
	    private static void withdrawMoney(Scanner scanner, Connection conn, int accountNumber) {
	        System.out.print("Enter amount to withdraw: ");
	        double amount = scanner.nextDouble();
	        if (accountCache.getOrDefault(accountNumber, 0.0) >= amount) {
	            accountCache.put(accountNumber, accountCache.get(accountNumber) - amount);
	            updateBalance(conn, accountNumber, -amount);
	            System.out.println("💵💵Withdrawal successful!");
	        } else {
	            System.out.println("‼️‼️Insufficient balance.");
	        }
	    }
	    
	    private static void updateBalance(Connection conn, int accountNumber, double amount) {
	        try {
	            String query = "UPDATE users SET balance = balance + ? WHERE account_number = ?";
	            PreparedStatement pstmt = conn.prepareStatement(query);
	            pstmt.setDouble(1, amount);
	            pstmt.setInt(2, accountNumber);
	            pstmt.executeUpdate();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
	
}
	

