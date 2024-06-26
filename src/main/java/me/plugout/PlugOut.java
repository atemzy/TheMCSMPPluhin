package me.plugout;

import commands.*;
import models.PlayerNote;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import utils.NoteStorageUtil;
import java.util.ArrayList;
import java.util.List;

public final class PlugOut extends JavaPlugin implements Listener {
    private static PlugOut PLUGIN;
    public PlugOut(){
        PLUGIN = this;
    }
    public static List<String> opsAwaitingLogin = new ArrayList<String>();
    public static PlugOut GetPlugin(){
        return PLUGIN;
    }
    public static void QuickLog(String message){
        String prefix = "[POU]";
        System.out.println(prefix + " " + message);
    }
    @Override
    public void onEnable() {
        PLUGIN = this;
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("register").setExecutor(new RegisterCommand());
        getCommand("setglobalspawn").setExecutor(new SetGlobalSpawn(this));
        getCommand("setglobalspawn").setTabCompleter(new SetGlobalSpawnCompleter());
        getCommand("login").setExecutor(new LoginCommand());
        getCommand("testme").setExecutor(new TestCommand());

    }

    @Override
    public void onDisable() {
        for(String player : opsAwaitingLogin){
            getServer().getPlayer(player).setOp(true);
        }
        opsAwaitingLogin.clear();
        QuickLog("Plugin Killed.");
    }
    @EventHandler
    public void OnHungerChange(FoodLevelChangeEvent e){
        FileConfiguration conf = getConfig();
        boolean isEnabled = conf.getBoolean("useLoginSpawn");
        String w = conf.getString("loginSpawnLocation.world");
        World world = getServer().getWorld(w);
        boolean isPlayer = e.getEntity() instanceof Player;

        if(isEnabled && isPlayer){
            if(e.getEntity().getWorld() != world) return;
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void OnHealthChange(EntityRegainHealthEvent e){
        FileConfiguration conf = getConfig();
        boolean isEnabled = conf.getBoolean("useLoginSpawn");
        String w = conf.getString("loginSpawnLocation.world");
        World world = getServer().getWorld(w);
        boolean isPlayer = e.getEntity() instanceof Player;

        if(isEnabled && isPlayer){
            if(e.getEntity().getWorld() != world) return;
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void OnBlockBreak(BlockBreakEvent e){
        FileConfiguration conf = getConfig();
        boolean isEnabled = conf.getBoolean("useLoginSpawn");
        String w = conf.getString("loginSpawnLocation.world");
        World world = getServer().getWorld(w);
        boolean canOpBypass = conf.getBoolean("canOpBypass");

        if(isEnabled && !canOpBypass){
            if(e.getPlayer().getWorld() != world) return;
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent pje){
        Player _player = pje.getPlayer();
        if(_player.isOp() && getConfig().getBoolean("useLoginSpawn")){
            opsAwaitingLogin.add(_player.getName());
            _player.setOp(false);
        }
        if(_player.isDead()){
            FileConfiguration config = getConfig();
            double x = config.getDouble("loginSpawnLocation.x");
            double y = config.getDouble("loginSpawnLocation.y");
            double z = config.getDouble("loginSpawnLocation.z");
            String w = config.getString("loginSpawnLocation.world");
            World world = getServer().getWorld(w);
            _player.setInvulnerable(config.getBoolean("invulnerableInSpawnWorld"));
            float yaw = (float)config.getDouble("loginSpawnLocation.yaw");
            float pitch = (float)config.getDouble("loginSpawnLocation.pitch");
            Location loc = new Location(world, x, y, z, yaw, pitch);
            _player.setBedSpawnLocation(loc, true);
        }
        _player.getInventory().setContents(new ItemStack[]{});
        _player.getInventory().setArmorContents(new ItemStack[]{});
        _player.updateInventory();
        pje.setJoinMessage(ChatColor.GREEN + "++ " + _player.getDisplayName());
        FileConfiguration config = getConfig();
        boolean useGSpawn = config.getBoolean("useLoginSpawn");
        if(!useGSpawn) return;
        double x = config.getDouble("loginSpawnLocation.x");
        double y = config.getDouble("loginSpawnLocation.y");
        double z = config.getDouble("loginSpawnLocation.z");
        String w = config.getString("loginSpawnLocation.world");
        World world = getServer().getWorld(w);
        _player.setInvulnerable(config.getBoolean("invulnerableInSpawnWorld"));
        float yaw = (float)config.getDouble("loginSpawnLocation.yaw");
        float pitch = (float)config.getDouble("loginSpawnLocation.pitch");
        Location loc = new Location(world, x, y, z, yaw, pitch);
        _player.teleport(loc);
    }
    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent pqe){
        Player _player = pqe.getPlayer();
        pqe.setQuitMessage(ChatColor.RED + "-- " + _player.getDisplayName());
        FileConfiguration conf = getConfig();
        if(_player.getWorld() == getServer().getWorld(String.valueOf(conf.get("loginSpawnLocation.world")))) return;
        PlayerNote playerNote = NoteStorageUtil.ReadPlayerNote(_player);
        if(opsAwaitingLogin.contains(_player.getName())){
            _player.setOp(true);
            opsAwaitingLogin.remove(_player.getName());
        }
        if(playerNote == null) {
            throw new RuntimeException();
        }
        playerNote.playWorldLoc = _player.getLocation();
        Inventory inv = _player.getInventory();
        playerNote.playWorldInv = inv.getContents();
        playerNote.respawnLoc = _player.getBedSpawnLocation() != null ? _player.getBedSpawnLocation() : null;
        try {
            QuickLog("saving...");
            NoteStorageUtil.SavePlayerNote(playerNote);
            QuickLog("saved");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
