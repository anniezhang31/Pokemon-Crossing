/*
    NPC.java
    Nick Liu + Annie Zhang
    ICS4U
    NPC class allows villagers to be created and also deals with the stationary npcs, such as tom nook and celeste.
 */

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;

public class NPC {
    // Dialogue
    public static final ArrayList<String> greetings = new ArrayList<>();
    public static final ArrayList<String> chats = new ArrayList<>();
    public static final ArrayList<String> goodbyes = new ArrayList<>();

    // Direction constants
    public static final int RIGHT = 0;
    public static final int UP = 1;
    public static final int LEFT = 2;
    public static final int DOWN = 3;

    // Position
    private int x, y;
    private int xTile, yTile;
    private int goingToxTile, goingToyTile;

    private int direction = DOWN;
    private boolean moving = false;

    private final int tileSize = GamePanel.tileSize;

    private Hashtable<String, Image> images;

    private final String name;
    private final String catchphrase;

    private int movementTick = 0;
    private int frame = 0;

    private Room room;
    private int id;

    private boolean talking = false;
    private boolean stopQueued = false;

    // Speech
    private String currentGreeting = "";
    private String currentChat = "";
    private String currentGoodbye = "";

    private ArrayList<String> playerOptions = new ArrayList<>();  // Dialogue responses the player can choose


    // Speech stage constants
    private int speechStage = 0;
    public static final int GREETING = 0;
    public static final int CHAT = 1;
    public static final int GOODBYE = 2;
    public static final int QUEST = 3;



    // Constructor
    public NPC(String name, Hashtable<String, Image> images, int xTile, int yTile, String catchphrase, Room room, int id) {
        this.name = name;
        this.images = images;
        this.xTile = xTile;
        this.yTile = yTile;
        this.catchphrase = catchphrase;
        this.room = room;
        this.x = tileSize * xTile;
        this.y = tileSize * yTile;
        goingToxTile = xTile;
        goingToyTile = yTile;
        this.id = id;


        // Default options
        playerOptions.add("Let's chat!");
        playerOptions.add("Never mind.");

    }

    // Loads the dialogue from the text files
    public static void loadDialogue() {
        try {
            Scanner stdin = new Scanner(new BufferedReader(new FileReader("Assets/NPCs/greetings.txt")));
            int n = Integer.parseInt(stdin.nextLine());
            for (int i = 0; i < n; i++) {
                greetings.add(stdin.nextLine().trim());
            }

            stdin = new Scanner(new BufferedReader(new FileReader("Assets/NPCs/chats.txt")));
            n = Integer.parseInt(stdin.nextLine());
            for (int i = 0; i < n; i++) {
                chats.add(stdin.nextLine().trim());
            }

            stdin = new Scanner(new BufferedReader(new FileReader("Assets/NPCs/goodbyes.txt")));
            n = Integer.parseInt(stdin.nextLine());
            for (int i = 0; i < n; i++) {
                goodbyes.add(stdin.nextLine().trim());
            }

        }
        catch (FileNotFoundException e) {
            System.out.println("Dialogue stuffs not found");
        }
    }

