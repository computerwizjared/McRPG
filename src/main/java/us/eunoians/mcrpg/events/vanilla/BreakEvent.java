package us.eunoians.mcrpg.events.vanilla;

import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.herbalism.DiamondFlowers;
import us.eunoians.mcrpg.abilities.herbalism.Replanting;
import us.eunoians.mcrpg.abilities.herbalism.TooManyPlants;
import us.eunoians.mcrpg.abilities.mining.DoubleDrop;
import us.eunoians.mcrpg.abilities.mining.ItsATriple;
import us.eunoians.mcrpg.abilities.mining.RemoteTransfer;
import us.eunoians.mcrpg.abilities.mining.RicherOres;
import us.eunoians.mcrpg.api.events.mcrpg.*;
import us.eunoians.mcrpg.api.util.DiamondFlowersData;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.api.util.RemoteTransferTracker;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.players.PlayerManager;
import us.eunoians.mcrpg.types.DefaultAbilities;
import us.eunoians.mcrpg.types.GainReason;
import us.eunoians.mcrpg.types.Skills;
import us.eunoians.mcrpg.types.UnlockedAbilities;
import us.eunoians.mcrpg.util.Parser;
import us.eunoians.mcrpg.util.mcmmo.HerbalismMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class BreakEvent implements Listener {

  @EventHandler(priority = EventPriority.LOW)
  @SuppressWarnings("Duplicates")
  public void breakEvent(BlockBreakEvent event) {
    if(!event.isCancelled() && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {

      Player p = event.getPlayer();
      Block block = event.getBlock();
      McRPGPlayer mp = PlayerManager.getPlayer((p).getUniqueId());
      FileConfiguration mining = McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG);
      FileConfiguration herbalism = McRPG.getInstance().getFileManager().getFile(FileManager.Files.HERBALISM_CONFIG);
      //Deal with herbalism
      if(herbalism.getBoolean("HerbalismEnabled")) {
        int dropMultiplier = 1;
        if(!McRPG.getPlaceStore().isTrue(block)) {
          if(herbalism.contains("ExpAwardedPerBlock." + block.getType().toString())) {
            int expWorth = herbalism.getInt("ExpAwardedPerBlock." + block.getType().toString());
            boolean oneBlockPlant = !(block.getType() == Material.CACTUS || block.getType() == Material.CHORUS_PLANT || block.getType() == Material.SUGAR_CANE);
            if(!oneBlockPlant) {
              int amount = HerbalismMethods.calculateMultiBlockPlantDrops(block.getState());
              expWorth *= amount;
            }
            mp.giveExp(Skills.HERBALISM, expWorth, GainReason.BREAK);
          }
        }
        if(DefaultAbilities.TOO_MANY_PLANTS.isEnabled() && mp.getBaseAbility(DefaultAbilities.TOO_MANY_PLANTS).isToggled()) {
          if(herbalism.getStringList("TooManyPlantsBlocks").contains(block.getType().toString())) {
            TooManyPlants tooManyPlants = (TooManyPlants) mp.getBaseAbility(DefaultAbilities.TOO_MANY_PLANTS);
            Parser parser = DefaultAbilities.TOO_MANY_PLANTS.getActivationEquation();
            parser.setVariable("herbalism_level", mp.getSkill(Skills.HERBALISM).getCurrentLevel());
            parser.setVariable("power_level", mp.getPowerLevel());
            int chance = (int) parser.getValue() * 1000;
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val) {
              TooManyPlantsEvent tooManyPlantsEvent = new TooManyPlantsEvent(mp, tooManyPlants, block.getType());
              Bukkit.getPluginManager().callEvent(tooManyPlantsEvent);
              if(!tooManyPlantsEvent.isCancelled()) {
                dropMultiplier = 2;
              }
            }
          }
        }
        if(McRPG.getPlaceStore().isTrue(block)) {
          dropMultiplier = 1;
        }
        Material type = block.getType();
        if(UnlockedAbilities.REPLANTING.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.REPLANTING) && mp.getBaseAbility(UnlockedAbilities.REPLANTING).isToggled()) {
          Replanting replanting = (Replanting) mp.getBaseAbility(UnlockedAbilities.REPLANTING);
          if(!McRPG.getPlaceStore().isTrue(block) && CropType.isCrop(block.getType()) || CropType.isCropSeed(block.getType())) {
            {
              int chance = (int) herbalism.getDouble("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".ActivationChance") * 1000;
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val) {
                int growChance = (int) herbalism.getDouble("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".StageGrowthChance") * 1000;
                int maxAge = herbalism.getInt("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".MaxGrowthLevel");
                int minAge = herbalism.getInt("ReplantingConfig.Tier" + Methods.convertToNumeral(replanting.getCurrentTier()) + ".MinGrowthLevel");
                boolean grow = false;
                if(growChance >= rand.nextInt(100000)) {
                  grow = true;
                }
                ReplantingEvent replantingEvent = new ReplantingEvent(mp, replanting, grow, maxAge, minAge);
                Bukkit.getPluginManager().callEvent(replantingEvent);
                if(!replantingEvent.isCancelled()) {
                  block.setType(type);
                  Ageable ageable = (Ageable) block.getBlockData();
                  ageable.setAge(0);
                  if(replantingEvent.isDoStageGrowth()) {
                    int tier = rand.nextInt(replantingEvent.getMaxAge() - replantingEvent.getMinAge() + 1);
                    ageable.setAge(tier);
                  }
                }
              }
            }
          }
        }
        if(UnlockedAbilities.DIAMOND_FLOWERS.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.DIAMOND_FLOWERS) && mp.getBaseAbility(UnlockedAbilities.DIAMOND_FLOWERS).isToggled()) {
          DiamondFlowers diamondFlowers = (DiamondFlowers) mp.getBaseAbility(UnlockedAbilities.DIAMOND_FLOWERS);
          if(!McRPG.getPlaceStore().isTrue(block)) {
            if(DiamondFlowersData.getDiamondFlowersData().containsKey(type)) {
              ArrayList<String> categoriesToChooseFrom = new ArrayList<>();
              Random rand = new Random();
              String key = "DiamondFlowersConfig.Tier" + Methods.convertToNumeral(diamondFlowers.getCurrentTier()) + ".Categories";
              for(String s : herbalism.getConfigurationSection(key).getKeys(false)) {
                int chance = (int) herbalism.getDouble(key + "." + s) * 1000;
                int val = rand.nextInt(100000);
                if(chance >= val) {
                  categoriesToChooseFrom.add(s);
                }
              }
              if(!categoriesToChooseFrom.isEmpty()) {
                int index = rand.nextInt(categoriesToChooseFrom.size());
                String catToUse = categoriesToChooseFrom.get(index);
                ArrayList<DiamondFlowersData.DiamondFlowersItem> itemsPossible = new ArrayList<>();
                while(itemsPossible.isEmpty()) {
                  for(DiamondFlowersData.DiamondFlowersItem diamondFlowersItem : DiamondFlowersData.getDiamondFlowersData().get(type).get(catToUse)) {
                    int chance = (int) diamondFlowersItem.getDropChance() * 1000;
                    int val = rand.nextInt(100000);
                    if(chance >= val) {
                      itemsPossible.add(diamondFlowersItem);
                    }
                  }
                }
                DiamondFlowersData.DiamondFlowersItem diamondFlowersItem = itemsPossible.get(rand.nextInt(itemsPossible.size()));
                DiamondFlowersEvent diamondFlowersEvent = new DiamondFlowersEvent(mp, diamondFlowers, diamondFlowersItem);
                Bukkit.getPluginManager().callEvent(diamondFlowersEvent);
                if(!diamondFlowersEvent.isCancelled()) {
                  mp.getSkill(Skills.HERBALISM).giveExp(diamondFlowersEvent.getExp(), GainReason.BONUS);
                  int range = diamondFlowersItem.getMaxAmount() - diamondFlowersItem.getMinAmount();
                  int bonusAmount = rand.nextInt((range == 0) ? 1 : range);
                  ItemStack itemToDrop = new ItemStack(diamondFlowersEvent.getMaterial(), diamondFlowersEvent.getMinAmount() + bonusAmount);
                  p.getLocation().getWorld().dropItemNaturally(block.getLocation(), itemToDrop);
                  p.getLocation().getWorld().spawnParticle(Particle.HEART, p.getLocation(), 10);
                }
              }
            }
          }
        }
        DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
      }
      //Deal with mining
      if(mining.getBoolean("MiningEnabled")) {
        int dropMultiplier = 1;

        if(!McRPG.getPlaceStore().isTrue(block)) {
          if(p.getItemInHand().getType().toString().contains("PICK") && mining.contains("ExpAwardedPerBlock." + block.getType().toString())) {
            int expWorth = mining.getInt("ExpAwardedPerBlock." + block.getType().toString());
            mp.giveExp(Skills.MINING, expWorth, GainReason.BREAK);
          }
          boolean incDrops = mining.getStringList("DoubleDropBlocks").contains(block.getType().toString());
          if(DefaultAbilities.DOUBLE_DROP.isEnabled() && mp.getSkill(Skills.MINING).getAbility(DefaultAbilities.DOUBLE_DROP).isToggled()) {
            DoubleDrop doubleDrop = (DoubleDrop) mp.getSkill(Skills.MINING).getAbility(DefaultAbilities.DOUBLE_DROP);
            double boost = 0;
            if(UnlockedAbilities.RICHER_ORES.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.RICHER_ORES)
                    && mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.RICHER_ORES).isToggled()) {
              RicherOres richerOres = (RicherOres) mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.RICHER_ORES);
              RicherOresEvent richerOresEvent = new RicherOresEvent(mp, richerOres);
              Bukkit.getPluginManager().callEvent(richerOresEvent);
              if(!richerOresEvent.isCancelled()) {
                boost = mining.getDouble("RicherOresConfig.Tier" + Methods.convertToNumeral(richerOres.getCurrentTier()) + ".ActivationBoost");
              }
            }

            Parser parser = DefaultAbilities.DOUBLE_DROP.getActivationEquation();
            parser.setVariable("mining_level", mp.getSkill(Skills.MINING).getCurrentLevel());
            parser.setVariable("power_level", mp.getPowerLevel());
            double chance = (parser.getValue() + doubleDrop.getBonusChance() + boost) * 1000;
            if(incDrops) {
              Random rand = new Random();
              int val = rand.nextInt(100000);
              if(chance >= val) {
                DoubleDropEvent doubleDropEvent = new DoubleDropEvent(mp, doubleDrop);
                Bukkit.getPluginManager().callEvent(doubleDropEvent);
                if(!doubleDropEvent.isCancelled()) {
                  dropMultiplier = 2;
                }
              }
            }
          }

          if(incDrops && UnlockedAbilities.ITS_A_TRIPLE.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.ITS_A_TRIPLE)
                  && mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.ITS_A_TRIPLE).isToggled()) {
            ItsATriple itsATriple = (ItsATriple) mp.getSkill(Skills.MINING).getAbility(UnlockedAbilities.ITS_A_TRIPLE);
            double chance = (double) mining.getDouble("ItsATripleConfig.Tier" + Methods.convertToNumeral(itsATriple.getCurrentTier()) + ".ActivationChance") * 1000;
            Random rand = new Random();
            int val = rand.nextInt(100000);
            if(chance >= val) {
              ItsATripleEvent itsATripleEvent = new ItsATripleEvent(mp, itsATriple);
              Bukkit.getPluginManager().callEvent(itsATripleEvent);
              if(!itsATripleEvent.isCancelled()) {
                dropMultiplier = 3;
              }
            }
          }
        }

        if(McRPG.getPlaceStore().isTrue(block)) {
          dropMultiplier = 1;
        }
        //Check if the block is tracked by remote transfer
        if(block.getType() == Material.CHEST && RemoteTransferTracker.isTracked(event.getBlock().getLocation())) {
          UUID uuid = RemoteTransferTracker.getUUID(event.getBlock().getLocation());
          if(p.getUniqueId().equals(uuid)) {
            if(!McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getBoolean("RemoteTransferConfig.UnlinkAndBreakOnMine")) {
              event.setCancelled(true);
            }
            RemoteTransfer remoteTransfer = (RemoteTransfer) mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER);
            remoteTransfer.setLinkedChestLocation(null);
            mp.setLinkedToRemoteTransfer(false);
            RemoteTransferTracker.removeLocation(uuid);
            p.sendMessage(Methods.color(McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.Unlinked")));
          }
          else {
            //TODO will need to add support for admins to remove these such as towny mayors
            if(p.hasPermission("mcadmin.*") || p.hasPermission("mcadmin.unlink")) {
              McRPGPlayer target;
              OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
              if(offlinePlayer.isOnline()) {
                target = PlayerManager.getPlayer(uuid);
                target.getPlayer().sendMessage(Methods.color(McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.AdminUnlinked")));
              }
              else {
                target = new McRPGPlayer(uuid);
              }
              target.setLinkedToRemoteTransfer(false);
              ((RemoteTransfer) target.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER)).setLinkedChestLocation(null);
              RemoteTransferTracker.removeLocation(uuid);
              if(!McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getBoolean("RemoteTransferConfig.UnlinkAndBreakOnMine")) {
                event.setCancelled(true);
              }
            }
            else {
              event.setCancelled(true);
              p.sendMessage(Methods.color(McRPG.getInstance().getPluginPrefix() + McRPG.getInstance().getLangFile().getString("Messages.Abilities.RemoteTransfer.IsLinked")
                      .replace("%Player%", Bukkit.getOfflinePlayer(uuid).getName())));
              return;
            }
          }
        }

        else if(UnlockedAbilities.REMOTE_TRANSFER.isEnabled() && mp.getAbilityLoadout().contains(UnlockedAbilities.REMOTE_TRANSFER)
                && mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER).isToggled() && mp.isLinkedToRemoteTransfer()) {
          RemoteTransfer transfer = (RemoteTransfer) mp.getBaseAbility(UnlockedAbilities.REMOTE_TRANSFER);
          int tier = transfer.getCurrentTier();
          int range = McRPG.getInstance().getFileManager().getFile(FileManager.Files.MINING_CONFIG).getInt("RemoteTransferConfig.Tier" + Methods.convertToNumeral(tier) + ".Range");
          if((block.getLocation().distance(transfer.getLinkedChestLocation()) <= range)) {
            DropItemEvent.getBlocksToRemoteTransfer().put(block.getLocation(), p.getUniqueId());
          }
        }
        DropItemEvent.getBlockDropsToMultiplier().put(block.getLocation(), dropMultiplier);
      }
    }
  }

  private enum EasterEgg {
    VERUM("HOE");

    @Getter
    private String socialStatus;

    EasterEgg(String socialStatus) {
      this.socialStatus = socialStatus;
    }
  }

  //Code from sogonda
