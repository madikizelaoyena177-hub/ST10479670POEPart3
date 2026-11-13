package com.mycompany.main1;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main1 {

    private static final Scanner scanner = new Scanner(System.in);

    // Registered users (kept in memory while the program runs)
    private static final ArrayList<User> users = new ArrayList<>();

    // The user who is currently logged in
    private static User currentUser = null;

    public static void main(String[] args) {

        // 1. Users must register / log in before they can send messages.
        if (!startScreen()) {
            System.out.println("Exiting QuickChat. Goodbye!");
            return;
        }

        // 2. Welcome message
        System.out.println("Welcome to QuickChat, " + currentUser.username + ".");

        // Read any previously stored messages from the JSON file into the stored array
        int loaded = Message.loadStoredMessages();
        if (loaded > 0) {
            System.out.println(loaded + " stored message(s) loaded from " + Message.getJsonPath() + ".");
        }

        // 5. User decides up front how many messages they want to enter
        int maxMessages = readPositiveInt("How many messages do you wish to enter? ");
        int messagesEntered = 0;

        // 4. Run until the user chooses Quit
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("Please choose an option:");
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            System.out.print("Option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    if (messagesEntered >= maxMessages) {
                        System.out.println("You have already entered all " + maxMessages
                                + " messages you planned to send.");
                    } else {
                        // Keep going until the planned number is reached (or the user stops)
                        while (messagesEntered < maxMessages) {
                            messagesEntered++;
                            boolean carryOn = captureMessage(messagesEntered - 1);
                            if (!carryOn) {
                                break;
                            }
                        }
                        if (messagesEntered >= maxMessages) {
                            System.out.println("\nTotal number of messages sent: "
                                    + Message.returnTotalMessages());
                        }
                    }
                    break;
                case "2":
                    System.out.println(Message.printMessages());
                    break;
                case "3":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                case "4":
                    storedMessagesMenu();
                    break;
                default:
                    System.out.println("Invalid option. Please enter 1, 2, 3 or 4.");
            }
        }
    }

    // ==================================================================
    // Stored messages menu (uses the arrays in the Message class)
    // ==================================================================
    private static void storedMessagesMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("=== Stored Messages ===");
            System.out.println("a) Display the sender and recipient of all stored messages");
            System.out.println("b) Display the longest stored message");
            System.out.println("c) Search for a message ID");
            System.out.println("d) Search for all messages for a particular recipient");
            System.out.println("e) Delete a message using the message hash");
            System.out.println("f) Display a report of all stored messages");
            System.out.println("g) Back to main menu");
            System.out.print("Option: ");

            String choice = scanner.nextLine().trim().toLowerCase();

            switch (choice) {
                case "a":
                    System.out.println(Message.displaySenderAndRecipient());
                    break;
                case "b":
                    System.out.println(Message.displayLongestMessage());
                    break;
                case "c":
                    System.out.print("Enter the message ID: ");
                    String id = scanner.nextLine().trim();
                    Message found = Message.findByMessageID(id);
                    if (found == null) {
                        System.out.println("No message found with ID " + id + ".");
                    } else {
                        System.out.println("Recipient: " + found.getRecipient());
                        System.out.println("Message: " + found.getMessageText());
                    }
                    break;
                case "d":
                    System.out.print("Enter the recipient cell number: ");
                    String recipient = scanner.nextLine().trim();
                    ArrayList<String> results = Message.searchByRecipient(recipient);
                    if (results.isEmpty()) {
                        System.out.println("No messages found for " + recipient + ".");
                    } else {
                        for (String text : results) {
                            System.out.println("\"" + text + "\"");
                        }
                    }
                    break;
                case "e":
                    System.out.print("Enter the message hash: ");
                    String hash = scanner.nextLine().trim();
                    System.out.println(Message.deleteByHash(hash));
                    break;
                case "f":
                    System.out.println(Message.displayReport(Message.getStoredMessages()));
                    break;
                case "g":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please enter a letter from a to g.");
            }
        }
    }

    // ==================================================================
    // Registration and login
    // ==================================================================

    // Shows Register / Login / Exit until the user logs in (true) or exits (false)
    private static boolean startScreen() {
        while (true) {
            System.out.println();
            System.out.println("=== QuickChat ===");
            System.out.println("1) Register");
            System.out.println("2) Login");
            System.out.println("3) Exit");
            System.out.print("Option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    register();
                    break;
                case "2":
                    if (login()) {
                        return true;
                    }
                    break;
                case "3":
                    return false;
                default:
                    System.out.println("Invalid option. Please enter 1, 2 or 3.");
            }
        }
    }

    // Registers a new user: username, password and South African cell phone number
    private static void register() {
        System.out.println("\n=== Register ===");

        // Username: keep asking until it is valid and not already taken
        String username;
        while (true) {
            System.out.print("Choose a username (no more than 5 characters, must contain an underscore): ");
            username = scanner.nextLine().trim();

            if (!checkUserName(username)) {
                System.out.println("Username is not correctly formatted. Please ensure that your "
                        + "username contains an underscore and is no more than five characters in length.");
            } else if (findUser(username) != null) {
                System.out.println("That username is already taken. Please choose another one.");
            } else {
                System.out.println("Username successfully captured.");
                break;
            }
        }

        // Password: keep asking until it meets the complexity rules
        String password;
        while (true) {
            System.out.print("Choose a password (at least 8 characters, with a capital letter, "
                    + "a number and a special character): ");
            password = scanner.nextLine().trim();

            if (checkPasswordComplexity(password)) {
                System.out.println("Password successfully captured.");
                break;
            }
            System.out.println("Password is not correctly formatted. Please ensure that the password "
                    + "contains at least eight characters, a capital letter, a number and a special character.");
        }

        // Cell phone number: South African format
        String cell;
        while (true) {
            System.out.print("Enter your South African cell phone number (e.g. +27718693002): ");
            cell = scanner.nextLine().trim();

            if (checkSouthAfricanCell(cell)) {
                System.out.println("Cell phone number successfully added.");
                break;
            }
            System.out.println("Cell phone number is incorrectly formatted. It must start with +27 "
                    + "followed by 9 digits (e.g. +27718693002).");
        }

        users.add(new User(username, password, cell));
        System.out.println("\nRegistration successful. You can now log in.");
    }

    // Logs a user in with username and password. Returns true on success.
    private static boolean login() {
        System.out.println("\n=== Login ===");

        if (users.isEmpty()) {
            System.out.println("No users are registered yet. Please register first.");
            return false;
        }

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        User user = findUser(username);
        if (user != null && user.password.equals(password)) {
            currentUser = user;
            System.out.println("Login successful.");
            return true;
        }

        System.out.println("Username or password incorrect, please try again.");
        return false;
    }

    // Username: contains an underscore and is no more than 5 characters
    static boolean checkUserName(String username) {
        return username.contains("_") && username.length() <= 5;
    }

    // Password: at least 8 characters, a capital letter, a number and a special character
    static boolean checkPasswordComplexity(String password) {
        if (password.length() < 8) {
            return false;
        }
        boolean hasCapital = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasCapital = true;
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            } else if (!Character.isLetter(c)) {
                hasSpecial = true;
            }
        }
        return hasCapital && hasNumber && hasSpecial;
    }

    // South African cell number: +27 followed by exactly 9 digits (e.g. +27718693002)
    static boolean checkSouthAfricanCell(String cell) {
        return cell.matches("^\\+27\\d{9}$");
    }

    // Looks up a registered user by username (case-sensitive); null if not found
    private static User findUser(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return u;
            }
        }
        return null;
    }

    // ==================================================================
    // Messaging
    // ==================================================================

    // Reads one message from the user, then lets them send / disregard / store it.
    // Returns false if the loop should stop early.
    private static boolean captureMessage(int messageNumber) {
        System.out.println("\n--- Message " + (messageNumber + 1) + " ---");

        // Recipient: keep asking until the number is valid
        String recipient;
        while (true) {
            System.out.print("Enter recipient cell number (with international code, e.g. +27718693002): ");
            recipient = scanner.nextLine().trim();
            Message check = new Message(messageNumber, recipient, "check");
            String result = check.checkRecipientCell();
            System.out.println(result);
            if (result.equals("Cell phone number successfully captured.")) {
                break;
            }
        }

        // Message text: keep asking until it is 250 characters or fewer
        String text;
        while (true) {
            System.out.print("Enter your message (max 250 characters): ");
            text = scanner.nextLine();
            if (text.trim().isEmpty()) {
                System.out.println("The message cannot be empty.");
                continue;
            }
            if (text.length() > 250) {
                System.out.println("Please enter a message of less than 250 characters.");
            } else {
                System.out.println("Message sent");
                break;
            }
        }

        Message message = new Message(messageNumber, recipient, text);
        System.out.println(message.getMessageIDMessage());
        System.out.println("Message Hash: " + message.getMessageHash());

        // Send / Disregard / Store
        while (true) {
            System.out.println("\nWhat would you like to do with this message?");
            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            System.out.print("Option: ");
            String option = scanner.nextLine().trim();

            if (option.equals("1") || option.equals("2") || option.equals("3")) {
                String result = message.sentMessage(Integer.parseInt(option));
                System.out.println(result);

                if (option.equals("1")) {
                    // 7. Show the full details after sending
                    System.out.println("\n" + message.getDetails());
                }
                return true;
            }
            System.out.println("Invalid option. Please choose 1, 2 or 3.");
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the message below
            }
            System.out.println("Please enter a whole number greater than 0.");
        }
    }

    // ==================================================================
    // User class: holds one registered user's details
    // ==================================================================
    private static class User {
        private final String username;
        private final String password;
        private final String cellNumber;

        User(String username, String password, String cellNumber) {
            this.username = username;
            this.password = password;
            this.cellNumber = cellNumber;
        }
    }

    // ==================================================================
    // Message class (nested so everything lives in the single Main class)
    // ==================================================================
    public static class Message {

        // ---- The arrays required by Part 3 (filled at run time, never hard-coded) ----
        private static final ArrayList<Message> sentMessages = new ArrayList<>();
        private static final ArrayList<Message> disregardedMessages = new ArrayList<>();
        private static final ArrayList<Message> storedMessages = new ArrayList<>();
        private static final ArrayList<String> messageHashes = new ArrayList<>();
        private static final ArrayList<String> messageIDs = new ArrayList<>();

        // File that stored messages are written to and read from
        private static String jsonPath = "messages.json";

        // ---- Data for ONE message ----
        private final String messageID;
        private final int messageNumber;
        private final String sender;
        private final String recipient;
        private final String messageText;
        private final String messageHash;

        // Constructor used by the real app: the ID is generated automatically
        public Message(int messageNumber, String recipient, String messageText) {
            this(generateMessageID(), messageNumber, recipient, messageText);
        }

        // Constructor used by the unit tests: lets us supply a known ID
        public Message(String messageID, int messageNumber, String recipient, String messageText) {
            this(messageID, messageNumber, defaultSender(), recipient, messageText);
        }

        // Full constructor: also lets us supply the sender (used when reading the JSON file)
        public Message(String messageID, int messageNumber, String sender, String recipient, String messageText) {
            this.messageID = messageID;
            this.messageNumber = messageNumber;
            this.sender = sender;
            this.recipient = recipient;
            this.messageText = messageText;
            this.messageHash = createMessageHash();
        }

        // The sender is the logged-in user's cell number ("Developer" when nobody is logged in)
        private static String defaultSender() {
            return currentUser != null ? currentUser.cellNumber : "Developer";
        }

        // Random ten-digit number (leading zeros allowed, e.g. 0012345678)
        private static String generateMessageID() {
            long number = ThreadLocalRandom.current().nextLong(10_000_000_000L);
            return String.format("%010d", number);
        }

        // Boolean: the message ID must be no more than ten characters
        public boolean checkMessageID() {
            return messageID.length() <= 10;
        }

        // String: the recipient must start with "+" (international code) and be digits only.
        // Country code (1-3 digits) followed by no more than ten digits.
        public String checkRecipientCell() {
            if (recipient != null && recipient.matches("^\\+\\d{1,3}\\d{1,10}$")) {
                return "Cell phone number successfully captured.";
            }
            return "Cell phone number is incorrectly formatted or does not contain an "
                    + "international code. Please correct the number and try again.";
        }

        // String: checks the 250 character limit (used by the unit tests)
        public String checkMessageLength() {
            if (messageText.length() <= 250) {
                return "Message ready to send.";
            }
            int tooMany = messageText.length() - 250;
            return "Message exceeds 250 characters by " + tooMany + "; please reduce the size.";
        }

        // Boolean version of the length check (used by the menu)
        public boolean isMessageLengthValid() {
            return messageText.length() <= 250;
        }

        // String: first two digits of ID : message number : FIRSTLAST (all caps)
        public String createMessageHash() {
            String firstTwo = messageID.substring(0, 2);

            String[] words = messageText.trim().split("\\s+");
            String firstWord = cleanWord(words[0]);
            String lastWord = cleanWord(words[words.length - 1]);

            return (firstTwo + ":" + messageNumber + ":" + firstWord + lastWord).toUpperCase();
        }

        // Removes punctuation such as "," or "?" so "tonight?" becomes "tonight"
        private String cleanWord(String word) {
            return word.replaceAll("[^A-Za-z0-9]", "");
        }

        // ==============================================================
        // Populating the arrays
        // ==============================================================

        // Adds a message to the array that matches its flag ("Sent", "Stored" or "Disregard"),
        // and records its hash and ID in the hash and ID arrays. Does not touch the JSON file.
        public static void addByFlag(Message message, String flag) {
            switch (flag.trim().toLowerCase()) {
                case "sent":
                    sentMessages.add(message);
                    break;
                case "stored":
                    storedMessages.add(message);
                    break;
                case "disregard":
                case "disregarded":
                    disregardedMessages.add(message);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown message flag: " + flag);
            }
            messageHashes.add(message.messageHash);
            messageIDs.add(message.messageID);
        }

        // String: handles the user's choice. 1 = send, 2 = disregard, 3 = store
        public String sentMessage(int choice) {
            switch (choice) {
                case 1:
                    addByFlag(this, "sent");
                    return "Message successfully sent.";
                case 2:
                    addByFlag(this, "disregard");
                    return "Message disregarded.";
                case 3:
                    storeMessage();
                    return "Message successfully stored.";
                default:
                    return "Invalid option. Please choose 1, 2 or 3.";
            }
        }

        // Stores the message in the stored array and saves all stored messages to the JSON file
        public void storeMessage() {
            addByFlag(this, "stored");
            saveStoredMessages();
        }

        // ==============================================================
        // JSON file: write and read the stored messages
        // ==============================================================

        // Simple holder that Gson converts to and from JSON
        private static class StoredRecord {
            String messageID;
            int messageNumber;
            String messageHash;
            String sender;
            String recipient;
            String message;
        }

        // Writes every message in the stored array to the JSON file
        public static void saveStoredMessages() {
            List<StoredRecord> records = new ArrayList<>();
            for (Message m : storedMessages) {
                StoredRecord r = new StoredRecord();
                r.messageID = m.messageID;
                r.messageNumber = m.messageNumber;
                r.messageHash = m.messageHash;
                r.sender = m.sender;
                r.recipient = m.recipient;
                r.message = m.messageText;
                records.add(r);
            }

            // Build the JSON text by hand: a list of objects, one per stored message
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < records.size(); i++) {
                StoredRecord r = records.get(i);
                json.append("  {\n");
                json.append("    \"messageID\": \"").append(escapeJson(r.messageID)).append("\",\n");
                json.append("    \"messageNumber\": ").append(r.messageNumber).append(",\n");
                json.append("    \"messageHash\": \"").append(escapeJson(r.messageHash)).append("\",\n");
                json.append("    \"sender\": \"").append(escapeJson(r.sender)).append("\",\n");
                json.append("    \"recipient\": \"").append(escapeJson(r.recipient)).append("\",\n");
                json.append("    \"message\": \"").append(escapeJson(r.message)).append("\"\n");
                json.append("  }");
                if (i < records.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("]");

            try (Writer writer = Files.newBufferedWriter(Paths.get(jsonPath))) {
                writer.write(json.toString());
            } catch (IOException e) {
                System.out.println("Could not save to JSON file: " + e.getMessage());
            }
        }

        // Reads the JSON file into the stored array. Returns how many messages were loaded.
        //
        // Attribution: the file is read as text and each "key": value pair is found with a
        // regular expression, using the Java regex classes Pattern and Matcher:
        // Oracle, "java.util.regex.Pattern", Java Platform SE API documentation,
        // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html
        public static int loadStoredMessages() {
            Path path = Paths.get(jsonPath);
            if (!Files.exists(path)) {
                return 0;
            }

            int loaded = 0;
            try {
                String content = Files.readString(path);

                // Matches  "name": "text"  and  "name": 123
                Pattern pair = Pattern.compile("\"(\\w+)\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|(-?\\d+))");
                Matcher matcher = pair.matcher(content);

                StoredRecord current = null;
                ArrayList<StoredRecord> records = new ArrayList<>();
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String text = matcher.group(2) != null ? unescapeJson(matcher.group(2)) : null;
                    String number = matcher.group(3);

                    if (key.equals("messageID")) {          // a new message object starts here
                        current = new StoredRecord();
                        records.add(current);
                        current.messageID = text;
                    } else if (current != null) {
                        switch (key) {
                            case "messageNumber":
                                current.messageNumber = Integer.parseInt(number);
                                break;
                            case "messageHash":
                                current.messageHash = text;
                                break;
                            case "sender":
                                current.sender = text;
                                break;
                            case "recipient":
                                current.recipient = text;
                                break;
                            case "message":
                                current.message = text;
                                break;
                            default:
                                break;
                        }
                    }
                }

                for (StoredRecord r : records) {
                    if (r.messageID == null || r.message == null || messageIDs.contains(r.messageID)) {
                        continue; // incomplete record, or already in memory
                    }
                    addByFlag(new Message(r.messageID, r.messageNumber, r.sender,
                            r.recipient, r.message), "stored");
                    loaded++;
                }
            } catch (IOException | RuntimeException e) {
                System.out.println("Could not read JSON file: " + e.getMessage());
            }
            return loaded;
        }

        // Makes text safe to place inside JSON quotes
        private static String escapeJson(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }

        // Reverses escapeJson when reading the file back
        private static String unescapeJson(String text) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\\' && i + 1 < text.length()) {
                    char next = text.charAt(++i);
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        default: sb.append(next);   // \\ and \" and anything else
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        // ==============================================================
        // Stored messages menu features
        // ==============================================================

        // a) Sender and recipient of all stored messages
        public static String displaySenderAndRecipient() {
            if (storedMessages.isEmpty()) {
                return "No stored messages.";
            }
            StringBuilder sb = new StringBuilder();
            for (Message m : storedMessages) {
                sb.append("Sender: ").append(m.sender)
                        .append(" | Recipient: ").append(m.recipient).append("\n");
            }
            return sb.toString().trim();
        }

        // The longest message in a list (null when the list is empty)
        public static Message longestMessage(List<Message> messages) {
            Message longest = null;
            for (Message m : messages) {
                if (longest == null || m.messageText.length() > longest.messageText.length()) {
                    longest = m;
                }
            }
            return longest;
        }

        // b) The text of the longest stored message
        public static String displayLongestMessage() {
            Message longest = longestMessage(storedMessages);
            if (longest == null) {
                return "No stored messages.";
            }
            return longest.messageText;
        }

        // c) Finds a message by its ID in the sent, stored and disregarded arrays (null if none)
        public static Message findByMessageID(String id) {
            for (ArrayList<Message> list : List.of(sentMessages, storedMessages, disregardedMessages)) {
                for (Message m : list) {
                    if (m.messageID.equals(id)) {
                        return m;
                    }
                }
            }
            return null;
        }

        // d) The text of every sent or stored message for one recipient
        public static ArrayList<String> searchByRecipient(String recipient) {
            ArrayList<String> results = new ArrayList<>();
            for (ArrayList<Message> list : List.of(sentMessages, storedMessages)) {
                for (Message m : list) {
                    if (m.recipient.equals(recipient)) {
                        results.add(m.messageText);
                    }
                }
            }
            return results;
        }

        // e) Deletes a stored message using its hash, and updates the JSON file
        public static String deleteByHash(String hash) {
            for (int i = 0; i < storedMessages.size(); i++) {
                Message m = storedMessages.get(i);
                if (m.messageHash.equals(hash)) {
                    storedMessages.remove(i);
                    messageHashes.remove(m.messageHash);
                    messageIDs.remove(m.messageID);
                    saveStoredMessages();
                    return "Message: \"" + m.messageText + "\" successfully deleted.";
                }
            }
            return "No stored message found with hash " + hash + ".";
        }

        // f) Report listing the hash, recipient and message of every message in the list
        public static String displayReport(List<Message> messages) {
            if (messages.isEmpty()) {
                return "No messages to report.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("===== MESSAGE REPORT =====\n");
            for (Message m : messages) {
                sb.append("Message Hash: ").append(m.messageHash).append("\n");
                sb.append("Recipient: ").append(m.recipient).append("\n");
                sb.append("Message: ").append(m.messageText).append("\n");
                sb.append("--------------------------\n");
            }
            return sb.toString().trim();
        }

        // String: all the messages sent while the program is running
        public static String printMessages() {
            if (sentMessages.isEmpty()) {
                return "No messages have been sent yet.";
            }
            return displayReport(sentMessages);
        }

        // Int: total number of messages sent
        public static int returnTotalMessages() {
            return sentMessages.size();
        }

        // ==============================================================
        // Details and getters
        // ==============================================================

        // Full details, in the order the brief asks for
        public String getDetails() {
            return "Message ID: " + messageID + "\n"
                    + "Message Hash: " + messageHash + "\n"
                    + "Recipient: " + recipient + "\n"
                    + "Message: " + messageText + "\n";
        }

        public String getMessageIDMessage() {
            return "Message ID generated: " + messageID;
        }

        public String getMessageID() { return messageID; }
        public String getMessageHash() { return messageHash; }
        public String getSender() { return sender; }
        public String getRecipient() { return recipient; }
        public String getMessageText() { return messageText; }

        // Copies of the arrays, so the unit tests can check what they contain
        public static ArrayList<Message> getSentMessages() { return new ArrayList<>(sentMessages); }
        public static ArrayList<Message> getDisregardedMessages() { return new ArrayList<>(disregardedMessages); }
        public static ArrayList<Message> getStoredMessages() { return new ArrayList<>(storedMessages); }
        public static ArrayList<String> getMessageHashes() { return new ArrayList<>(messageHashes); }
        public static ArrayList<String> getMessageIDs() { return new ArrayList<>(messageIDs); }

        public static ArrayList<String> getSentMessageTexts() { return texts(sentMessages); }
        public static ArrayList<String> getDisregardedMessageTexts() { return texts(disregardedMessages); }
        public static ArrayList<String> getStoredMessageTexts() { return texts(storedMessages); }

        private static ArrayList<String> texts(List<Message> messages) {
            ArrayList<String> result = new ArrayList<>();
            for (Message m : messages) {
                result.add(m.messageText);
            }
            return result;
        }

        public static String getJsonPath() { return jsonPath; }

        // Lets the unit tests write to a temporary file instead of the real one
        public static void setJsonPath(String path) { jsonPath = path; }

        // Used by the unit tests so each test starts fresh
        public static void resetAll() {
            sentMessages.clear();
            disregardedMessages.clear();
            storedMessages.clear();
            messageHashes.clear();
            messageIDs.clear();
        }
    }
}