    // Draws the npc in relation to the player's position and what frame and direction it is on
    public void draw(Graphics g, int playerX, int playerY) {
        if (!moving) {  // Standing still
            switch (direction) {
                case (RIGHT):
                    g.drawImage(images.get("right0"), x - playerX + 480, y - playerY + 300 - 12, null);
                    break;
                case (UP):
                    g.drawImage(images.get("back0"), x - playerX + 480, y - playerY + 300 - 12,null);
                    break;
                case (LEFT):
                    g.drawImage(images.get("left0"), x - playerX + 480, y - playerY + 300 - 12, null);
                    break;
                case (DOWN):
                    g.drawImage(images.get("front0"), x - playerX + 480, y - playerY + 300 - 12, null);
                    break;
            }
        }
        else {  // Moving
            if (movementTick % 15 == 0) {
                frame++;
            }

            switch (direction) {
                case (RIGHT):
                    switch (frame % 4) {
                        case (0):
                            g.drawImage(images.get("right1"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (1):
                        case (3):
                            g.drawImage(images.get("right0"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (2):
                            g.drawImage(images.get("right2"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                    }
                    break;
                case (UP):
                    switch (frame % 2) {
                        case (0):
                            g.drawImage(images.get("back1"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (1):
                            g.drawImage(images.get("back2"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                    }
                    break;
                case (LEFT):
                    switch (frame % 4) {
                        case (0):
                            g.drawImage(images.get("left1"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (1):
                        case (3):
                            g.drawImage(images.get("left0"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (2):
                            g.drawImage(images.get("left2"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                    }
                    break;
                case (DOWN):
                    switch (frame % 2) {
                        case (0):
                            g.drawImage(images.get("front1"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                        case (1):
                            g.drawImage(images.get("front2"), x - playerX + 480, y - playerY + 300 - 12, null);
                            break;
                    }
                    break;
            }
        }
    }


    // Moves the villagers
    public void move(int[][] grid, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {
        // 1/200 chance per frame that the npc will move
        // Will not move while talking
        if (!moving && GamePanel.randint(1, 200) == 1 && !talking) {
            // 1/10 chance that the direction will be switched when moving
            // If the tile in front cannot be accessed it will also change direction
            if (GamePanel.randint(1, 10) == 1 || inDir(grid, direction, playerX, playerY, pgX, pgY, npcs) != 1) {
                int newDir = direction;
                int count = 0;
                // Generate a new direction until a valid one is found
                while (newDir == direction || inDir(grid, direction, playerX, playerY, pgX, pgY, npcs) != 1 && count < 5) {
                    newDir = GamePanel.randint(0, 3);
                    count++;
                }
                direction = newDir;
            }

            // Move the npc
            if (inDir(grid, direction, playerX, playerY, pgX, pgY, npcs) == 1) {
                moving = true;
                movementTick = 0;
                frame = 0;

                switch (direction) {  // Set destination
                    case (RIGHT):
                        goingToxTile = xTile + 1;
                        goingToyTile = yTile;
                        break;
                    case (UP):
                        goingToxTile = xTile;
                        goingToyTile = yTile - 1;
                        break;
                    case (LEFT):
                        goingToxTile = xTile - 1;
                        goingToyTile = yTile;
                        break;
                    case (DOWN):
                        goingToxTile = xTile;
                        goingToyTile = yTile + 1;
                        break;
                }
            }
        }

        // Change position
        if (moving) {
            int speed = 2;
            switch (direction) {
                case (RIGHT):
                    x += speed;
                    break;
                case (UP):
                    y -= speed;
                    break;
                case (LEFT):
                    x -= speed;
                    break;
                case (DOWN):
                    y += speed;
                    break;
            }
            movementTick++;
        }

        // NPC has reached new tile
        if (x % tileSize == 0 && y % tileSize == 0) {
            // Set tile pos
            xTile = x / tileSize;
            yTile = y / tileSize;
            moving = false;
        }
    }

    // Get tile in front of player. Also checks if a player or other npc is in front as well
    public int inDir(int[][] grid, int dir, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {
        int ans = 0;
        playerX /= GamePanel.tileSize;
        playerY /= GamePanel.tileSize;

        switch (dir) {
            case (Player.RIGHT):
                ans = grid[xTile+1][yTile];
                if ((xTile + 1 == playerX && yTile == playerY) || (xTile + 1 == pgX && yTile == pgY)) {
                    ans = 0;
                }

                for (NPC temp : npcs) {
                    if (!temp.name.equals(name) && ((xTile + 1 == temp.xTile && yTile == temp.yTile) || (xTile + 1 == temp.goingToxTile && yTile == temp.goingToyTile))) {
                        ans = 0;
                    }
                }
                break;
            case (Player.UP):
                ans = grid[xTile][yTile-1];
                if ((xTile == playerX && yTile - 1 == playerY) || (xTile == pgX && yTile - 1 == pgY)) {
                    ans = 0;
                }

                for (NPC temp : npcs) {
                    if (!temp.name.equals(name) && ((xTile == temp.xTile && yTile - 1 == temp.yTile) || (xTile == temp.goingToxTile && yTile - 1 == temp.goingToyTile))) {
                        ans = 0;
                    }
                }
                break;
            case (Player.LEFT):
                ans = grid[xTile-1][yTile];
                if ((xTile - 1 == playerX && yTile == playerY) || (xTile - 1 == pgX && yTile == pgY)) {
                    ans = 0;
                }

                for (NPC temp : npcs) {
                    if (!temp.name.equals(name) && ((xTile - 1 == temp.xTile && yTile == temp.yTile) || (xTile - 1 == temp.goingToxTile && yTile == temp.goingToyTile))) {
                        ans = 0;
                    }
                }
                break;
            case (Player.DOWN):
                ans = grid[xTile][yTile+1];
                if ((xTile == playerX && yTile + 1 == playerY) || (xTile == pgX && yTile + 1 == pgY)) {
                    ans = 0;
                }

                for (NPC temp : npcs) {
                    if (!temp.name.equals(name) && ((xTile == temp.xTile && yTile + 1 == temp.yTile) || (xTile == temp.goingToxTile && yTile + 1 == temp.goingToyTile))) {
                        ans = 0;
                    }
                }
                break;
        }
        if (ans == 4 || ans == 6) {
            ans = 1;
        }
        return ans;
    }

    // Getters and setters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getxTile() {
        return xTile;
    }

    public int getyTile() {
        return yTile;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int dir) {
        direction = dir;
    }

    public boolean isMoving() {
        return moving;
    }

    public Hashtable<String, Image> getImages() {
        return images;
    }

    public String getName() {
        return name;
    }

    public String getCatchphrase() {
        return catchphrase;
    }

    public Room getRoom() {
        return room;
    }

    public int getGoingToxTile() {
        return goingToxTile;
    }

    public int getGoingToyTile() {
        return goingToyTile;
    }

    public int getId() {
        return id;
    }

    public boolean isTalking() {
        return talking;
    }

    public void setTalking(Boolean b) {
        talking = b;
    }

    public boolean isStopQueued() {
        return stopQueued;
    }

    public void setStopQueued(Boolean b) {
        stopQueued = b;
    }

    public void generateGreeting(String name) {
        String greeting = greetings.get(GamePanel.randint(0, greetings.size() - 1));
        greeting = greeting.replace("[PLAYER]", name);
        greeting = greeting.replace("[CATCHPHRASE]", catchphrase);

        currentGreeting = greeting;
    }

    public String getCurrentGreeting() {
        return currentGreeting;
    }

    public void setCurrentGreeting(String s) {
        currentGreeting = s;
    }

    public void generateChat(String name) {
        String chat = chats.get(GamePanel.randint(0, chats.size() - 1));
        chat = chat.replace("[PLAYER]", name);
        chat = chat.replace("[CATCHPHRASE]", catchphrase);

        currentChat = chat;
    }

    public String getCurrentChat() {
        return currentChat;
    }

    public void generateGoodbye(String name) {
        String goodbye = goodbyes.get(GamePanel.randint(0, goodbyes.size() - 1));
        goodbye = goodbye.replace("[PLAYER]", name);
        goodbye = goodbye.replace("[CATCHPHRASE]", catchphrase);

        currentGoodbye = goodbye;
    }

    public String getCurrentGoodbye() {
        return currentGoodbye;
    }

    public ArrayList<String> getPlayerOptions() {
        return playerOptions;
    }

    public int getSpeechStage() {
        return speechStage;
    }

    public void setSpeechStage(int n) {
        speechStage = n;
    }

    public void resetDialogue() {
        currentGreeting = "";
        currentChat = "";
        currentGoodbye = "";
    }
}

// Class for Tom Nook
class Tom_Nook extends NPC {
    private final Image image = new ImageIcon("Assets/NPCs/tom nook.png").getImage();

    private ArrayList<Item> storeItems = new ArrayList<>();  // items in store

    private final Image[] storeItemImages = new Image[GamePanel.getItems().size()];  // item images in store

    private ArrayList<String> playerOptions;

    // More speech stages
    public static final int SHOP = 4;
    public static final int HOUSING = 5;
    public static final int SELL_SHOP = 6;

    private final Rectangle buyRect = new Rectangle(353, 575,140, 40);
    private final Rectangle cancelRect = new Rectangle(533, 575,140, 40);
    private final ArrayList<Rectangle> itemRects = new ArrayList<>();

    // Constructor
    public Tom_Nook(String name, Hashtable<String, Image> images, int xTile, int yTile, String catchphrase, Room room, int id) {
        super(name, images, xTile, yTile, catchphrase, room, id);

        playerOptions = super.getPlayerOptions();

        // Setting player options
        playerOptions.clear();
        playerOptions.add("Buy.");
        playerOptions.add("Sell.");
        playerOptions.add("Never mind.");

        // Adding item rects
        for (int i = 0; i < 5; i++) {
            itemRects.add(new Rectangle(332, 64 + 109*i, 395, 56));
        }
    }

    @Override
    public void draw(Graphics g, int playerX, int playerY) {
        g.drawImage(image, getxTile() * GamePanel.tileSize - playerX + 480, getyTile() * GamePanel.tileSize - playerY + 300, null);
    }

    // Can't move so move is blank
    @Override
    public void move(int[][] grid, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {

    }

    // Randomly generates 5 items sold at store
    public void generateStoreItems() {
        storeItems.clear();
        for (int i = 0; i < 5; i++) {
            Item item = GamePanel.getItems().get(Item.soldAtStore[GamePanel.randint(0, Item.soldAtStore.length - 1)]);
            if (item.isFloor()) {
                item.setName(Furniture.floorNames[GamePanel.randint(0, Furniture.floorNames.length - 1)]);
            }
            else if (item.isWallpaper()) {
                item.setName(Furniture.wallpaperNames[GamePanel.randint(0, Furniture.wallpaperNames.length - 1)]);
            }
            storeItems.add(item);

        }

        for (int i : Item.soldAtStore) {  // Getting images
            storeItemImages[i] = GamePanel.getItems().get(i).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        }
    }

    public Image getImage() {
        return image;
    }

    @Override
    public ArrayList<String> getPlayerOptions() {
        return playerOptions;
    }

    public Image[] getStoreItemImages() {
        return storeItemImages;
    }

    public ArrayList<Item> getStoreItems() {
        return storeItems;
    }

    public Rectangle getBuyRect() {
        return buyRect;
    }

    public Rectangle getCancelRect() {
        return cancelRect;
    }

    public ArrayList<Rectangle> getItemRects() {
        return itemRects;
    }
}


class Boat_Operator extends NPC {
    private ArrayList<String> playerOptions;

    public Boat_Operator(String name, Hashtable<String, Image> images, int xTile, int yTile, String catchphrase, Room room, int id) {
        super(name, images, xTile, yTile, catchphrase, room, id);
        playerOptions = getPlayerOptions();
        playerOptions.clear();
        if (id == 6) {
            playerOptions.add("Go to Minigame Island.");
        }
        else {
            playerOptions.add("Return to Main Island.");
        }
        playerOptions.add("Never mind.");
    }


    @Override
    public void draw(Graphics g, int playerX, int playerY) {

    }

    @Override
    public void move(int[][] grid, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {

    }

}

class Celeste extends NPC {
    private ArrayList<String> playerOptions;
    private final Image image = new ImageIcon("Assets/NPCs/celeste.png").getImage();

    // Speech stages
    public static final int MUSEUM = 4;
    public static final int DONATION = 5;

    // Museum collection
    private ArrayList<Item> fish = new ArrayList<>();
    private ArrayList<Item> bugs = new ArrayList<>();
    private ArrayList<Item> fossils = new ArrayList<>();

    // What index the museum page is starting
    private int fossilStart = 0;
    private int bugStart = 0;
    private int fishStart = 0;

    // Page constants
    public static final int BUG_PAGE = 0;
    public static final int FISH_PAGE = 1;
    public static final int FOSSIL_PAGE = 2;

    // Museum rects for clicking on different pages
    private Rectangle bugRect = new Rectangle(367, 120, 307, 95);
    private Rectangle fishRect = new Rectangle(50, 120, 307, 95);
    private Rectangle fossilRect = new Rectangle(614, 120, 307, 95);

    // What page the museum is on
    private int page = 0;

    public Celeste(String name, Hashtable<String, Image> images, int xTile, int yTile, String catchphrase, Room room, int id) {
        super(name, images, xTile, yTile, catchphrase, room, id);
        playerOptions = getPlayerOptions();
        playerOptions.clear();

        playerOptions.add("View museum.");
        playerOptions.add("Donate.");
        playerOptions.add("Never mind.");
    }

    @Override
    public void draw(Graphics g, int playerX, int playerY) {
        g.drawImage(image, getxTile() * GamePanel.tileSize - playerX + 480, getyTile() * GamePanel.tileSize - playerY + 300, null);
    }

    @Override
    public void move(int[][] grid, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {

    }

    // Getters and setters
    public ArrayList<Item> getFish() {
        return fish;
    }

    public ArrayList<Item> getBugs() {
        return bugs;
    }

    public ArrayList<Item> getFossils() {
        return fossils;
    }

    public int getFossilStart() {
        return fossilStart;
    }

    public int getBugStart() {
        return bugStart;
    }

    public int getFishStart() {
        return fishStart;
    }

    public void setFossilStart(int n) {
        fossilStart = n;
    }

    public void setBugStart(int n) {
        bugStart = n;
    }

    public void setFishStart(int n) {
        fishStart = n;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Rectangle getBugRect() {
        return bugRect;
    }

    public Rectangle getFishRect() {
        return fishRect;
    }

    public Rectangle getFossilRect() {
        return fossilRect;
    }

    // Adds a new bug into the collection in the appropriate spot alphabetically
    public void addBug(Item item) {
        if (bugs.contains(item)) {  // Don't add it if it is already there
            return;
        }

        if (bugs.size() == 0) {  // Arraylist is empty add it
            bugs.add(item);
        }
        else {  // Iterate through and find the appropriate position in alphabetical order
            if (item.getName().compareTo(bugs.get(0).getName()) < 0) {
                bugs.add(0, item);
            }
            else if (item.getName().compareTo((bugs.get(bugs.size() - 1).getName())) > 0) {  // Add to end if it is greater than the end
                bugs.add(item);
            }
            else {
                for (int i = 0; i < bugs.size() - 2; i++) {
                    if (item.getName().compareTo(bugs.get(i).getName()) > 0 && item.getName().compareTo(bugs.get(i+1).getName()) < 0) {
                        bugs.add(i+1, item);
                        break;
                    }
                    else if (item.getName().compareTo(bugs.get(i).getName()) == 0) {
                        return;
                    }
                }
            }
        }
    }

    // Same as bugs but for fish
    public void addFish(Item item) {
        if (fish.contains(item)) {
            return;
        }

        if (fish.size() == 0) {
            fish.add(item);
        }
        else {
            if (item.getName().compareTo(fish.get(0).getName()) < 0) {
                fish.add(0, item);
            }
            else if (item.getName().compareTo((fish.get(fish.size() - 1).getName())) > 0) {
                fish.add(item);
            }
            else {
                for (int i = 0; i < fish.size() - 2; i++) {
                    if (item.getName().compareTo(fish.get(i).getName()) > 0 && item.getName().compareTo(fish.get(i+1).getName()) < 0) {
                        fish.add(i+1, item);
                        break;
                    }
                    else if (item.getName().compareTo(fish.get(i).getName()) == 0) {
                        return;
                    }
                }
            }
        }
    }

    // Same as bugs but for fossils
    public void addFossil(Item item) {
        if (fossils.contains(item)) {
            return;
        }

        if (fossils.size() == 0) {
            fossils.add(item);
        }
        else {
            if (item.getName().compareTo(fossils.get(0).getName()) < 0) {
                fossils.add(0, item);
            }
            else if (item.getName().compareTo((fossils.get(fossils.size() - 1).getName())) > 0) {
                fossils.add(item);
            }
            else {
                for (int i = 0; i < fossils.size() - 2; i++) {
                    if (item.getName().compareTo(fossils.get(i).getName()) > 0 && item.getName().compareTo(fossils.get(i+1).getName()) < 0) {
                        fossils.add(i+1, item);
                        break;
                    }
                    else if (item.getName().compareTo(fossils.get(i).getName()) == 0) {
                        return;
                    }
                }
            }
        }
    }
}

class Isabelle extends NPC {
    private final Image image = new ImageIcon("Assets/NPCs/isabelle.png").getImage();

    public Isabelle(String name, Hashtable<String, Image> images, int xTile, int yTile, String catchphrase, Room room, int id) {
        super(name, images, xTile, yTile, catchphrase, room, id);
    }

    @Override
    public void draw(Graphics g, int playerX, int playerY) {
        g.drawImage(image, getxTile() * GamePanel.tileSize - playerX + 480, getyTile() * GamePanel.tileSize - playerY + 300 - 50, null);
    }

    @Override
    public void move(int[][] grid, int playerX, int playerY, int pgX, int pgY, ArrayList<NPC> npcs) {

    }
}