// https://gitlab.com/Songoda/EpicFarming/blob/master/EpicFarming-Plugin/src/main/java/com/songoda/epicfarming/utils/CropType.java
  public enum CropType {

    WHEAT("Wheat", Material.WHEAT, Material.WHEAT, Material.WHEAT_SEEDS),

    CARROT("Carrot", Material.CARROTS, Material.CARROT, Material.CARROT),

    POTATO("Potato", Material.POTATOES, Material.POTATO, Material.POTATO),

    BEETROOT("Beetroot", Material.BEETROOTS, Material.BEETROOT, Material.BEETROOT_SEEDS),

    WATER_MELON_STEM("Watermelon", Material.MELON_STEM, Material.MELON, Material.MELON_SEEDS),

    PUMPKIN_STEM("Pumpkin", Material.PUMPKIN_STEM, Material.PUMPKIN, Material.PUMPKIN_SEEDS),

    NETHER_WARTS("Nether Wart", Material.NETHER_WART_BLOCK, Material.NETHER_WART, Material.NETHER_WART);

    private final String name;
    private final Material yieldMaterial, blockMaterial, seedMaterial;

    CropType(String name, Material blockMaterial, Material yieldMaterial, Material seedMaterial) {
      this.name = name;
      this.blockMaterial = blockMaterial;
      this.seedMaterial = seedMaterial;
      this.yieldMaterial = yieldMaterial;
    }


    /**
     * Get the friendly name of the crop
     *
     * @return the name of the crop
     */
    public String getName() {
      return name;
    }

    /**
     * Get the blockMaterial that represents this crop type
     *
     * @return the represented blockMaterial
     */
    public Material getBlockMaterial() {
      return blockMaterial;
    }

    /**
     * Get the yield Material that represents this crop type
     *
     * @return the represented yieldMaterial
     */
    public Material getYieldMaterial() {
      return yieldMaterial;
    }

    /**
     * Get the blockMaterial that represents the seed item for this crop type
     *
     * @return the represented seed blockMaterial
     */
    public Material getSeedMaterial() {
      return seedMaterial;
    }

    /**
     * Check whether a specific blockMaterial is an enumerated crop type or not
     *
     * @param material the blockMaterial to check
     * @return true if it is a crop, false otherwise
     */
    public static boolean isCrop(Material material) {
      for(CropType type : values())
        if(type.getBlockMaterial() == material) return true;
      return false;
    }

    /**
     * Check whether a specific blockMaterial is an enumerated crop type seed or not
     *
     * @param material the blockMaterial to check
     * @return true if it is a seed, false otherwise
     */
    public static boolean isCropSeed(Material material) {
      for(CropType type : values())
        if(type.getSeedMaterial() == material) return true;
      return false;
    }

    /**
     * Get the crop type based on the specified blockMaterial
     *
     * @param material the crop blockMaterial
     * @return the respective CropType. null if none found
     */
    public static CropType getCropType(Material material) {
      for(CropType type : values())
        if(type.getBlockMaterial() == material) return type;
      return null;
    }

  }

  private ArrayList<ItemStack> getCropDrops(Material material) {
    Random random = new Random();

    CropType cropTypeData = CropType.getCropType(material);

    if(material == null || cropTypeData == null) return new ArrayList<>();
    Material yieldMat = cropTypeData.getYieldMaterial();
    ItemStack stack = new ItemStack(cropTypeData.getYieldMaterial(), (yieldMat != Material.WHEAT && yieldMat != Material.PUMPKIN) ? random.nextInt(2) + 2 : 1);
    ItemStack seedStack = new ItemStack(cropTypeData.getSeedMaterial(), random.nextInt(3) + 1);
    return new ArrayList<>(Arrays.asList(stack, seedStack));
  }

}
