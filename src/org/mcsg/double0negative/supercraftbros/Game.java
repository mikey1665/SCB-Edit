package org.mcsg.double0negative.supercraftbros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.mcsg.double0negative.supercraftbros.classes.PlayerClass;
import org.mcsg.double0negative.tabapi.TabAPI;

import com.gmail.Jacob6816.scb.utils.Gameboard;

public class Game {
    
    public enum State {
        INGAME, LOBBY, DISABLED, WAITING
    }
    
    // 2lolololol
    
    private int gameID;
    private int spawnCount;
    private Arena arena;
    private State state;
    private Gameboard b;
    private HashMap<Player, Integer> players = new HashMap<Player, Integer>();
    private HashMap<Player, PlayerClass> pClasses = new HashMap<Player, PlayerClass>();
    private ArrayList<Player> inactive = new ArrayList<Player>();
    private ArrayList<Player> queue = new ArrayList<Player>();
    
    public Game(int a) {
        this.gameID = a;
        
        init();
    }
    
    public void init() {
        FileConfiguration s = SettingsManager.getInstance().getSystemConfig();
        int x = s.getInt("system.arenas." + gameID + ".x1");
        int y = s.getInt("system.arenas." + gameID + ".y1");
        int z = s.getInt("system.arenas." + gameID + ".z1");
        System.out.println(x + " " + y + " " + z);
        int x1 = s.getInt("system.arenas." + gameID + ".x2");
        int y1 = s.getInt("system.arenas." + gameID + ".y2");
        int z1 = s.getInt("system.arenas." + gameID + ".z2");
        System.out.println(x1 + " " + y1 + " " + z1);
        Location max = new Location(SettingsManager.getGameWorld(gameID), Math.max(x, x1), Math.max(y, y1), Math.max(z, z1));
        System.out.println(max.toString());
        Location min = new Location(SettingsManager.getGameWorld(gameID), Math.min(x, x1), Math.min(y, y1), Math.min(z, z1));
        System.out.println(min.toString());
        
        arena = new Arena(min, max);
        
        state = State.LOBBY;
        
        spawnCount = SettingsManager.getInstance().getSpawnCount(gameID);
        
    }
    
