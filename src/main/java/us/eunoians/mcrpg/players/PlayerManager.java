package us.eunoians.mcrpg.players;

import com.cyr1en.flatdb.Database;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.party.Party;
import us.eunoians.mcrpg.types.DisplayType;
import us.eunoians.mcrpg.types.Skills;
import us.eunoians.mcrpg.types.TipType;
import us.eunoians.mcrpg.util.mcmmo.MobHealthbarUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlayerManager {

  //Players who are currently logged on
  private static HashMap<UUID, McRPGPlayer> players = new HashMap<>();
  private static ArrayList<UUID> playersFrozen = new ArrayList<>();
  private static McRPG plugin;
  private static BukkitTask saveTask;

  public PlayerManager(McRPG plugin){
    PlayerManager.plugin = plugin;
  }

  public static void addMcRPGPlayer(Player player, boolean freeze) {
    if(players.containsKey(player.getUniqueId())) {
      return;
    }
    UUID uuid = player.getUniqueId();
    if(freeze) {
      playersFrozen.add(uuid);
    }

    BukkitTask task = new BukkitRunnable() {
      public void run() {
        McRPGPlayer mp = getPlayer(uuid);
        mp.getUsedTips().add(TipType.LOGIN_TIP);
        if(mp.isOnline()) {
          if(!mp.isIgnoreTips()) {
            List<String> possibleMessages = McRPG.getInstance().getLangFile().getStringList("Messages.Tips.LoginTips");
            Random rand = new Random();
            int val = rand.nextInt(possibleMessages.size());
            new BukkitRunnable(){
              @Override
              public void run() {
                if(mp.isOnline()){
                  mp.getPlayer().sendMessage(Methods.color(mp.getPlayer(), possibleMessages.get(val)));
                }
              }
            }.runTaskLater(McRPG.getInstance(), 40L);
          }
          players.put(uuid, mp);
        }
        playersFrozen.remove(uuid);
      }
    }.runTaskAsynchronously(plugin);
  }

  private static McRPGPlayer createMcRPGPlayer(UUID uuid) {
    McRPGPlayer.McRPGPlayerBuilder builder = McRPGPlayer.builder()
        .uuid(uuid)
        .guardianSummonChance(plugin.getConfig().getDouble("PlayerConfiguration.PoseidonsGuardian.DefaultSummonChance"));

    Database database = plugin.getMcRPGDb().getDatabase();
    Optional<ResultSet> playerDataSet = database.executeQuery("SELECT * FROM mcrpg_player_data WHERE uuid = '" + uuid.toString() + "'");

    boolean isNew = false;
    try {
      if(playerDataSet.isPresent()) {
        isNew = !playerDataSet.get().next();
      }
      else {
        isNew = true;
      }
    } catch(SQLException e) {
      e.printStackTrace();
    }

    if (isNew) {
      for (Skills type : Skills.values()) {
        String query = "INSERT INTO mcrpg_" + type.getName() + "_data (uuid) VALUES ('" + uuid.toString() + "')";
        database.executeUpdate(query);
      }
      String query = "INSERT INTO MCRPG_PLAYER_SETTINGS (UUID) VALUES ('" + uuid.toString() + "')";
      database.executeUpdate(query);
      query = "INSERT INTO MCRPG_PLAYER_DATA (UUID) VALUES ('" + uuid.toString() + "')";
      database.executeUpdate(query);
      query = "INSERT INTO MCRPG_LOADOUT (UUID) VALUES ('" + uuid.toString() + "')";
      database.executeUpdate(query);
      playerDataSet = database.executeQuery("SELECT * FROM mcrpg_player_data WHERE uuid = '" + uuid.toString() + "'");
      try {
        playerDataSet.get().next();
      } catch(SQLException e) {
        e.printStackTrace();
      }
    }

    playerDataSet.ifPresent(resultSet -> {
      try {
        builder
            .abilityPoints(resultSet.getInt("ability_points"))
            .redeemableExp(resultSet.getInt("redeemable_exp"))
            .redeemableLevels(resultSet.getInt("redeemable_levels"))
            .boostedExp(resultSet.getInt("boosted_exp"))
            .divineEscapeExpDebuff(resultSet.getDouble("divine_escape_exp_debuff"))
            .divineEscapeExpEnd(resultSet.getInt("divine_escape_exp_end_time"))
            .divineEscapeDamageEnd(resultSet.getInt("divine_escape_damage_end_time"));
        String partyIDString = resultSet.getString("party_uuid");
        UUID partyID;
        if(partyIDString.equalsIgnoreCase("nu")) {
          partyID = null;
        }
        else {
          partyID = UUID.fromString(partyIDString);
          Party party = plugin.getPartyManager().getParty(partyID);
          StringBuilder nullPartyMessage = new StringBuilder();
          if(party == null) {
            partyID = null;
            nullPartyMessage.append("&cYour party no longer exists.");
          }
          else {
            if(!party.isPlayerInParty(uuid)) {
              partyID = null;
              nullPartyMessage.append("&cYou were removed from your party whilst offline.");
            }
          }
          if (nullPartyMessage.length() != 0) {
            new BukkitRunnable() {
              @Override
              public void run() {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if(offlinePlayer.isOnline()) {
                  ((Player) offlinePlayer).sendMessage(Methods.color(plugin.getPluginPrefix() + nullPartyMessage.toString()));
                }
              }
            }.runTaskLater(plugin, 2 * 20);
          }
        }
        long replaceCooldown = resultSet.getLong("replace_ability_cooldown_time");
        if(System.currentTimeMillis() < replaceCooldown) {
          builder.endTimeForReplaceCooldown(replaceCooldown);
        }
        builder.partyID(partyID);
      } catch(SQLException e) {
        e.printStackTrace();
      }
    });

    Optional<ResultSet> settingsSet = database.executeQuery("SELECT * FROM mcrpg_player_settings WHERE uuid = '" + uuid.toString() + "'");
    settingsSet.ifPresent(rs -> {
      try {
        if(rs.next()) {
          builder.healthbarType(MobHealthbarUtils.MobHealthbarType.fromString(rs.getString("health_type")))
              .keepHandEmpty(rs.getBoolean("keep_hand"))
              .displayType(DisplayType.fromString(rs.getString("display_type")))
              .autoDeny(rs.getBoolean("auto_deny"))
              .ignoreTips(rs.getBoolean("ignore_tips"))
              .requireEmptyOffHand(rs.getBoolean("require_empty_offhand"))
              .unarmedIgnoreSlot(rs.getInt("unarmed_ignore_slot"));
        }
      } catch(SQLException e) {
        e.printStackTrace();
      }
    });

    return builder.build();
  }

  public static boolean isPlayerFrozen(UUID uuid) {
    if(isPlayerStored(uuid)){
      playersFrozen.remove(uuid);
    }
    return playersFrozen.contains(uuid);
  }

  public static McRPGPlayer getPlayer(UUID uuid) {
    if (Bukkit.getPlayer(uuid) != null && players.containsKey(uuid)) {
      return players.get(uuid);
    } else {
      return createMcRPGPlayer(uuid);//McRPGPlayer.builder().uuid(uuid).mcRPG(plugin/* plugin reference should be refactored out eventually */).build();
    }
  }

  public static boolean isPlayerStored(UUID uuid) {
    return players.containsKey(uuid);
  }

  public static void removePlayer(UUID uuid) {
    players.remove(uuid).saveData();
  }

  public static void startSave(Plugin p) {
    plugin = (McRPG) p;
    if(saveTask != null) {
      System.out.println(Methods.color(plugin.getPluginPrefix() + "&eRestarting player saving task...."));
      saveTask.cancel();
    }
    saveTask = new BukkitRunnable(){
      @Override
      public void run() {
        PlayerManager.run();
      }
    }.runTaskTimerAsynchronously(p, 500, ((McRPG) p).getFileManager().getFile(FileManager.Files.CONFIG).getInt("Configuration.SaveInterval") * 1200);
    System.out.println(Methods.color(plugin.getPluginPrefix() + "&aPlayer saving task has been started!"));
    new BukkitRunnable() {
      @Override
      public void run() {
        Collection<McRPGPlayer> clone = ((HashMap<UUID, McRPGPlayer>) players.clone()).values();
        for(McRPGPlayer mp : clone) {
          if(isPlayerFrozen(mp.getUuid())) {
            continue;
          }
          mp.updateCooldowns();
        }
      }
    }.runTaskTimer(p, 0, 20);
  }


  private static void run() {
    HashMap<UUID, McRPGPlayer> clone = (HashMap<UUID, McRPGPlayer>) players.clone();
    if(players.values() != null && !players.values().isEmpty()) {
      clone.values().forEach(McRPGPlayer::saveData);
    }
  }

  public static void saveAll() {run();}

  public static void shutDownManager() {
    saveAll();
    if(saveTask != null) {
      saveTask.cancel();
    }
  }
}
