/*
    Main.java
    Nick Liu + Annie Zhang
    ICS4U
    Main class implementing the game
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.applet.*;

public class Main extends JFrame implements ActionListener {
    private int num;  // File number

    private javax.swing.Timer myTimer;  // Game Timer
    private GamePanel game;  // GamePanel for the actual game
    private Thin_Ice thinIce;  // Thin Ice panel
    private Astro_Barrier astroBarrier;  // Astro Barrier panel

    private JPanel cards;
    private CardLayout cLayout = new CardLayout();  // Allows switching between games

    private String panel;  // Indicates what game the screen is on

    private int gameScore = 0;  // Score achieved from minigame for bell earning purposes

    public Main(int num) {
        // Creating frame
        super("Pokemon Crossing");
        setSize(1020,695);

        this.num = num;

        // Cards
        cards = new JPanel(cLayout);

        // Creating panels
        game = new GamePanel(this);
        cards.add(game, "game");

        Thin_Ice.load();
        thinIce = new Thin_Ice(this);
        cards.add(thinIce, "thin ice");

        astroBarrier = new Astro_Barrier(this);
        cards.add(astroBarrier, "astro barrier");

        add(cards);

        myTimer = new javax.swing.Timer(10, new TickListener());


        cLayout.show(cards, "game");
        panel = "game";

        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public void start() {
        myTimer.start();
    }


    // Class that deals with actions within the game
    class TickListener implements ActionListener {
        public void actionPerformed(ActionEvent evt){
            /*
                Method is called every tick while the game is running. Calls the move and paint methods
                which deal with game logic.
            */
            if (game != null && panel.equals("game")) {
                game.grabFocus();
                game.move();
                game.repaint();

            }
            else if (thinIce != null && panel.equals("thin ice")) {
                thinIce.grabFocus();
                thinIce.move();
                thinIce.repaint();
            }
            else if (astroBarrier != null && panel.equals("astro barrier")) {
                astroBarrier.grabFocus();
                astroBarrier.move();
                astroBarrier.repaint();
                astroBarrier.checkComplete();
                astroBarrier.checkCollisions();
            }
            else if (panel.equals("")) {  // End the game if the panel is equal to empty string
                endGame();
            }
        }

    }

    public void endGame() {
        setVisible(false);
    }

    // Changes the current game
    public void changeGame(String game) {
        panel = game;
        cLayout.show(cards, game);

        // Initiate the selected game
        if (game.equals("thin ice")) {
            thinIce.init();
        }
        else if (game.equals("astro barrier")) {
            astroBarrier.init();
        }
    }

    // Getters/setters
    public int getGameScore() {
        return gameScore;
    }

    public void setGameScore(int n) {
        gameScore = n;
    }

    public int getNum() {
        return num;
    }

    public void setPanel(String s) {
        panel = s;
    }

}

class GamePanel extends JPanel implements KeyListener, MouseListener {
    private boolean[] keys;   // Array of keys that keeps track if they are down or not
    private Main mainFrame;   // Frame of the program
    private int count = 0;

    public static final int tileSize = 60;  // Dimension of the tile
    private Player player;
    private int mostRecentKeyPress = 0;  // Most recent movement key press (WASD)
    private Room curRoom;  // Room player is in
    private Room outside;  // The outside map room
    private int[][] grid;  // Current grid of the room the player is in
    private Hashtable<Point, Room> rooms = new Hashtable<>();  // Hashtable of all rooms with the entry point as the key

    private Room minigameIsland;

    private static ArrayList<Item> items = new ArrayList<>();  // ArrayList of all items

    private Point mouse = new Point(0, 0);
    private boolean clicked = false;

    // Variables to help with the fade effect when entering rooms
    private boolean fadingToBlack = false;
    private int fadeTimeStart = 0;

    // NPCs
    private Tom_Nook tom_nook;
    private Boat_Operator boat_operator;
    private Boat_Operator boat_operator_on_island;
    private Celeste celeste;
    private Isabelle isabelle;

    private ArrayList<NPC> NPCs = new ArrayList<>();

    // Images
    private final Image speechBubbleImage = new ImageIcon("Assets/Misc/speech bubble copy.png").getImage();
    private final Image selectionMenuImage = new ImageIcon("Assets/Misc/With click.png").getImage();
    private final Image selectionMenuNoClickImage = new ImageIcon("Assets/Misc/No Click.png").getImage();
    private final Image shopImage = new ImageIcon("Assets/Misc/shop menu.png").getImage();
    private final Image rightClickImage = new ImageIcon("Assets/Misc/right click.png").getImage();
    private final Image museumMenuImage = new ImageIcon("Assets/Misc/Museum Menu.png").getImage();
    private final Image exitButtonImage = new ImageIcon("Assets/Misc/exit button.png").getImage();
    private final Image museumBugsImage = new ImageIcon("Assets/Misc/bugs.png").getImage();
    private final Image museumFishImage = new ImageIcon("Assets/Misc/fish.png").getImage();
    private final Image museumFossilImage = new ImageIcon("Assets/Misc/fossils.png").getImage();
    private final Image holeImage = new ImageIcon("Assets/Misc/hole.png").getImage();
    private final Image buriedObjectImage = new ImageIcon("Assets/Misc/buried object.png").getImage();

    // Arrays of info
    private int[][] diggableTiles = new int[94][85];
    private int[][] minigameIslandDiggable = new int[49][46];

    private int[][] waterTiles = new int[94][85];
    private int[][] minigameIslandWater = new int[49][46];

    private int[][] beachTiles = new int[94][85];
    private int[][] minigameIslandBeach = new int[49][46];

    private ArrayList<Tree> trees = new ArrayList<>();


    private double selectionAngle = 0;  // Angle of dialogue selection
    private int gameScore = 0;  // Score from minigame
    private long lastOn; // Unix timestamp of last time player opened file

    private int dialogueDelay = 0;  // Counter for talking after player is leaving
    private boolean clickedRightClickMenu = false;

    // Fonts
    public static Font finkheavy15 = null;
    public static Font finkheavy30 = null;
    public static Font finkheavy32 = null;
    public static Font finkheavy36 = null;

    // Arcade tiles to play games
    private int[][] thinIceArcadeTiles = {{13, 14}, {13, 11}, {21, 11}, {21, 14}};
    private int[][] astroBarrierArcadeTiles = {{12, 14}, {12, 11}, {20, 11}, {20, 14}};

    private boolean exitMenuOpen = false;

    // Sounds
	private File diggingWav = new File("Assets/Sounds/Digging.wav");
	private AudioClip diggingSFX;
	private File fishingWav = new File("Assets/Sounds/Fishing.wav");
	private AudioClip fishingSFX;
	private File animaleseWav = new File("Assets/Sounds/Animalese.wav");
	private AudioClip animaleseSFX;
	private MP3 music = new MP3("Assets/Sounds/Background Music.mp3");
	
    public GamePanel(Main m) {
        keys = new boolean[KeyEvent.KEY_LAST + 1];  // Key presses

        // Setting panel
        mainFrame = m;
        setSize(1020, 695);

        // Adding action listeners
        addKeyListener(this);
        addMouseListener(this);
        GamePanel.loadFonts();
        init();
    }

    // Requests focus of game panel
    public void addNotify() {
        super.addNotify();
        requestFocus();
    }

    // Initiates the game
    public void init() {
        // Load stuff
        loadMap();
        loadSounds();
        loadItems();

        // Set initial room to be the outside
        curRoom = outside;
        grid = curRoom.getGrid();

        // Load more stuff
        Item.loadFoundImages();
        Player.load();
        NPC.loadDialogue();

        // Add nulls to NPC arraylist
        for (int i = 0; i < 8; i++) {
            NPCs.add(null);
        }

        // Create NPCs
        tom_nook = new Tom_Nook("Tom Nook", null, 11, 8, "mate", rooms.get(new Point(39, 55)),0);
        tom_nook.generateStoreItems();
        NPCs.set(0, tom_nook);

        boat_operator = new Boat_Operator("Boat Operator", null,30, 75, "dude", outside, 6);
        NPCs.set(6, boat_operator);

        boat_operator_on_island = new Boat_Operator("Boat Operator", null,22, 36, "dude", minigameIsland, 7);
        NPCs.set(7, boat_operator_on_island);

        celeste = new Celeste("Celeste", null, 16, 11, "my guy", rooms.get(new Point(64, 48)),2);
        NPCs.set(2, celeste);

        isabelle = new Isabelle("Isabelle", null, 15, 8, "my guy", rooms.get(new Point(63, 35)), 1);
        NPCs.set(1, isabelle);

        // Load more stuff
        Furniture.loadImages();
        Furniture.loadFurnitureSizes();
        loadMainStuff();
        createNPCs();
        Tree.loadFruits();

        // Restock the shop and have fruit grow back if the time they opened is a different day then the last time closed
        if (Instant.now().getEpochSecond() / 86400 > lastOn / 86400) {
            tom_nook.generateStoreItems();
            for (Tree tree : trees) {
                if (tree.getSize() == 3) {
                    tree.setNumFruit(3);
                }
            }
        }

        // Generate shells and fossils
        generateShells();
        generateFossils();
        
        music.play();
        
    }

    // Randomly generate shells on the beaches
    public void generateShells() {
        for (int i = 0; i < 200; i++) {  // Main island
            // X and Y tile position
            int a = randint(0, 93);
            int b = randint(0, 84);

            // Add a shell to the position if the position is a beach tile
            if (beachTiles[a][b] == 1 && outside.getGrid()[a][b] == 1) {
                outside.getGrid()[a][b] = 4;
                outside.getDroppedItems().put(new Point(a, b), new DroppedItem(items.get(randint(106, 111)), a, b));
            }
        }

        for (int i = 0; i < 100; i++) { // Minigame Island
            // X and Y tile position
            int a = randint(0, 48);
            int b = randint(0, 45);

            // Add a shell to the position if the position is a beach tile
            if (minigameIslandBeach[a][b] == 1 && minigameIsland.getGrid()[a][b] == 1) {
                minigameIsland.getGrid()[a][b] = 4;
                minigameIsland.getDroppedItems().put(new Point(a, b), new DroppedItem(items.get(randint(106, 111)), a, b));
            }
        }
    }

    // Generate fossils on diggable tiles
    public void generateFossils() {
        for (int i = 0; i < 12; i++) { // Main island
            // X and Y tile position
            int a = randint(0, 93);
            int b = randint(0, 84);

            // Add a fossil to the position if the position is a diggable tile
            if (diggableTiles[a][b] == 1 && outside.getGrid()[a][b] == 1) {
                outside.getGrid()[a][b] = 6;
            }
        }

        for (int i = 0; i < 6; i++) {
            // X and Y tile position
            int a = randint(0, 48);
            int b = randint(0, 45);

            // Add a fossil to the position if the position is a diggable tile
            if (minigameIslandDiggable[a][b] == 1 && minigameIsland.getGrid()[a][b] == 1) {
                minigameIsland.getGrid()[a][b] = 6;
            }
        }
    }