    public void addPlayer(Player p) {
        if (state == State.LOBBY && getPlayers().size() < 10) {
            p.teleport(SettingsManager.getInstance().getGameLobbySpawn(gameID));
            
            getPlayers().put(p, 3);
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20D);
            p.setFoodLevel(20);
            
            TabAPI.setPriority(GameManager.getInstance().getPlugin(), p, 2);
            p.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Joined arena " + gameID + ". Select a class! \nHit tab for HUD!");
            msgAll(ChatColor.GREEN + p.getName() + " joined the game!");
            updateTabAll();
        }
        else if (state == State.INGAME) {
            p.sendMessage(ChatColor.RED + "Game already started!");
        }
        else if (getPlayers().size() >= 10) {
            p.sendMessage(ChatColor.RED + "Game Full!");
        }
        else {
            p.sendMessage(ChatColor.RED + "Cannot join game!");
        }
        
    }
    
    public void startGame() {
        if (getPlayers().size() < 2) {
            msgAll("Not enough players");
            return;
        }
        inactive.clear();
        state = State.INGAME;
        b = new Gameboard(this);
        b.setup(true);
        for (Player p : getPlayers().keySet().toArray(new Player[0])) {
            if (pClasses.containsKey(p)) {
                spawnPlayer(p);
            }
            else {
                removePlayer(p, false);
                p.sendMessage(ChatColor.RED + "You didn't pick a class!");
            }
            
        }
    }
    
    int count = 20;
    int tid = 0;
    
    public void countdown(int time) {
        count = time;
        Bukkit.getScheduler().cancelTask(tid);
        
        if (state == State.LOBBY) {
            tid = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin) GameManager.getInstance().getPlugin(), new Runnable() {
                public void run() {
                    if (count > 0) {
                        if (count % 10 == 0) {
                            msgAll(ChatColor.BLUE + "Game starting in " + count);
                        }
                        if (count < 6) {
                            msgAll(ChatColor.BLUE + "Game starting in " + count);
                        }
                        count--;
                    }
                    else {
                        startGame();
                        Bukkit.getScheduler().cancelTask(tid);
                    }
                }
            }, 0, 20);
            
        }
    }
    
    boolean started = false;
    
    public void setPlayerClass(Player player, PlayerClass playerClass) {
        if (player.hasPermission("scb.class." + playerClass.getName())) {
            clearPotions(player);
            player.sendMessage(ChatColor.GREEN + "You choose " + playerClass.getName() + "!");
            // int prev = pClasses.keySet().size();
            pClasses.put(player, playerClass);
            updateTabAll();
            if (!started && pClasses.keySet().size() >= 4 && getPlayers().size() >= 4) {
                countdown(60);
                started = true;
            }
        }
        else {
            player.sendMessage(ChatColor.RED + "You do not have permission for this class!");
        }
    }
    
    public void killPlayer(Player p, String msg) {
        clearPotions(p);
        
        msgAll(ChatColor.GOLD + msg);
        int lives = getPlayers().get(p) - 1;
        if (lives <= 0) {
            playerEliminate(p);
            
        }
        else {
            getPlayers().put(p, lives);
            msgAll(p.getName() + " has " + lives + " lives left");
        }
        b.setup(false);
    }
    
    @SuppressWarnings("deprecation")
    public void playerEliminate(Player p) {
        started = false;
        msgAll(ChatColor.DARK_RED + p.getName() + " has been eliminated!");
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        getPlayers().remove(p);
        // pClasses.remove(p);
        inactive.add(p);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        p.updateInventory();
        p.setAllowFlight(false);
        p.setFlying(false);
        clearPotions(p);
        p.teleport(SettingsManager.getInstance().getLobbySpawn());
        p.setDisplayName(p.getName());
        
        if (getPlayers().keySet().size() <= 1 && state == State.INGAME) {
            Player pl = null;
            for (Player pl2 : getPlayers().keySet()) {
                pl = pl2;
            }
            Bukkit.broadcastMessage(ChatColor.BLUE + pl.getName() + " won Super Craft Bros on arena " + gameID);
            gameEnd();
        }
        TabAPI.setPriority(GameManager.getInstance().getPlugin(), p, -1);
        TabAPI.updatePlayer(p);
        p.setDisplayName(p.getName());
        updateTabAll();
        
    }
    
    public void clearPotions(Player p) {
        for (PotionEffectType e : PotionEffectType.values()) {
            if (e != null && p.hasPotionEffect(e)) p.removePotionEffect(e);
        }
    }
    
    @SuppressWarnings("deprecation")
    public void gameEnd() {
        /*
         * for(Entity e:SettingsManager.getGameWorld(gameID).getEntities()){
         * if(arena.containsBlock(e.getLocation())){ e.remove(); } }
         */
        for (Player p : getPlayers().keySet()) {
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.updateInventory();
            p.teleport(SettingsManager.getInstance().getLobbySpawn());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            clearPotions(p);
            TabAPI.setPriority(GameManager.getInstance().getPlugin(), p, -2);
            TabAPI.updatePlayer(p);
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setDisplayName(p.getName());
        }
        getPlayers().clear();
        pClasses.clear();
        inactive.clear();
        state = State.LOBBY;
        
    }
    
    public void updateTabAll() {
        for (Player p : getPlayers().keySet()) {
            updateTab(p);
        }
    }
    
    public void updateTab(Player p) {
        Plugin plugin = GameManager.getInstance().getPlugin();
        TabAPI.setTabString(plugin, p, 0, 0, "        \u00a7lSuper");
        TabAPI.setTabString(plugin, p, 0, 1, "   \u00a7lCraft");
        TabAPI.setTabString(plugin, p, 0, 2, "  \u00a7lBros");
        TabAPI.setTabString(plugin, p, 1, 1, "   \u00a7lBrawl");
        TabAPI.setTabString(plugin, p, 2, 0, " \u00a76\u00a7l----------");
        TabAPI.setTabString(plugin, p, 2, 1, "\u00a7e\u00a7l----------");
        TabAPI.setTabString(plugin, p, 2, 2, "\u00a76\u00a7l---------- ");
        
        TabAPI.setTabString(plugin, p, 4, 0, "\u00a7lArena");
        TabAPI.setTabString(plugin, p, 4, 1, gameID + TabAPI.nextNull());
        TabAPI.setTabString(plugin, p, 5, 0, "\u00a7lClass");
        TabAPI.setTabString(plugin, p, 5, 1, (getPlayerClass(p) != null) ? getPlayerClass(p).getName() + TabAPI.nextNull() : "None " + TabAPI.nextNull());
        
        TabAPI.setTabString(plugin, p, 7, 0, "\u00a7e\u00a7lPlayer");
        TabAPI.setTabString(plugin, p, 7, 1, "\u00a7e\u00a7lLives");
        TabAPI.setTabString(plugin, p, 7, 2, "\u00a7e\u00a7lClass");
        
        int a = 8;
        for (Player pl : getPlayers().keySet()) {
            int h = convertHealth(((Damageable) pl).getHealth());
            TabAPI.setTabString(plugin, p, a, 0, pl.getName(), h);
            TabAPI.setTabString(plugin, p, a, 1, "\u00a7a" + getPlayers().get(pl) + TabAPI.nextNull(), h);
            TabAPI.setTabString(plugin, p, a, 2, (getPlayerClass(pl) != null) ? getPlayerClass(pl).getName() + TabAPI.nextNull() : "None " + TabAPI.nextNull(), h);
            a++;
        }
        
        if (state == State.INGAME) {
            for (Player pl : inactive) {
                TabAPI.setTabString(plugin, p, a, 0, pl.getName(), -1);
                TabAPI.setTabString(plugin, p, a, 1, "\u00a7c0" + TabAPI.nextNull(), -1);
                TabAPI.setTabString(plugin, p, a, 2, (getPlayerClass(pl) != null) ? getPlayerClass(pl).getName() + TabAPI.nextNull() : "None " + TabAPI.nextNull(), -1);
                
                a++;
            }
        }
        TabAPI.updatePlayer(p);
        
    }
    
    private int convertHealth(double h) {
        if (h > 17) {
            return 1;
        }
        else if (h > 14) {
            return 151;
        }
        else if (h > 10) {
            return 301;
        }
        else if (h > 5) {
            return 601;
        }
        else if (h > 2) {
            return 1001;
        }
        else {
            return -1;
        }
        
    }
    
    public void spawnPlayer(Player p) {
        if (getPlayers().containsKey(p)) {
            p.setAllowFlight(true);
            Random r = new Random();
            Location l = SettingsManager.getInstance().getSpawnPoint(gameID, r.nextInt(spawnCount) + 1);
            p.teleport(getSafePoint(l));
            getPlayerClass(p).PlayerSpawn();
        }
        
    }
    
    @SuppressWarnings("deprecation")
    public Location getSafePoint(Location l) {
        if (isInVoid(l)) {
            while (l.getBlockY() < 256) {
                if (l.getBlock().getTypeId() != 0) {
                    return l.add(0, 1, 0);
                }
                else {
                    l.add(0, 1, 0);
                }
            }
        }
        return l; // nothing safe at this point
    }
    
    @SuppressWarnings("deprecation")
    public boolean isInVoid(Location l) {
        Location loc = l.clone();
        while (loc.getBlockY() > 0) {
            loc.add(0, -1, 0);
            if (loc.getBlock().getTypeId() != 0) { return false; }
        }
        return true;
    }
    
    public int getID() {
        return gameID;
    }
    
    public boolean isBlockInArena(Location v) {
        return arena.containsBlock(v);
    }
    
    public void addSpawn() {
        spawnCount++;
    }
    
    public boolean isPlayerActive(Player p) {
        return getPlayers().keySet().contains(p);
    }
    
    public boolean isInQueue(Player p) {
        return queue.contains(p);
    }
    
    public void removeFromQueue(Player p) {
        queue.remove(p);
    }
    
    @SuppressWarnings("deprecation")
    public void removePlayer(Player p, boolean b) {
        getPlayers().remove(p);
        p.getInventory().clear();
        p.updateInventory();
        clearPotions(p);
        playerEliminate(p);
        inactive.remove(p);
        p.teleport(SettingsManager.getInstance().getLobbySpawn());
        msgAll(ChatColor.RED + p.getName() + " left the game!");
    }
    
    public void msgAll(String msg) {
        for (Player p : getPlayers().keySet()) {
            p.sendMessage(msg);
        }
    }
    
    public void enable() {
        if (state != State.DISABLED) {
            disable();
        }
        state = State.LOBBY;
    }
    
    public void disable() {
        for (Player p : getPlayers().keySet().toArray(new Player[0])) {
            playerEliminate(p);
            p.sendMessage(ChatColor.RED + "Game Disabled");
        }
        gameEnd();
        state = State.DISABLED;
        
    }
    
    public State getState() {
        return state;
    }
    
    public PlayerClass getPlayerClass(Player p) {
        return pClasses.get(p);
    }
    
    public Set<Player> getActivePlayers() {
        return getPlayers().keySet();
    }
    
    public HashMap<Player, Integer> getPlayers() {
        return players;
    }
    
    public void setPlayers(HashMap<Player, Integer> players) {
        this.players = players;
    }
    
    public Gameboard getBoard() {
        return b;
    }
    
}