    // Load player info and other stuff
    public void loadMainStuff() {
        try {
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/save" + mainFrame.getNum() + ".txt")));
            String name = stdin.nextLine();  // Player name
            int gender = Integer.parseInt(stdin.nextLine());  // Player gender

            // Create player
            player = new Player(name,1800, 2100, gender, grid, this);
            player.setBells(Integer.parseInt(stdin.nextLine()));

            // Add items to inventory
            for (int i = 0; i < 18; i++) {
                // Converting from 1d to 2d array
                int b = i / 6; // row
                int a = i % 6; // column

                String line = stdin.nextLine();

                // Adding the item based on id
                if (line.equals("null")) {
                    player.getItems()[a][b] = null;
                }
                else {
                    player.getItems()[a][b] = items.get(Integer.parseInt(line));
                }
            }

            // Get equipped item
            String equipped = stdin.nextLine();
            if (!equipped.equals("null")) {
                player.equipItem(items.get(Integer.parseInt(equipped)));
            }

            player.setSelectedWallpaper(stdin.nextLine()); // Room wallpaper
            player.setSelectedFloor(stdin.nextLine()); // Room floor

            // Bugs in museum
            String[] lineItems = stdin.nextLine().split(" ");
            for (String s : lineItems) {
                if (!s.equals("")) {
                    celeste.addBug(items.get(Integer.parseInt(s)));
                }
            }

            // Fish in museum
            lineItems = stdin.nextLine().split(" ");
            for (String s : lineItems) {
                if (!s.equals("")) {
                    celeste.addFish(items.get(Integer.parseInt(s)));
                }
            }

            // Fossils in museum
            lineItems = stdin.nextLine().split(",");
            for (String s : lineItems) {
                if (!s.equals("")) {
                    celeste.addFossil(new Item(2, s, new ImageIcon("Assets/Items/General/Fossil.png").getImage(), 0, 100));
                }
            }

            lastOn = Long.parseLong(stdin.nextLine());  // Timestamp of last time player was on

            // Furniture placed
            while (stdin.hasNextLine()) {
                lineItems = stdin.nextLine().split(" ");
                System.out.println(Arrays.toString(lineItems));
                player.getFurniture().add(new Furniture(Integer.parseInt(lineItems[0]), Integer.parseInt(lineItems[1]), Integer.parseInt(lineItems[2]), Integer.parseInt(lineItems[3]), Integer.parseInt(lineItems[4])));
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Load sound effects
 	public void loadSounds(){
 		try{
			diggingSFX = Applet.newAudioClip(diggingWav.toURL());
			fishingSFX = Applet.newAudioClip(fishingWav.toURL());
			animaleseSFX = Applet.newAudioClip(animaleseWav.toURL());
		}
		catch(Exception e){
            e.printStackTrace();
		}
 	}

 	// Load fonts
    public static void loadFonts() {
        try {
            finkheavy15 = Font.createFont(Font.TRUETYPE_FONT, new File("Assets/Misc/FinkHeavy.ttf")).deriveFont(15f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(finkheavy15);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        try {
            finkheavy30 = Font.createFont(Font.TRUETYPE_FONT, new File("Assets/Misc/FinkHeavy.ttf")).deriveFont(30f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(finkheavy30);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        try {
            finkheavy32 = Font.createFont(Font.TRUETYPE_FONT, new File("Assets/Misc/FinkHeavy.ttf")).deriveFont(32f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(finkheavy32);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        try {
            //create the font to use. Specify the size!
            finkheavy36 = Font.createFont(Font.TRUETYPE_FONT, new File("Assets/Misc/FinkHeavy.ttf")).deriveFont(36f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(finkheavy36);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    // Read from the map and room files and loads them
    public void loadMap() {
        int[][] mapGrid = new int[94][85];

        // Reading from the map grid
        try{
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/map.txt")));
            for (int i = 0; i < 85; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 94; j++) {
                    mapGrid[j][i] = Integer.parseInt(s[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Creating the outside room
        outside = new Room(mapGrid, new ImageIcon("Assets/Map/PC Map.png").getImage(), 0, 0, 0 ,0, 0, 0, "PC Map");

        int[][] minigameIslandMapGrid = new int[49][46];

        // Reading from the map grid
        try{
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/minigame island map.txt")));
            for (int i = 0; i < 46; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 49; j++) {
                    minigameIslandMapGrid[j][i] = Integer.parseInt(s[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        minigameIsland = new Room(minigameIslandMapGrid, new ImageIcon("Assets/Map/Minigame Island.png").getImage(), 31, 75, 23, 36,0, 0, "Minigame Island");
        rooms.put(new Point(31, 75), minigameIsland);

        try {  // Loading diggable tiles on main island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/outside diggable tiles.txt")));
            for (int i = 0; i < 85; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 94; j++) {
                    diggableTiles[j][i] = Integer.parseInt(s[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {  // Loading water tiles on main island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/Map/Main Island Water.txt")));
            for (int i = 0; i < 85; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 94; j++) {
                    waterTiles[j][i] = Integer.parseInt(s[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        try {  // Loading diggable tiles on minigame island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/minigame island diggable tiles.txt")));
            for (int i = 0; i < 46; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 49; j++) {
                    minigameIslandDiggable[j][i] = Integer.parseInt(s[j]);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {  // Loading water tiles on minigame island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/Map/minigame island water.txt")));
            for (int i = 0; i < 46; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 49; j++) {
                    minigameIslandWater[j][i] = Integer.parseInt(s[j]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {  // Loading beach tiles on main island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/Map/outside beach.txt")));
            for (int i = 0; i < 85; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 94; j++) {
                    beachTiles[j][i] = Integer.parseInt(s[j]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {  // Loading beach tiles on minigame island
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/Map/minigame island beach.txt")));
            for (int i = 0; i < 46; i++) {
                String[] s = stdin.nextLine().split(" ");
                for (int j = 0; j < 49; j++) {
                    minigameIslandBeach[j][i] = Integer.parseInt(s[j]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Reading from the room file
        try{
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/Rooms.txt")));
            int n = Integer.parseInt(stdin.nextLine());  // Number of rooms
            for (int i = 0; i < n; i++) {
                String file = stdin.nextLine();

                // Reading room info
                int entryX, entryY, exitX, exitY, exitX2, exitY2;
                String[] line = stdin.nextLine().split(" ");

                entryX = Integer.parseInt(line[0]);
                entryY = Integer.parseInt(line[1]);
                exitX = Integer.parseInt(line[2]);
                exitY = Integer.parseInt(line[3]);
                exitX2 = Integer.parseInt(line[4]);
                exitY2 = Integer.parseInt(line[5]);

                int len, wid;
                line = stdin.nextLine().split(" ");
                wid = Integer.parseInt(line[0]);
                len = Integer.parseInt(line[1]);

                // Reading grid
                int[][] grid = new int[wid][len];
                for (int j = 0; j < len; j++) {
                    line = stdin.nextLine().split(" ");
                    for (int k = 0; k < wid; k++) {
                        grid[k][j] = Integer.parseInt(line[k]);
                    }
                }

                // Adding to rooms
                rooms.put(new Point(entryX, entryY), new Room(grid, new ImageIcon("Assets/Rooms/"+file).getImage(), entryX, entryY, exitX, exitY, exitX2, exitY2, file.substring(0, file.length()-4)));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {  // Loading trees
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Saves/save" + mainFrame.getNum() + "/trees.txt")));
            int n = Integer.parseInt(stdin.nextLine());  // Main island trees
            for (int i = 0; i < n; i++) {
                String[] line = stdin.nextLine().split(" ");
                trees.add(new Tree(Integer.parseInt(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2]), Integer.parseInt(line[3]), outside, Integer.parseInt(line[4])));
            }
            n = Integer.parseInt(stdin.nextLine());  // Minigame island trees
            for (int i = 0; i < n; i++) {
                String[] line = stdin.nextLine().split(" ");
                trees.add(new Tree(Integer.parseInt(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2]), Integer.parseInt(line[3]), minigameIsland, Integer.parseInt(line[4])));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Reading from the items file and loading it to the ArrayList
    // Iterates through all files in the items folder and all subfolders
    public void loadItems() {
        items.clear(); // Clear the arraylist
        Hashtable<String, int[]> itemInfo = new Hashtable<>();  // Temporary info

        try {  // Reading from file
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/Items/Items.txt")));
            int n = Integer.parseInt(stdin.nextLine());
            String[] line;
            String fileName;
            for (int i = 0 ; i < n; i++) {
                line = stdin.nextLine().split(" ");
                fileName = "";
                for (int j = 1; j < line.length-2; j++) {
                    fileName += line[j] + " ";
                }
                fileName = fileName.substring(0, fileName.length()-1);
                itemInfo.put(fileName, new int[] {Integer.parseInt(line[0]), Integer.parseInt(line[line.length-2]), Integer.parseInt(line[line.length-1])});
            }

            for (int i = 0; i < n; i++) {  // Add nulls in the arraylist for later
                items.add(null);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Going through all files in the Items folder and all subfolders within the folder
        File folder = new File("Assets/Items");
        ArrayList<String> absolutePaths = new ArrayList<>();
        search(".*\\.png", folder, absolutePaths);  // Recursively get the files and add to the arraylist

        String name;
        String[] splitFile;
        int[] info;

        // Go through every file
        for (String file : absolutePaths) {
            splitFile = file.split("\\\\");
            name = splitFile[splitFile.length-1];
            info = itemInfo.get(name);
            name = name.substring(0, name.length()-4);

            // Add the file at the appropriate position in the arraylist based on id
            items.set(info[0], new Item(info[0], capitalizeWord(name), new ImageIcon(file).getImage(), info[1], info[2]));
        }

        Item.loadFossils();  // Loading fossil names
    }

    // Given a folder and a pattern for the files, adds the names of all the files in the folder
    // Copied from stack overflow
    public void search(String pattern, File folder, ArrayList<String> result) {
        for (File f : Objects.requireNonNull(folder.listFiles())) {
            if (f.isDirectory()) {
                search(pattern, f, result);
            }

            if (f.isFile()) {
                if (f.getName().matches(pattern)) {
                    result.add(f.getAbsolutePath());
                }
            }
        }
    }

    // Creates the NPCs by loading images and other information
    // Iterates through ever file in the villager's folder and adds them to a hashtable and creates the NPC objects
    public void createNPCs() {
        // Annie npc images
        File folder = new File("Assets/NPCs/Annie");
        File[] listOfFiles = folder.listFiles();
        Hashtable<String, Image> annieImages = new Hashtable<>();


        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                annieImages.put(listOfFiles[i].getName().substring(0, listOfFiles[i].getName().length() - 4),
                    new ImageIcon("Assets/NPCs/Annie/"+listOfFiles[i].getName()).getImage());
            }
        }
        NPCs.set(3, new NPC("Annie", annieImages, 45, 50, "my guy", outside, Player.ANNIE));

        // Bob the builder npcs
        folder = new File("Assets/NPCs/Bob the Builder");
        listOfFiles = folder.listFiles();
        Hashtable<String, Image> bobImages = new Hashtable<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                bobImages.put(listOfFiles[i].getName().substring(0, listOfFiles[i].getName().length() - 4),
                    new ImageIcon("Assets/NPCs/Bob the Builder/"+listOfFiles[i].getName()).getImage());
            }
        }

        NPCs.set(4, new NPC("Bob the Builder", bobImages, 45, 51, "pthhpth", outside, Player.BOB_THE_BUILDER));

        // Nick npc images
        folder = new File("Assets/NPCs/Nick");
        listOfFiles = folder.listFiles();
        Hashtable<String, Image> nickImages = new Hashtable<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                nickImages.put(listOfFiles[i].getName().substring(0, listOfFiles[i].getName().length() - 4),
                    new ImageIcon("Assets/NPCs/Nick/"+listOfFiles[i].getName()).getImage());
            }
        }

        NPCs.set(5, new NPC("Nick", nickImages, 45, 52, "kid", outside, Player.NICK));
    }

    // Deals with all player movement
    public void move() {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();  // Get mouse position
        Point offset = getLocationOnScreen();  // Get window position
        mouse = new Point (mousePos.x-offset.x, mousePos.y-offset.y);

        // Increment the counter
        count++;

        // Increment the dialogue delay if player is talking to npc and is about to leave
        if (player.isTalkingToNPC() && !player.isDialogueSelectionOpen()) {
            dialogueDelay++;
        }


        // Add bells after player finishes game
        if (mainFrame.getGameScore() > 0) {
            player.setBells(player.getBells() + mainFrame.getGameScore() / 10);
            gameScore = mainFrame.getGameScore();
            mainFrame.setGameScore(0);
            player.setEarnedBellsPromptOpen(true);
        }


        // Move player in different directions if WASD is pressed and the inventory is not open
        if (!player.isInventoryOpen() && !fadingToBlack && !player.isActionProgressOpen() && !player.isItemFoundPrompt() && !exitMenuOpen) {
            if (keys[KeyEvent.VK_D] && KeyEvent.VK_D == mostRecentKeyPress) {  // right
                player.move(Player.RIGHT, keys, grid);
            }

            if (keys[KeyEvent.VK_W] && KeyEvent.VK_W == mostRecentKeyPress) {  // up
                player.move(Player.UP, keys, grid);
            }

            if (keys[KeyEvent.VK_A] && KeyEvent.VK_A == mostRecentKeyPress) {  // left
                player.move(Player.LEFT, keys, grid);
            }

            if (keys[KeyEvent.VK_S] && KeyEvent.VK_S == mostRecentKeyPress) {  // down
                player.move(Player.DOWN, keys, grid);
            }

            player.move();

            // Deal with going to new room
            if (player.isGoingToNewRoom() && !fadingToBlack) {
                fadingToBlack = true;
                fadeTimeStart = count;
            }

            // Deal with exiting room
            else if (player.isExitingRoom() && !fadingToBlack) {
                fadingToBlack = true;
                fadeTimeStart = count;
            }
        }

        // Move villagers
        for (NPC temp : NPCs) {
            if (curRoom == outside) {
                temp.move(grid, player.getX(), player.getY(), player.getGoingToxTile(), player.getGoingToyTile(), NPCs);
            }
        }

        // If player clicks on dialogue selection circle set flag true
        if (player.isDialogueSelectionOpen() && Math.hypot(510 - mouse.x, 186 - mouse.y) <= 34 && clicked) {
            player.setSelectionMenuClicked(true);
        }
        else if (!clicked) {
            player.setSelectionMenuClicked(false);
        }

        // Get selection angle of dialogue selection
        if (player.isDialogueSelectionOpen()) {
            selectionAngle = ((Math.atan2((186 - mouse.y), (mouse.x - 510)) + 2*Math.PI) % (2*Math.PI));
        }

        // If dialogue delay has finished then player is no longer talking to npcs
        if (player.isTalkingToNPC() && dialogueDelay >= 300) {
            if (!(player.getVillagerPlayerIsTalkingTo() == Player.TOM_NOOK && tom_nook.getSpeechStage() != Tom_Nook.GOODBYE) &&
            !(player.getVillagerPlayerIsTalkingTo() == Player.CELESTE && celeste.getSpeechStage() != Celeste.GOODBYE)) {
                player.setTalkingToNPC(false);
                dialogueDelay = 0;
                NPCs.get(player.getVillagerPlayerIsTalkingTo()).setTalking(false);
            }
        }

        // Action (e.g. bug catching) has completed after 160 frames
        if (player.isActionProgressOpen() && player.getActionProgress() >= 160) {
            player.setActionProgress(0);
            player.setActionProgressOpen(false);

            // Add bug to inventory with 30% chance if player was bug catching
            if (player.getAction() == Player.BUG_CATCHING) {
                if (randint(1, 10) <= 3) {
                    Item item = items.get(randint(7, 31));
                    player.addItem(item);
                    player.setItemFoundPrompt(true);
                    player.setCaughtItem(item);
                }
            }

            // Add fossil to inventory if player was digging fossils
            if (player.getAction() == Player.DIGGING_FOSSIL) {
                Item item = items.get(3);
                item.setName(capitalizeWord(Item.fossilNames.get(randint(0, Item.fossilNames.size() - 1))));
                player.setItemFoundPrompt(true);
                player.setCaughtItem(item);
                player.addItem(item);
            }

            // Add fish to inventory if player was fishing with 30% chance
            if (player.getAction() == Player.FISHING) {
                if (randint(1, 10) <= 3) {
                    Item item = items.get(randint(38, 105));
                    player.setItemFoundPrompt(true);
                    player.setCaughtItem(item);
                    player.addItem(item);
                }
                player.setFishing(false);
            }
        }

        // If player leaves their room and are trying to place furniture, it stops them
        if (curRoom != rooms.get(new Point(30, 35))) {
            player.setPlacingFurniture(false);
        }


    }

    // Takes player to a new room
    public void goToNewRoom() {
        curRoom.setGrid(grid);
        curRoom = rooms.get(new Point(player.getxTile(), player.getyTile()));  // Set curRoom to be new the new room
        grid = curRoom.getGrid();  // Set grid

        // Set player pos
        player.setxTile(curRoom.getExitX());
        player.setyTile(curRoom.getExitY());
        player.setX(curRoom.getExitX() * tileSize);
        player.setY(curRoom.getExitY() * tileSize);

        player.setGoingToNewRoom(false);
    }

    // Allows player to exit room
    public void exitRoom() {
        curRoom.setGrid(grid);
        // Set player pos
        player.setxTile(curRoom.getEntryX());
        player.setyTile(curRoom.getEntryY());
        player.setX(curRoom.getEntryX() * tileSize);
        player.setY(curRoom.getEntryY() * tileSize);

        // Set curRoom and grid to be the outside
        if (curRoom.getEntryX() == 24) {
            curRoom = minigameIsland;
        }
        else {
            curRoom = outside;
        }

        grid = curRoom.getGrid();

        player.setExitingRoom(false);
    }

    // Returns if a tile is adjacent to a player or not
    public boolean isAdjacentToPlayer(int xTile, int yTile) {
        return (xTile == player.getxTile() && yTile == player.getyTile() - 1) || (xTile == player.getxTile() && yTile == player.getyTile() + 1) ||
            (xTile == player.getxTile() - 1 && yTile == player.getyTile()) || (xTile == player.getxTile() + 1 && yTile == player.getyTile());
    }

    // Returns the NPC at a given location, or null if there isn't any
    public NPC npcAtPoint(int xTile, int yTile) {
        for (NPC temp : NPCs) {
            if ((curRoom == temp.getRoom()) && ((xTile == temp.getxTile() && yTile == temp.getyTile()) || (xTile == temp.getGoingToxTile() && yTile == temp.getGoingToyTile()))) {
                return temp;
            }
        }
        return null;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!keys[e.getKeyCode()] && (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_W ||
            e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_S)) {
            mostRecentKeyPress = e.getKeyCode();  // If WASD is pressed set the most recent keypress
        }

        keys[e.getKeyCode()] = true;  // Set key in key array to be down

        // Inventory opening
        if (keys[KeyEvent.VK_Q]) {
            player.setPlacingFurniture(false);

            if (!player.isShopOpen() && !player.isTalkingToNPC() && !player.isActionProgressOpen() && !player.isItemFoundPrompt() && !exitMenuOpen) {
                if (!player.isInventoryOpen()) {
                    player.setEscapeQueued(true);  // Queue the inventory opening
                }
                else {
                    player.setInventoryOpen(false);
                }

                // Deselect any items
                player.setSelectedItemR(-1);
                player.setSelectedItemC(-1);
            }
        }

        // Exit menu
        if (keys[KeyEvent.VK_ESCAPE]) {
            exitMenuOpen = !exitMenuOpen;
        }

        player.setRightClickMenuOpen(false);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }


    @Override
    public void mouseClicked(MouseEvent e) {
		
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            clicked = true;
            clickedRightClickMenu = false;


            if (player.isInventoryOpen()){
                // Select item if right click menu is not open
                if (!player.isRightClickMenuOpen()) {
                    player.selectItem(mouse);
                }

                else {
                    if (player.clickedMenuBox(mouse.x, mouse.y) == 0) {  // First box in right click menu
                        clickedRightClickMenu = true;
                        if (grid[player.getxTile()][player.getyTile()] != 4) {  // Drop the item if there is no item on the tile already
                            curRoom.addDroppedItem(new DroppedItem(player.getSelectedItem(), player.getxTile(), player.getyTile()));
                            grid[player.getxTile()][player.getyTile()] = 4;
                            player.dropSelectedItem();

                            // Deselect item
                            player.setSelectedItemC(-1);
                            player.setSelectedItemR(-1);
                        }
                    }
                    if (player.clickedMenuBox(mouse.x, mouse.y) == 1) {  // Second box in right click menu
                        if (player.getSelectedItemR() != -1 && player.getSelectedItemC() != -1) {
                            clickedRightClickMenu = true;
                            Item item = player.getItems()[player.getSelectedItemR()][player.getSelectedItemC()];

                            if (item.isFloor()) {  // Apply floor if item is floor
                                String temp = player.getSelectedFloor();
                                player.setSelectedFloor(item.getName());
                                player.removeItem(item);
                                player.addItem(new Item(144, temp, new ImageIcon("Assets/Items/General/flooring.png").getImage(), 1500, 350));
                                player.setSelectedItemC(-1);
                                player.setSelectedItemR(-1);

                            }
                            else if (item.isWallpaper()) {  // Apply wallpaper if item is wallpaper
                                String temp = player.getSelectedWallpaper();
                                player.setSelectedWallpaper(item.getName());
                                player.removeItem(item);
                                player.addItem(new Item(125, temp, new ImageIcon("Assets/Items/General/wallpaper.png").getImage(), 1000, 200));
                                player.setSelectedItemC(-1);
                                player.setSelectedItemR(-1);
                            }
                            else if (item.isFurniture()) {  // Place furniture if item is furniture
                                player.setPlacingFurniture(true);
                                player.setInventoryOpen(false);
                            }

                            else if (!player.isSelectedEquipped()) {  // Equip item if item can be equipped
                                player.equipItem();
                                player.setSelectedEquipped(true);
                                player.setSelectedItemC(-1);
                                player.setSelectedItemR(-1);
                            }

                        }

                        else { // Item is equipped item
                            // Unequip item
                            player.unequipItem();
                            clickedRightClickMenu = true;
                            player.setSelectedEquipped(false);
                        }
                    }
                }
            }
            else if (player.isShopOpen()) {
                for (int i = 0; i < 5; i++) {  // Check if any of the shop rects have been selected
                    if (tom_nook.getItemRects().get(i).contains(mouse)) {
                        player.setSelectedItemInShop(i);
                    }
                }

                if (tom_nook.getBuyRect().contains(mouse)) {  // Buy button pressed
                    if (player.inventoryHasSpace()) {
                        if (player.getSelectedItemInShop() <= tom_nook.getStoreItems().size() - 1) {
                            Item item = tom_nook.getStoreItems().get(player.getSelectedItemInShop());

                            if (player.getBells() >= item.getBuyCost()) {  // If player has enough bells buy the item
                                player.addItem(item);
                                player.setBells(player.getBells() - item.getBuyCost());
                                tom_nook.getStoreItems().remove(item);

                                player.setShopOpen(false);
                                player.setDialogueSelectionOpen(true);
                                tom_nook.setSpeechStage(NPC.GREETING);
                            }
                        }
                    }
                    else {
                        player.setInventoryFullPromptOpen(true);
                    }
                }

                else if (tom_nook.getCancelRect().contains(mouse)) {  // Cancel button pressed
                    player.setShopOpen(false);
                    player.setDialogueSelectionOpen(true);
                    tom_nook.setSpeechStage(NPC.GREETING);
                }
            }

            else if (player.isSellShopOpen()) {  // Selling items
                player.selectSellItem(mouse);
                if (player.getSellRect().contains(mouse)) {  // Sell button pressed - sell selected items
                    player.sellItems();
                    player.setSellShopOpen(false);
                    player.setDialogueSelectionOpen(true);
                    tom_nook.setSpeechStage(NPC.GREETING);
                }
                else if (player.getCancelRect().contains(mouse)) {  // Cancel button pressed - return to talking to tom nook
                    player.setSellShopOpen(false);
                    player.setDialogueSelectionOpen(true);
                    tom_nook.setSpeechStage(NPC.GREETING);
                }
            }

            else if (player.isMuseumOpen()) {  // Museum open
                // Scroll up and down buttons
                Rectangle upRect = new Rectangle(483, 223, 62, 20);
                Rectangle downRect = new Rectangle(483, 512, 62, 20);

                if (Math.hypot(950 - mouse.x, 140 - mouse.y) < 20) {  // X button in top right
                    player.setMuseumOpen(false);
                    player.setDialogueSelectionOpen(true);
                    celeste.setSpeechStage(NPC.GREETING);
                }
                else if (celeste.getBugRect().contains(mouse)) {  // Bug section
                    celeste.setPage(Celeste.BUG_PAGE);
                }
                else if (celeste.getFishRect().contains(mouse)) {  // Fish section
                    celeste.setPage(Celeste.FISH_PAGE);
                }
                else if (celeste.getFossilRect().contains(mouse)) {  // Fossil section
                    celeste.setPage(Celeste.FOSSIL_PAGE);
                }
                // Museum draws from starting index to starting index + 5
                else if (upRect.contains(mouse)) {  // scroll up by increasing the starting index by 5
                    if (celeste.getPage() == Celeste.BUG_PAGE) {
                        if (celeste.getBugStart() - 5 >= 0) {
                            celeste.setBugStart(celeste.getBugStart() - 5);
                        }
                    }
                    else if (celeste.getPage() == Celeste.FISH_PAGE) {
                        if (celeste.getFishStart() - 5 >= 0) {
                            celeste.setFishStart(celeste.getFishStart() - 5);
                        }
                    }
                    else {
                        if (celeste.getFossilStart() - 5 >= 0) {
                            celeste.setFossilStart(celeste.getFossilStart() - 5);
                        }
                    }
                }
                else if (downRect.contains(mouse)) {  // scroll down by increasing the starting index by 5
                    if (celeste.getPage() == Celeste.BUG_PAGE) {
                        if (celeste.getBugStart() + 5 <= celeste.getBugs().size() - 1) {
                            celeste.setBugStart(celeste.getBugStart() + 5);
                        }
                    }
                    else if (celeste.getPage() == Celeste.FISH_PAGE) {
                        if (celeste.getFishStart() + 5 <= celeste.getFish().size() - 1) {
                            celeste.setFishStart(celeste.getFishStart() + 5);
                        }
                    }
                    else {
                        if (celeste.getFossilStart() + 5 <= celeste.getBugs().size() - 1) {
                            celeste.setFossilStart(celeste.getFossilStart() + 5);
                        }
                    }
                }
            }
            else if (player.isDonateMenuOpen()) {  // Donation menu
                player.selectDonateItem(mouse);
                if (player.getSellRect().contains(mouse)) {  // Donate button pressed
                    donateItems();
                    player.setDonateMenuOpen(false);
                    player.setDialogueSelectionOpen(true);
                    celeste.setSpeechStage(NPC.GREETING);
                }
                else if (player.getCancelRect().contains(mouse)) {  // Cancel button pressed
                    player.setDonateMenuOpen(false);
                    player.setDialogueSelectionOpen(true);
                    celeste.setSpeechStage(NPC.GREETING);
                }
            }

            // Item found (e.g. fossil dug up)
            else if (player.isItemFoundPrompt()) {
                Rectangle okRect = new Rectangle(440, 500, 140, 40);
                if (okRect.contains(mouse)) {  // Press ok to exit prompt
                    player.setItemFoundPrompt(false);
                }
            }

            // Inventory is full when player tries to acquire another item
            else if (player.isInventoryFullPromptOpen()) {
                Rectangle okRect = new Rectangle(440, 500, 140, 40);
                if (okRect.contains(mouse)) {
                    player.setInventoryFullPromptOpen(false);
                }
            }

            // Player earned bells from minigame
            else if (player.isEarnedBellsPromptOpen()) {
                Rectangle okRect = new Rectangle(440, 500, 140, 40);
                if (okRect.contains(mouse)) {
                    player.setEarnedBellsPromptOpen(false);
                    gameScore = 0;
                }
            }

            // Player is placing furniture
            else if (player.isPlacingFurniture()) {
                Item item = player.getItems()[player.getSelectedItemR()][player.getSelectedItemC()];  // furniture being placed

                // Tile pos of mouse
                int xTile = (int) ((mouse.getX() + player.getX() - 480) / 60);
                int yTile = (int) ((mouse.getY() + player.getY() - 300) / 60);

                // Dimension of the furniture
                int length = Furniture.furnitureSizes.get(item.getName()).getKey();
                int width = Furniture.furnitureSizes.get(item.getName()).getValue();

                // Place the furniture at the specified position if it is valid
                if (validFurniturePlacement(xTile, yTile, length, width)) {
                    placeFurniture(xTile, yTile, length, width, item);
                    player.setPlacingFurniture(false);
                }
            }

            // Exit menu
            else if (exitMenuOpen) {
                if (new Rectangle(250, 80, 520, 80).contains(mouse)) {  // Save and continue
                    save(mainFrame.getNum());
                }

                else if (new Rectangle(250, 80 + 118, 520, 80).contains(mouse)) {  // Save and return to menu
                    save(mainFrame.getNum());
                    StartMenu menu = new StartMenu();
                    mainFrame.setPanel("");
					music.close();
                }

                else if (new Rectangle(250, 80 + 118*2, 520, 80).contains(mouse)) {  // Save and exit to desktop
                    save(mainFrame.getNum());
                    System.exit(0);
                    music.close();
                }

                else if (new Rectangle(250, 80 + 118*3, 520, 80).contains(mouse)) {  // Return to game
                    exitMenuOpen = false;
                }
            }
        }

        player.setRightClickMenuOpen(false);

        if (e.getButton() == MouseEvent.BUTTON3) {
            if (player.isInventoryOpen()) {  // Right click menu if clicked on item
                player.selectItem(mouse);
                if (player.getSelectedItem() != null) {
                    player.setRightClickMenuOpen(true);
                }
            }

            // Basically every action involving right click
            else if (!player.isTalkingToNPC() && !player.isActionProgressOpen() && !player.isItemFoundPrompt() &&!player.isMoving() && !exitMenuOpen) {
                // Mouse position
                int xTile = (int) ((mouse.getX() + player.getX() - 480) / 60);
                int yTile = (int) ((mouse.getY() + player.getY() - 300) / 60);


                NPC npc = npcAtPoint(xTile, yTile);

                // Talk to npc if there is one
                if (npc != null && isAdjacentToPlayer(npc.getxTile(), npc.getyTile())) {
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(npc.getId());

                    if (!npc.isMoving()) {  // Talk to npc
                    	animaleseSFX.play();
                        npc.setTalking(true);
                        npc.setSpeechStage(NPC.GREETING);
                    }
                    else {
                        npc.setStopQueued(true);  // By queuing it the npc will tak after it has finished moving
                    }

                    int playerDir;

                    // Make player face npc
                    if (npc.getxTile() == player.getxTile() + 1) {
                        playerDir = Player.RIGHT;
                    }
                    else if (npc.getyTile() == player.getyTile() - 1) {
                        playerDir = Player.UP;
                    }
                    else if (npc.getxTile() == player.getxTile() - 1) {
                        playerDir = Player.LEFT;
                    }
                    else {
                        playerDir = Player.DOWN;
                    }

                    player.setDirection(playerDir);
                    npc.setDirection((playerDir + 2) % 4);  // Make npc face player by setting its direction opposite of that of the player

                }

                // Talking to Tom Nook
                if (curRoom == tom_nook.getRoom() && (xTile == tom_nook.getxTile() && (yTile == tom_nook.getyTile() || yTile == tom_nook.getyTile() + 1))) {
                	animaleseSFX.play();
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(Player.TOM_NOOK);
                    tom_nook.setSpeechStage(NPC.GREETING);
                    tom_nook.resetDialogue();
                }

                // Talking to boat operator
                else if (curRoom == boat_operator.getRoom() && (xTile == boat_operator.getxTile()) && (yTile == boat_operator.getyTile()) && isAdjacentToPlayer(xTile, yTile)) {
                    animaleseSFX.play();
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(Player.BOAT_OPERATOR);
                    boat_operator.setSpeechStage(NPC.GREETING);
                    boat_operator.resetDialogue();
                }

                // Talking to boat operator on island
                else if (curRoom == boat_operator_on_island.getRoom() && (xTile == boat_operator_on_island.getxTile()) && (yTile == boat_operator_on_island.getyTile()) && isAdjacentToPlayer(xTile, yTile)) {
                    animaleseSFX.play();
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(Player.BOAT_OPERATOR_ON_ISLAND);
                    boat_operator_on_island.setSpeechStage(NPC.GREETING);
                    boat_operator_on_island.resetDialogue();
                }

                // Talking to Celeste
                else if (curRoom == celeste.getRoom() && (xTile == celeste.getxTile()) && (yTile == celeste.getyTile())) {
                	animaleseSFX.play();
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(Player.CELESTE);
                    celeste.setSpeechStage(NPC.GREETING);
                    celeste.resetDialogue();
                }

                // Talking to isabelle
                else if (curRoom == isabelle.getRoom() && (xTile == isabelle.getxTile()) && (yTile == isabelle.getyTile())) {
                	animaleseSFX.play();
                    player.setTalkingToNPC(true);
                    player.setDialogueSelectionOpen(true);
                    player.setVillagerPlayerIsTalkingTo(Player.ISABELLE);
                    isabelle.setSpeechStage(NPC.GREETING);
                    isabelle.resetDialogue();
                }

                // Picking up item if item is clicked
                if (Math.hypot(xTile*tileSize + 30 - (mouse.getX() + player.getX() - 480), yTile*tileSize + 30 - (mouse.getY() + player.getY() - 300)) < 19) {
                    DroppedItem droppedItem = curRoom.getDroppedItems().get(new Point(xTile, yTile));

                    if (grid[xTile][yTile] == 4 && droppedItem != null && isAdjacentToPlayer(xTile, yTile)) {
                        if (player.inventoryHasSpace()) {  // Adding item to player inventory
                            player.addItem(new Item(droppedItem.getId(), droppedItem.getName(), droppedItem.getImage(), droppedItem.getBuyCost(), droppedItem.getSellCost()));
                            Hashtable<Point, DroppedItem> temp = curRoom.getDroppedItems();
                            temp.remove(new Point(xTile, yTile));
                            curRoom.setDroppedItems(temp);
                            grid[xTile][yTile] = curRoom.getOriginalGrid()[xTile][yTile];
                            System.out.println(curRoom.getOriginalGrid()[xTile][yTile]);
                        }
                        else {
                            player.setInventoryFullPromptOpen(true);
                        }
                    }
                }

                // Picking fruit if player is adjacent to the tree and they do not have the net equipped
                if (treeAtTile(xTile, yTile) != null && treeAtTile(xTile, yTile).getNumFruit() > 0
                    && treeAtTile(xTile, yTile).isTileAdjacent(player.getxTile(), player.getyTile())
                && !(player.getEquippedItem() != null && player.getEquippedItem().getId() == 5)) {
                    if (player.inventoryHasSpace()) {  // adding fruit to player inventory
                        Tree tree = treeAtTile(xTile, yTile);
                        tree.pickFruit();

                        switch (tree.getFruit()) {
                            case (Tree.APPLE):
                                player.addItem(items.get(112));
                                break;
                            case (Tree.ORANGE):
                                player.addItem(items.get(113));
                                break;
                            case (Tree.PEACH):
                                player.addItem(items.get(114));
                                break;
                            case (Tree.PEAR):
                                player.addItem(items.get(115));
                                break;
                        }
                    }
                    else {
                        player.setInventoryFullPromptOpen(true);
                    }
                }

                // Catching bugs if net is equipped
                else if (treeAtTile(xTile, yTile) != null && treeAtTile(xTile, yTile).isTileAdjacent(player.getxTile(), player.getyTile())
                    && (player.getEquippedItem() != null && player.getEquippedItem().getId() == 5)) {
                    if (player.inventoryHasSpace()) {  // Starting the action progress
                        player.setActionProgressOpen(true);
                        player.setActionMessage("Catching bugs");
                        player.setAction(Player.BUG_CATCHING);
                    }

                    else {
                        player.setInventoryFullPromptOpen(true);
                    }
                }

                // Digging on main island
                else if (curRoom == outside && (player.getEquippedItem() != null && player.getEquippedItem().getId() == 6) && (grid[xTile][yTile] != 4) && isAdjacentToPlayer(xTile, yTile) && npcAtPoint(xTile, yTile) == null) {
                    if (player.inventoryHasSpace()) {
                        // Set action progress
                        player.setActionProgressOpen(true);
                        player.setAction(Player.DIGGING);
                        diggingSFX.play();

                        if (diggableTiles[xTile][yTile] == 1) {  // 1 --> can be dug; 0 --> cannot
                            diggableTiles[xTile][yTile] = 0;
                            if (grid[xTile][yTile] == 6) {  // Fossil digging
                                player.setAction(Player.DIGGING_FOSSIL);
                            }

                            grid[xTile][yTile] = 5;
                            player.setActionMessage("Digging");
                        }
                        else if (grid[xTile][yTile] == 5) {  // Filling up hole
                            diggableTiles[xTile][yTile] = 1;
                            if (grid[xTile][yTile] == 6) {
                                player.setAction(Player.DIGGING_FOSSIL);
                            }

                            grid[xTile][yTile] = 1;
                            player.setActionMessage("Filling up hole");
                        }
                    }
                   else {
                       player.setInventoryFullPromptOpen(true);
                    }
                }

                // Digging on minigame island; same process as on main island
                else if (curRoom == minigameIsland && (player.getEquippedItem() != null && player.getEquippedItem().getId() == 6) && (grid[xTile][yTile] != 4) && isAdjacentToPlayer(xTile, yTile)) {
                    if (player.inventoryHasSpace()) {
                        player.setActionProgressOpen(true);
                        player.setAction(Player.DIGGING);
                        diggingSFX.play();

                        if (minigameIslandDiggable[xTile][yTile] == 1) {
                            minigameIslandDiggable[xTile][yTile] = 0;
                            if (grid[xTile][yTile] == 6) {
                                player.setAction(Player.DIGGING_FOSSIL);
                            }
                            grid[xTile][yTile] = 5;
                            player.setActionMessage("Digging");
                        }
                        else if (grid[xTile][yTile] == 5) {
                            minigameIslandDiggable[xTile][yTile] = 1;
                            grid[xTile][yTile] = 1;
                            player.setActionMessage("Filling up hole");
                        }
                    }
                    else {
                        player.setInventoryFullPromptOpen(true);
                    }
                }

                // Fishing
                else if (player.getEquippedItem() != null && player.getEquippedItem().getId() == 1 && validFishingTile(xTile, yTile)) {
                    if (player.inventoryHasSpace()) {  // Set action progress
                        player.setActionProgressOpen(true);
                        player.setAction(Player.FISHING);
                        player.setActionMessage("Casting line");
                        player.setFishing(true);
                        fishingSFX.play();
                    }
                    else {
                        player.setInventoryFullPromptOpen(true);
                    }
                }

                // Arcade machines for minigames
                else if (curRoom == rooms.get(new Point(24, 21))) {
                    if (tileIsThinIce(xTile, yTile) && isAdjacentToPlayer(xTile, yTile)) {  // thin ice
                        mainFrame.changeGame("thin ice");
                    }

                    else if (tileIsAstroBarrier(xTile, yTile) && isAdjacentToPlayer(xTile, yTile)) {  // Astro barrier
                        mainFrame.changeGame("astro barrier");
                    }
                }

                // Player rrom
                if (curRoom == rooms.get(new Point(30, 35))) {
                    if (furnitureAtTile(xTile, yTile) != null) {  // Picking up furniture
                        Furniture furniture = furnitureAtTile(xTile, yTile);

                        if (player.inventoryHasSpace()) {  // adding furniture to inventory
                            player.addItem(new Item(furniture.getId(), capitalizeWord(items.get(furniture.getId()).getName()), items.get(furniture.getId()).getImage(),
                                items.get(furniture.getId()).getBuyCost(), items.get(furniture.getId()).getSellCost()));

                            // Setting the grid back to normal as no furniture is there anymore
                            for (int i = furniture.getxTile(); i < furniture.getxTile() + furniture.getLength(); i++) {
                                for (int j = furniture.getyTile(); j < furniture.getyTile() + furniture.getWidth(); j++) {
                                    grid[i][j] = 1;
                                }
                            }

                            player.getFurniture().remove(furniture);  // remove from arraylist

                        }
                        else {
                            player.setInventoryFullPromptOpen(true);
                        }
                    }
                }
            }
        }
    }

    // Gets the items that can be donated in the player inventory
    public boolean[][] findCanBeDonatedItems() {
        boolean[][] ans = new boolean[6][3];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                boolean canBeDonated = false;

                // Checks if item is fish, bug, or fossil and then checks the museum if that item has already been donated or not
                // If it has been donated already then it cannot be donated again
                if (player.getItems()[i][j] != null) {
                    canBeDonated = true;
                    Item temp = player.getItems()[i][j];
                    if (temp.isBug()) {
                        for (int k = 0; k < celeste.getBugs().size(); k++) {
                            if (temp.getName().equals(celeste.getBugs().get(k).getName())) {
                                canBeDonated = false;
                                break;
                            }
                        }
                    }
                    else if (temp.isOceanFish() || temp.isPondFish() || temp.isRiverFish()) {
                        for (int k = 0; k < celeste.getFish().size(); k++) {
                            if (temp.getName().equals(celeste.getFish().get(k).getName())) {
                                canBeDonated = false;
                                break;
                            }
                        }
                    }
                    else if (temp.isFossil()) {
                        for (int k = 0; k < celeste.getFossils().size(); k++) {
                            if (temp.getName().equals(celeste.getFossils().get(k).getName())) {
                                canBeDonated = false;
                                break;
                            }
                        }
                    }
                    else {  // If item is not a bug, fish, or fossil it cannot be donated
                        canBeDonated = false;
                    }
                }
                ans[i][j] = canBeDonated;

            }
        }

        return ans;
    }

    // Donates the selected items and adds to the appropriate arraylist
    public void donateItems() {
        Item[][] temp = player.getItems();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                if (player.getSelectedItems()[i][j]) {  // donate selected items
                    if (temp[i][j].isBug()) {
                        celeste.addBug(temp[i][j]);
                    }
                    else if (temp[i][j].isFossil()) {
                        celeste.addFossil(temp[i][j]);
                    }
                    else if (temp[i][j].isRiverFish() || temp[i][j].isPondFish() || temp[i][j].isOceanFish()) {
                        celeste.addFish(temp[i][j]);
                    }
                    temp[i][j] = null;  // remove item from inventory

                }
            }
        }
    }

    // Returns the tree at a tile if there is one otherwise null
    public Tree treeAtTile(int xTile, int yTile) {
        Tree ans = null;
        for (Tree tree : trees) {
            if (tree.getRoom() == curRoom && tree.isTileOnTree(xTile, yTile)) {
                ans = tree;
            }
        }

        return ans;

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            clicked = false;
            if (player.isInventoryOpen() && !player.isRightClickMenuOpen() && !clickedRightClickMenu) {  // If item is selected move the item
                player.moveItem(mouse);
            }

            // Select the dialogue if selecting dialogue
            if (player.isDialogueSelectionOpen() && player.isSelectionMenuClicked() && Math.hypot(510 - mouse.x, 186 - mouse.y) > 34) {
                player.setSelectionMenuClicked(false);
                selectDialogue();
            }
        }


    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    // Fading to black effect when changing rooms
    public void fadingToBlack(boolean isEntering, Graphics g) {
        if (count - fadeTimeStart == 100) {  // At 100 frames effect is done
            if (isEntering) {
                goToNewRoom();
            }
            else {
                exitRoom();
            }
            fadingToBlack = false;
            return;
        }
        int alpha = Math.min((int) ((double) (count - fadeTimeStart + 20) / 100 * 255), 255);  // opacity
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    // Draws the sprites
    public void paintComponent(Graphics g) {
        g.drawImage(curRoom.getImage(), 480 - player.getX(), 303 - player.getY(), null);  // Drawing room
        if (curRoom == rooms.get(new Point(30, 35))) {  // Draw player room if curRoom is player room
            drawPlayerRoom(g);
        }

        //drawGrids(g);
        //drawXs(g);

        // Drawing holes and buried objects
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == 5) {
                    g.drawImage(holeImage, i*tileSize - player.getX() + 480, j*tileSize - player.getY() + 300, null);
                }
                else if (grid[i][j] == 6) {
                    g.drawImage(buriedObjectImage, i*tileSize - player.getX() + 480, j*tileSize - player.getY() + 300, null);
                }
            }
        }

        // Drawing dropped items
        for (Map.Entry<Point, DroppedItem> pair : curRoom.getDroppedItems().entrySet()) {
            DroppedItem item = pair.getValue();
            if (item.isFurniture()) {
                g.drawImage(Item.leafImage, (item.getxTile()+8)*tileSize - player.getX()+13, (item.getyTile()+5)*tileSize - player.getY()+13, null);
            }
            else {
                g.drawImage(item.getImage(), (item.getxTile()+8)*tileSize - player.getX()+13, (item.getyTile()+5)*tileSize - player.getY()+13, null);
            }
        }

        // Drawing trees
        for (Tree tree : trees) {
            if (tree.getRoom() == curRoom) {
                tree.draw(g, player.getX(), player.getY());
            }
        }

        // Drawing npcs
        for (NPC temp : NPCs) {
            if (temp.getRoom() == curRoom) {
                temp.draw(g, player.getX(), player.getY());
            }
        }

        // Drawing player
        player.draw(g);

        // Drawing earned bells prompt if it's open
        if (player.isEarnedBellsPromptOpen()) {
            g.setColor(new Color(251, 255, 164));
            g.fillRect(200, 50, 620, 500);

            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;

                FontMetrics fontMetrics = new JLabel().getFontMetrics(GamePanel.finkheavy32);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(GamePanel.finkheavy32);
                g2.setColor(new Color(0, 0, 0));

                g.setColor(Color.WHITE);
                g.fillRect(440, 500, 140, 40);

                g.setColor(Color.BLACK);
                g.drawRect(440, 500, 140, 40);

                int width = fontMetrics.stringWidth("You earned " + gameScore/10 + " bells!");
                g2.drawString("You earned " + gameScore/10 + " bells!", 200 + (620 - width) / 2, 100);

                width = fontMetrics.stringWidth("Ok");
                g2.drawString("Ok", 200 + (620 - width) / 2, 532);

            }
        }

        // Drawing fade to black effect if active
        if (fadingToBlack) {
            fadingToBlack(player.isGoingToNewRoom(), g);
        }

        // Draw talking to npcs
        drawTalkingToGeneralNPC(g);
        drawTalkingToTomNook(g);
        drawTalkingToCeleste(g);

        // Hover text
        if (!player.isTalkingToNPC() && !player.isInventoryOpen() && !player.isActionProgressOpen() && !player.isItemFoundPrompt() && !player.isInventoryFullPromptOpen() && !exitMenuOpen) {
            drawHoverText(g);
        }

        // Placing furniture
        if (player.isPlacingFurniture()) {
            drawPlacingFurniture(g);
        }

        // Drawing exit menu
        if (exitMenuOpen) {
            g.setColor(new Color(251, 255, 164));
            g.fillRect(200, 50, 620, 500);

            for (int i = 0 ; i < 4; i++) {
                g.setColor(Color.WHITE);
                g.fillRect(250, 80 + 118 * i, 520, 80);
                g.setColor(Color.BLACK);
                g.drawRect(250, 80 + 118 * i, 520, 80);
            }

            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;

                FontMetrics fontMetrics = new JLabel().getFontMetrics(GamePanel.finkheavy32);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(GamePanel.finkheavy32);
                g2.setColor(new Color(0, 0, 0));

                g2.drawString("Save and continue", 250 + (520 - fontMetrics.stringWidth("Save and continue")) / 2, 130);
                g2.drawString("Save and return to menu", 250 + (520 - fontMetrics.stringWidth("Save and return to menu")) / 2, 248);
                g2.drawString("Save and exit to desktop", 250 + (520 - fontMetrics.stringWidth("Save and exit to desktop")) / 2, 366);
                g2.drawString("Return to game", 250 + (520 - fontMetrics.stringWidth("Return to game")) / 2, 484);
            }
        }
    }

    // Draws grids. Not used in game just during development
    public void drawGrids(Graphics g) {
        g.setColor(new Color(255, 255, 255));
        // Drawing grids
        g.setColor(new Color(255, 255, 255));
        g.drawRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(0, 0, 0));
        for (int i = 0; i < 1020; i+=tileSize) {
            g.drawLine(i, 0, i, 660);
        }

        for (int i = 0; i < 660; i+=tileSize) {
            g.drawLine(0, i, 1020, i);
        }
    }

    // Draws x's on tiles that cannot be accessed. Not used in game just during development
    public void drawXs(Graphics g) {
        g.setColor(new Color(255,0,0));
        for (int i = Math.max(0, player.getxTile()-8); i <= Math.min(94, player.getxTile() + 8); i++) {
            for (int j = Math.max(0, player.getyTile()-5); j < Math.min(85, player.getyTile() + 5); j++) {
                int a = i - Math.max(0, player.getxTile()-8);
                int b = j - Math.max(0, player.getyTile()-5);

                if (grid[i][j] == 0) {
                    g.drawLine(a*60, b*60, a*60 + 60, b*60 + 60);
                    g.drawLine(a*60, b*60+60, a*60+60, b*60);
                }
            }
        }
    }

    // Talking to npc and drawing speech
    public void drawTalkingToGeneralNPC(Graphics g) {
        if (player.isTalkingToNPC()) {
            NPC npc = NPCs.get(player.getVillagerPlayerIsTalkingTo());

            // Drawing speech bubble
            if (!(npc == tom_nook && npc.getSpeechStage() == Tom_Nook.SHOP) && !(npc == celeste && npc.getSpeechStage() == Celeste.MUSEUM)) {
                g.drawImage(speechBubbleImage, (1020 - 700) / 2, 350, null);
            }

            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;

                FontMetrics fontMetrics = new JLabel().getFontMetrics(finkheavy32);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(finkheavy32);
                g2.setColor(Color.BLACK);

                int x, y;  // x, y coordinates of text
                int width;  // width of text

                width = fontMetrics.stringWidth(npc.getName());

                x = 204 + (416 - 204 - width) / 2;
                y = 397;

                // Drawing npc name in the name spot
                if (!(npc == tom_nook && (npc.getSpeechStage() == Tom_Nook.SHOP))) {
                    g2.drawString(npc.getName(), x, y);
                }

                // Drawing the greetings speech
                if (npc.getSpeechStage() == NPC.GREETING) {
                    if (npc.getCurrentGreeting().equals("")) {
                        npc.generateGreeting(player.getName());
                    }

                    g2.setColor(new Color(240, 240, 240));

                    ArrayList<String> dialogue = wordWrap(npc.getCurrentGreeting(), 450);

                    for (int i = 0; i < dialogue.size(); i++) {
                        g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                    }
                }

                // Drawing the chat speech
                else if (npc.getSpeechStage() == NPC.CHAT) {
                    if (npc.getCurrentChat().equals("")) {
                        npc.generateChat(player.getName());
                    }

                    g2.setColor(new Color(240, 240, 240));

                    ArrayList<String> dialogue = wordWrap(npc.getCurrentChat(), 450);

                    for (int i = 0; i < dialogue.size(); i++) {
                        g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                    }
                }

                // Drawing the goodbye speech
                else if (npc.getSpeechStage() == NPC.GOODBYE) {
                    if (npc.getCurrentGoodbye().equals("")) {
                        npc.generateGoodbye(player.getName());
                    }

                    g2.setColor(new Color(240, 240, 240));

                    ArrayList<String> dialogue = wordWrap(npc.getCurrentGoodbye(), 450);

                    for (int i = 0; i < dialogue.size(); i++) {
                        g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                    }
                }


                // Drawing the sell shop
                if (npc == tom_nook && npc.getSpeechStage() == Tom_Nook.SELL_SHOP) {
                    g2.setColor(new Color(240, 240, 240));

                    ArrayList<String> dialogue = wordWrap("Select which items to sell.", 450);
                    for (int i = 0; i < dialogue.size(); i++) {
                        g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                    }
                }

                // Drawing the donation menu
                if (npc == celeste && npc.getSpeechStage() == Celeste.DONATION) {
                    g2.setColor(new Color(240, 240, 240));
                    if (player.hasItemToDonate()) {
                        ArrayList<String> dialogue = wordWrap("Select which items to donate.", 450);
                        for (int i = 0; i < dialogue.size(); i++) {
                            g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                        }
                    }
                    else {
                        ArrayList<String> dialogue = wordWrap("It seems you do not have anything to donate.", 450);
                        for (int i = 0; i < dialogue.size(); i++) {
                            g2.drawString(dialogue.get(i), 270, 470 + 40 * i);
                        }
                    }
                }

                // Drawing the dialogue selection
                if (player.isDialogueSelectionOpen()) {
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(310, 86, 400, 200);


                    if (player.isSelectionMenuClicked()) {
                        g.drawImage(selectionMenuNoClickImage, 421, 118, null);
                    }
                    else {
                        g.drawImage(selectionMenuImage, 421, 120, null);
                    }

                    g2.setColor(Color.BLACK);


                    // Drawing the options
                    if (npc.getPlayerOptions().size() == 2) {
                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(0));
                        g2.drawString(npc.getPlayerOptions().get(0), (1020 - width) / 2, 140);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(1));
                        g2.drawString(npc.getPlayerOptions().get(1), (1020 - width) / 2, 250);
                    }

                    else if (npc.getPlayerOptions().size() == 3) {
                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(0));
                        g2.drawString(npc.getPlayerOptions().get(0), (1020 - width) / 2, 140);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(1));
                        g2.drawString(npc.getPlayerOptions().get(1), 570, 195);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(2));
                        g2.drawString(npc.getPlayerOptions().get(2), (1020 - width) / 2, 250);
                    }

                    else if (npc.getPlayerOptions().size() == 4) {
                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(0));
                        g2.drawString(npc.getPlayerOptions().get(0), (1020 - width) / 2, 140);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(1));
                        g2.drawString(npc.getPlayerOptions().get(1), 570, 195);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(2));
                        g2.drawString(npc.getPlayerOptions().get(2), (1020 - width) / 2, 250);

                        width = fontMetrics.stringWidth(npc.getPlayerOptions().get(3));
                        g2.drawString(npc.getPlayerOptions().get(3), 450 - width, 195);
                    }
                }
            }
        }
    }

    // Draw the shop when talking to tom nook
    public void drawTalkingToTomNook(Graphics g) {
        if (player.isTalkingToNPC() && player.getVillagerPlayerIsTalkingTo() == Player.TOM_NOOK) {
            if (player.isShopOpen()) {
                g.drawImage(shopImage, (1020 - 390) / 2,  10, null);

                // Buy/cancel rects
                Rectangle br = tom_nook.getBuyRect();
                Rectangle cr = tom_nook.getCancelRect();

                g.setColor(Color.WHITE);
                g.fillRect(br.x, br.y, br.width, br.height);
                g.fillRect(cr.x, cr.y, cr.width, cr.height);

                g.setColor(Color.BLACK);
                g.drawRect(br.x, br.y, br.width, br.height);
                g.drawRect(cr.x, cr.y, cr.width, cr.height);

                // Drawing selected rect
                if (player.selectedItemInShop != -1) {
                    Rectangle temp = tom_nook.getItemRects().get(player.getSelectedItemInShop());

                    g.setColor(Color.GREEN);
                    g.drawRect(temp.x, temp.y, temp.width, temp.height);
                }

                g.setColor(Color.BLACK);

                // Drawing store items
                for (int i = 0; i < tom_nook.getStoreItems().size(); i++) {
                    if (tom_nook.getStoreItems().get(i).isFurniture()) {
                        g.drawImage(Item.storeLeafImage, 330,63 + 110*i, null);
                    }
                    else {
                        g.drawImage(tom_nook.getStoreItemImages()[tom_nook.getStoreItems().get(i).getId()], 330,63 + 110*i, null);
                    }

                    if (g instanceof Graphics2D) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.drawString(tom_nook.getStoreItems().get(i).getName(), 375, 101 + 110*i);
                        g2.drawString(String.valueOf(tom_nook.getStoreItems().get(i).getBuyCost()), 630, 101 + 110*i);

                        FontMetrics fontMetrics = new JLabel().getFontMetrics(finkheavy30);

                        int width = fontMetrics.stringWidth("Buy");
                        g2.drawString("Buy", 353 + (140 - width) / 2, 575 + 32);

                        width = fontMetrics.stringWidth("Cancel");
                        g2.drawString("Cancel", 533 + (140 - width) / 2, 575 + 32);
                    }

                }
                if (g instanceof Graphics2D) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.drawString(String.valueOf(player.getBells()), 400, 48);
                }
            }
        }
    }

    // Musueum drawing stuff
    public void drawTalkingToCeleste(Graphics g) {
         if (player.isMuseumOpen()) {
             if (g instanceof Graphics2D) {
                 Graphics2D g2 = (Graphics2D) g;
                 g2.setColor(Color.BLACK);
                 g2.setFont(finkheavy30);

                 g.drawImage(museumMenuImage, 50, 120, null);
                 g.drawImage(exitButtonImage, 930, 120, null);


                 // Drawing colored image based on which page is selected
                 if (celeste.getPage() == Celeste.BUG_PAGE) {
                     g.drawImage(museumBugsImage, 50, 120, null);
                 }
                 else if (celeste.getPage() == Celeste.FISH_PAGE) {
                     g.drawImage(museumFishImage, 50, 120, null);
                 }
                 else {
                     g.drawImage(museumFossilImage, 50, 120, null);
                 }


                 // Drawing museum items. Starts at the starting index and draws 5 items
                 for (int i = 0; i < 5; i++) {
                     if (celeste.getPage() == Celeste.BUG_PAGE && i + celeste.getBugStart() < celeste.getBugs().size()) {
                         g.drawImage(celeste.getBugs().get(i + celeste.getBugStart()).getImage(), 764, 260 + 49 * i, null);
                         g2.drawString(celeste.getBugs().get(i + celeste.getBugStart()).getName(), 220, 290 + 49 * i);
                     }
                     else if (celeste.getPage() == Celeste.FISH_PAGE && i + celeste.getFishStart() < celeste.getFish().size()) {
                         g.drawImage(celeste.getFish().get(i + celeste.getFishStart()).getImage(), 764, 260 + 49 * i, null);
                         g2.drawString(celeste.getFish().get(i + celeste.getFishStart()).getName(), 220, 290 + 49 * i);
                     }
                     else if (celeste.getPage() == Celeste.FOSSIL_PAGE && i + celeste.getFossilStart() < celeste.getFossils().size()){
                         g.drawImage(celeste.getFossils().get(i + celeste.getFossilStart()).getImage(), 764, 260 + 49 * i, null);
                         g2.drawString(celeste.getFossils().get(i + celeste.getFossilStart()).getName(), 220, 290 + 49 * i);
                     }
                 }
             }
         }
    }

    // Draws player room
    public void drawPlayerRoom(Graphics g) {
        // Floor and wallpaper images
        g.drawImage(Furniture.wallpaperImages.get(player.getSelectedWallpaper()), 9 * tileSize - player.getX() + 480,  6 * tileSize - player.getY() + 300, null);
        g.drawImage(Furniture.floorImages.get(player.getSelectedFloor()), 9 * tileSize - player.getX() + 480, 8 * tileSize - player.getY() + 300, null);


        // Drawing furniture
        for (Furniture temp : player.getFurniture()) {
            temp.draw(g, player.getX(), player.getY());
        }
    }


    // Takes a string and splits it into an arraylist where each element does not exceed a certain width
    public static ArrayList<String> wordWrap(String str, int width) {
        FontMetrics fontMetrics = new JLabel().getFontMetrics(finkheavy32);
        ArrayList<String> ans = new ArrayList<>();  // output
        String[] wordsArray = str.split(" ");
        ArrayList<String> words = new ArrayList<>();
        words.addAll(Arrays.asList(wordsArray));
        Collections.reverse(words);

        String word = "";  // Current word
        String string;  // current line

        while (words.size() > 0 || !word.equals("")) {
            string = "";  // Reset line
            while (words.size() > 0 || !word.equals("")) {
                if (word.equals("")) {  // Set word to be the next word
                    word = words.get(words.size() - 1);
                    words.remove(words.size() - 1);
                }

                // Add the word to the line if it does not exceed the limit otherwise go to next line
                if (fontMetrics.stringWidth(string) + fontMetrics.stringWidth(" " + word) <= width) {
                    string += " " + word;
                    word = "";
                }
                else {
                    break;
                }
            }
            ans.add(string.substring(1));  // Add to output arraylist
        }

        return ans;
    }

    // Draws hover text (e.g. right click to dig)
    public void drawHoverText(Graphics g) {
        int xTile = (int) ((mouse.getX() + player.getX() - 480) / 60);
        int yTile = (int) ((mouse.getY() + player.getY() - 300) / 60);

        // Dimensions of the rect that will be drawn
        int width;
        int height = 30;
        int x = mouse.x + 20;
        int y = mouse.y - 15;


        String msg = "";  // Message
        boolean draw = false;  // Whether or not text will be drawn

        // Fishing message
        if (player.getEquippedItem() != null && player.getEquippedItem().getId() == 1 && (curRoom == outside || curRoom == minigameIsland)) {
            if (validFishingTile(xTile, yTile)) {
                msg = "Cast line.";
                draw = true;
            }
        }

        // Pick up item message
        else if (xTile < grid.length && yTile < grid[0].length && grid[xTile][yTile] == 4) {
            msg = "Pick up item.";
            draw = true;
        }

        // Pick fruit message
        else if (treeAtTile(xTile, yTile) != null && treeAtTile(xTile, yTile).getNumFruit() > 0 &&
            !(player.getEquippedItem() != null && player.getEquippedItem().getId() == 5)) {
            msg = "Pick fruit";
            draw = true;
        }

        // Digging message
        else if (player.getEquippedItem() != null && player.getEquippedItem().getId() == 6 && (curRoom == outside || curRoom == minigameIsland)) {
            if (curRoom == outside && diggableTiles[xTile][yTile] == 1) {
                msg = "Dig";
                draw = true;
            }
            else if (curRoom == minigameIsland && minigameIslandDiggable[xTile][yTile] == 1) {
                msg = "Dig";
                draw = true;
            }
            else if (curRoom == outside && grid[xTile][yTile] == 5) {
                msg = "Fill";
                draw = true;
            }
            else if (curRoom == minigameIsland && grid[xTile][yTile] == 5) {
                msg = "Fill";
                draw = true;
            }
        }

        else if (npcAtPoint(xTile, yTile) != null) {
            msg = "Talk to villager.";
            draw = true;
        }

        else if (treeAtTile(xTile, yTile) != null && (player.getEquippedItem() != null && player.getEquippedItem().getId() == 5)) {
            msg = "Catch bugs";
            draw = true;
        }

        else if (curRoom == rooms.get(new Point(30, 35)) && furnitureAtTile(xTile, yTile) != null) {
            msg = "Pick up furniture";
            draw = true;
        }

        // Arcade machines
        else if (curRoom == rooms.get(new Point(24, 21))) {
            if (tileIsThinIce(xTile, yTile)) {
                msg = "Play Thin Ice";
                draw = true;
            }

            else if (tileIsAstroBarrier(xTile, yTile)) {
                msg = "Play Astro Barrier";
                draw = true;
            }
        }

        if (draw) {  // Draws the text and a rect if a message has been selected
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;

                FontMetrics fontMetrics = new JLabel().getFontMetrics(finkheavy15);
                width = fontMetrics.stringWidth(msg) + 40;

                g.setColor(new Color(255, 255, 255, 100));
                g.fillRect(x, y, width, height);
                g.drawImage(rightClickImage, x - 18, y - 15, null);

                g2.setFont(finkheavy15);
                g2.setColor(Color.BLACK);
                g2.drawString(msg, x + 25, y + 20);

            }
        }
    }

    // Selects the dialogue based on the selection angle (angle the mouse makes with the center of the selection circle)
    public void selectDialogue() {
        NPC npc = NPCs.get(player.getVillagerPlayerIsTalkingTo());
        player.setDialogueSelectionOpen(false);
        dialogueDelay = 0;

        // Regular villagers
        if ((player.getVillagerPlayerIsTalkingTo() >= 3 && player.getVillagerPlayerIsTalkingTo() < 6)) {
            if (npc.getPlayerOptions().size() == 2) {
                if (selectionAngle >= 0 && selectionAngle <= Math.PI) {  // Chat
                    if (npc.getSpeechStage() == NPC.GREETING) {
                        npc.setSpeechStage(NPC.CHAT);
                    }
                }
                else {  // Goodbye
                    npc.setSpeechStage(NPC.GOODBYE);
                    npc.resetDialogue();
                }
            }
        }

        if (npc == isabelle) {
            if (npc.getPlayerOptions().size() == 2) {
                if (selectionAngle >= 0 && selectionAngle <= Math.PI) {  // Chat
                    if (npc.getSpeechStage() == NPC.GREETING) {
                        npc.setSpeechStage(NPC.CHAT);
                    }
                }
                else {  // Goodbye
                    npc.setSpeechStage(NPC.GOODBYE);
                    npc.resetDialogue();
                }
            }
        }

        if (npc == tom_nook) {
            if (selectionAngle > (7.0/4.0)*Math.PI || selectionAngle <= Math.PI/4) {  // Sell item
                player.setDialogueSelectionOpen(false);
                player.setSellShopOpen(true);
                player.setSellAmount(0);
                player.setSelectedItems(new boolean[6][3]);
                tom_nook.setSpeechStage(Tom_Nook.SELL_SHOP);

            }
            else if (selectionAngle > Math.PI/4 && selectionAngle <= 3.0/4.0 * Math.PI) {  // Buy item
                player.setDialogueSelectionOpen(false);
                player.setShopOpen(true);
                tom_nook.setSpeechStage(Tom_Nook.SHOP);
                player.setSelectedItemInShop(-1);
            }
            else {  // Goodbye
                tom_nook.setSpeechStage(NPC.GOODBYE);
                npc.resetDialogue();
            }
        }

        else if (npc == boat_operator) {  // Go to island
            if (selectionAngle >= 0 && selectionAngle <= Math.PI) {
                player.setDialogueSelectionOpen(false);
                player.setTalkingToNPC(false);

                npc.setTalking(false);

                player.setGoingToNewRoom(true);
            }
            else {  // goodbye
                npc.setSpeechStage(NPC.GOODBYE);
                npc.resetDialogue();
            }
        }

        else if (npc == boat_operator_on_island) {  // return to main island
            if (selectionAngle >= 0 && selectionAngle <= Math.PI) {
                player.setDialogueSelectionOpen(false);
                player.setTalkingToNPC(false);

                npc.setTalking(false);

                player.setExitingRoom(true);
            }
            else {  // goodbye
                npc.setSpeechStage(NPC.GOODBYE);
                npc.resetDialogue();
            }
        }

        else if (npc == celeste) {
            if (selectionAngle > (7.0/4.0)*Math.PI || selectionAngle <= Math.PI/4) {  // Donate item
                player.setDialogueSelectionOpen(false);
                player.setDonateMenuOpen(true);
                npc.setSpeechStage(Celeste.DONATION);
                player.setSelectedItems(new boolean[6][3]);
                player.setCanBeDonatedItems(findCanBeDonatedItems());

            }
            else if (selectionAngle > Math.PI/4 && selectionAngle <= 3.0/4.0 * Math.PI) {  // View museum
                player.setMuseumOpen(true);
                player.setDialogueSelectionOpen(false);
                npc.setSpeechStage(Celeste.MUSEUM);
            }

            else if (selectionAngle > 5.0/4.0 * Math.PI && selectionAngle < 7.0/4.0 * Math.PI) {  // Goodbye
                npc.setSpeechStage(NPC.GOODBYE);
                npc.resetDialogue();
            }
        }
    }

    // Returns if a given tile is a valid fishing tale based on player position
    // Tile must be in line with player (i.e. xTile == player xTile or yTile == player yTile) and 2 or less tiles away
    public boolean validFishingTile(int xTile, int yTile) {
        if (curRoom == outside) {  // Outside
            if (waterTiles[xTile][yTile] != 0) {
                if (xTile == player.getxTile() && (Math.abs(yTile - player.getyTile()) <= 2 && Math.abs(yTile - player.getyTile()) > 0)) {
                    return true;
                }
                else if (yTile == player.getyTile() && (Math.abs(xTile - player.getxTile()) <= 2 && Math.abs(xTile - player.getxTile()) > 0)) {
                    return true;
                }
            }
        }
        else if (curRoom == minigameIsland) {  // Minigame island
            if (minigameIslandWater[xTile][yTile] != 0) {
                if (xTile == player.getxTile() && (Math.abs(yTile - player.getyTile()) <= 2 && Math.abs(yTile - player.getyTile()) > 0)) {
                    return true;
                }
                else if (yTile == player.getyTile() && (Math.abs(xTile - player.getxTile()) <= 2 && Math.abs(xTile - player.getxTile()) > 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check if a tile is a valid place to place furniture based on the dimensions of the furniture
    public boolean validFurniturePlacement(int xTile, int yTile, int length, int width) {
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                if (xTile + i > 31 || yTile + j > 22) {
                    return false;
                }
                if (grid[xTile + i][yTile+ j] != 1 || (xTile + i == player.getxTile() && yTile + j == player.getyTile())) {
                    return false;
                }
            }
        }
        return true;
    }

    // Draw the placing furniture preview
    public void drawPlacingFurniture(Graphics g) {
        if (player.getSelectedItemR() != -1 && player.getSelectedItemC() != -1) {
            Item item = player.getItems()[player.getSelectedItemR()][player.getSelectedItemC()];
            int xTile = (int) ((mouse.getX() + player.getX() - 480) / 60);
            int yTile = (int) ((mouse.getY() + player.getY() - 300) / 60);

            System.out.println(item.getName());
            int length = Furniture.furnitureSizes.get(item.getName()).getKey();
            int width = Furniture.furnitureSizes.get(item.getName()).getValue();


            for (int i = 0; i < length; i++) {
                for (int j = 0; j < width; j++) {
                    if (xTile + i <= 31 && yTile + j <= 22) {
                        // Draw green rect if placement is valid else red
                        if (validFurniturePlacement(xTile, yTile, length, width)) {
                            g.setColor(new Color(0, 255, 0, 100));
                        }
                        else {
                            g.setColor(new Color(255, 0, 0, 100));
                        }

                        g.fillRect(tileSize * (xTile + i) - player.getX() + 480, GamePanel.tileSize * (yTile + j) - player.getY() + 300, tileSize, tileSize);
                    }
                }
            }

            Furniture display = new Furniture(xTile, yTile, length, width, item.getId());
            display.draw(g, player.getX(), player.getY());  // Draw the preview
        }
    }

    // Return the furniture at a tile or null if there is none
    public Furniture furnitureAtTile(int xTile, int yTile) {
        for (Furniture temp : player.getFurniture()) {
            for (int i = temp.getxTile(); i < temp.getxTile() + temp.getLength(); i++) {
                for (int j = temp.getyTile(); j < temp.getyTile() + temp.getWidth(); j++) {
                    if (i == xTile && j == yTile) {
                        return temp;
                    }
                }
            }
        }
        return null;
    }

    // Getters
    public Point getMouse() {
        return mouse;
    }

    public boolean isClicked() {
        return clicked;
    }

    public static ArrayList<Item> getItems() {
        return items;
    }

    public ArrayList<NPC> getNPCs() {
        return NPCs;
    }

    public static boolean contains(int n, int[] array) {  // Check if array contains an int n
        for (int i = 0; i < array.length; i++) {
            if (array[i] == n) {
                return true;
            }
        }
        return false;
    }

    public static String capitalizeWord(String str) {  // Capitalize every word in a string
        String[] words = str.split("\\s");
        String capitalizeWord = "";
        for (String w:words) {
            String first = w.substring(0,1);
            String afterFirst = w.substring(1);
            capitalizeWord += first.toUpperCase() + afterFirst + " ";
        }
        return capitalizeWord.trim();
    }

    public static int randint(int low, int high){
        /*
            Returns a random integer on the interval [low, high].
        */
        return (int) (Math.random()*(high-low+1)+low);
    }

    public Room getCurRoom() {
        return curRoom;
    }

    public Room getPlayerRoom() {
        return rooms.get(new Point(30, 35));
    }

    // Places furniture
    public void placeFurniture(int xTile, int yTile, int length, int width, Item item) {
        player.getFurniture().add(new Furniture(xTile, yTile, length, width, item.getId()));

        player.getItems()[player.getSelectedItemR()][player.getSelectedItemC()] = null;

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < width; j++) {
                grid[xTile+i][yTile+j] = 0;  // Set the grid so player cannot go on the tiles where the furniture is
            }
        }
    }

    // returns if the tile is a thin ice machine
    public boolean tileIsThinIce(int xTile, int yTile) {
        for (int i = 0; i < thinIceArcadeTiles.length; i++) {
            if (xTile == thinIceArcadeTiles[i][0] && yTile == thinIceArcadeTiles[i][1]) {
                return true;
            }
        }
        return false;
    }

    // returns if the tile is an astro barrier machine
    public boolean tileIsAstroBarrier(int xTile, int yTile) {
        for (int i = 0; i < astroBarrierArcadeTiles.length; i++) {
            if (xTile == astroBarrierArcadeTiles[i][0] && yTile == astroBarrierArcadeTiles[i][1]) {
                return true;
            }
        }
        return false;
    }

    // Saves the file in a text file
    public void save(int num) {
        PrintWriter outFile;
        try {  // Main stuff
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/save" + num + ".txt")));

            outFile.println(player.getName()); // name
            outFile.println(player.getGender()); // gender
            outFile.println(player.getBells()); // bells

            String line = "";

            for (int i = 0; i < 6; i++) {  // items
                for (int j = 0; j < 3; j++) {
                    if (player.getItems()[i][j] == null) {
                        outFile.println("null");
                    }
                    else {
                        outFile.println(player.getItems()[i][j].getId());
                    }
                }
            }

            // Equipped item
            if (player.getEquippedItem() == null) {
                outFile.println("null");
            }
            else {
                outFile.println(player.getEquippedItem().getId());
            }


            outFile.println(player.getSelectedWallpaper()); // wallpaper
            outFile.println(player.getSelectedFloor()); // floor

            // museum bugs
            line = "";
            for (Item temp : celeste.getBugs()) {
                line += temp.getId() + " ";
            }

            if (line.length() > 0) {
                line = line.substring(0, line.length()-1);
            }

            outFile.println(line);

            // Museum fish
            line = "";
            for (Item temp : celeste.getFish()) {
                line += temp.getId() + " ";
            }
            if (line.length() > 0) {
                line = line.substring(0, line.length()-1);
            }
            outFile.println(line);

            // Museum fossils
            line = "";
            for (Item temp : celeste.getFossils()) {
                line += temp.getName() + ",";
            }
            if (line.length() > 0) {
                line = line.substring(0, line.length()-1);
            }
            outFile.println(line);

            // timestamp of last on
            long unixTime = Instant.now().getEpochSecond();
            outFile.println(unixTime);

            // Furniture
            for (Furniture furniture : player.getFurniture()) {
                outFile.println(furniture.getxTile() + " " + furniture.getyTile() + " " + furniture.getLength() + " " + furniture.getWidth() + " " + furniture.getId());
            }


            outFile.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {  // Trees
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/trees.txt")));

            outFile.println(29);
            for (Tree tree : trees) {
                if (tree.getRoom() == outside) {
                    outFile.println(tree.getxTile() + " " + tree.getyTile() + " " + tree.getSize() + " " + tree.getFruit() + " " + tree.getNumFruit());
                }
            }

            outFile.println(8);
            for (Tree tree : trees) {
                if (tree.getRoom() == minigameIsland) {
                    outFile.println(tree.getxTile() + " " + tree.getyTile() + " " + tree.getSize() + " " + tree.getFruit() + " " + tree.getNumFruit());
                }
            }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        try {  // Main map
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/map.txt")));

            for (int i = 0; i < outside.getGrid()[0].length; i++) {
                String line = "";
                for (int j = 0; j < outside.getGrid().length; j++) {
                    if (outside.getGrid()[j][i] == 4 || outside.getGrid()[j][i] == 6) {
                        outside.getGrid()[j][i] = 1;
                    }
                    line += outside.getGrid()[j][i] + " ";
                }
                line = line.substring(0, line.length() - 1);
                outFile.println(line);
            }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {  // Minigame island
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/minigame island map.txt")));

            for (int i = 0; i < minigameIsland.getGrid()[0].length; i++) {
                String line = "";
                for (int j = 0; j < minigameIsland.getGrid().length; j++) {
                    if (minigameIsland.getGrid()[j][i] == 4 || outside.getGrid()[j][i] == 6) {
                        minigameIsland.getGrid()[j][i] = 1;
                    }

                    line += minigameIsland.getGrid()[j][i] + " ";
                }
                line = line.substring(0, line.length() - 1);
                outFile.println(line);
            }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try { // Rooms
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/rooms.txt")));

            ArrayList<Room> temp = new ArrayList<>(rooms.values());

            outFile.println(8);

            for (Room room : temp) {
                if (room != outside && room != minigameIsland) {
                    outFile.println(room.getName() + ".png");
                    outFile.println(room.getEntryX() + " " + room.getEntryY() + " " + room.getExitX() + " " + room.getExitY() + " " + room.getExitX2() + " " + room.getExitY2());
                    outFile.println(room.getGrid().length + " " +  room.getGrid()[0].length);

                    for (int i = 0; i < room.getGrid()[0].length; i++) {
                        String line = "";
                        for (int j = 0; j < room.getGrid().length; j++) {
                            if (room.getGrid()[j][i] == 4 || outside.getGrid()[j][i] == 6) {
                                room.getGrid()[j][i] = 1;
                            }

                            line += room.getGrid()[j][i] + " ";
                        }
                        line = line.substring(0, line.length() - 1);
                        outFile.println(line);
                    }
                }
             }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        try {  // Main map diggable
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/outside diggable tiles.txt")));

            for (int i = 0; i < diggableTiles[0].length; i++) {
                String line = "";
                for (int j = 0; j < diggableTiles.length; j++) {
                    line += diggableTiles[j][i] + " ";
                }
                line = line.substring(0, line.length() - 1);
                outFile.println(line);
            }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {  // Minigame island diggable
            outFile = new PrintWriter(
                new BufferedWriter(new FileWriter("Saves/save" + num + "/minigame island diggable tiles.txt")));

            for (int i = 0; i < minigameIslandDiggable[0].length; i++) {
                String line = "";
                for (int j = 0; j < minigameIslandDiggable.length; j++) {
                    line += minigameIslandDiggable[j][i] + " ";
                }
                line = line.substring(0, line.length() - 1);
                outFile.println(line);
            }

            outFile.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}