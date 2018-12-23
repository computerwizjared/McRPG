package us.eunoians.mcrpg.gui;


import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.BaseAbility;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.players.McRPGPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbilityOverrideGUI extends GUI {

  @Getter
  private BaseAbility abiltyToReplace;
  @Getter
  private BaseAbility replaceAbility;

  private GUIInventoryFunction buildGUIFunction;

  public AbilityOverrideGUI(McRPGPlayer player, BaseAbility abiltyToReplace, BaseAbility replaceAbility){
	super(new GUIBuilder(player));
	this.abiltyToReplace = abiltyToReplace;
	this.replaceAbility = replaceAbility;
	buildGUIFunction = (GUIBuilder builder) -> {
	  FileConfiguration config = McRPG.getInstance().getFileManager().getFile(FileManager.Files.fromString(abiltyToReplace.getGenericAbility().getSkill()));
	  Inventory inv = Bukkit.createInventory(null, 27,
		  Methods.color("&eReplace &5" + abiltyToReplace.getGenericAbility().getName() + " &ewith &5" + replaceAbility.getGenericAbility().getName()));
	  ArrayList<GUIItem> items = new ArrayList<>();

	  ItemStack confirmItem = new ItemStack(Material.LIME_CONCRETE, 1);
	  ItemMeta confirmMeta = confirmItem.getItemMeta();
	  confirmMeta.setDisplayName(Methods.color("&aAccept this ability"));
	  confirmItem.setItemMeta(confirmMeta);
	  items.add(new GUIItem(confirmItem, 10));

	  ItemStack denyItem = new ItemStack(Material.RED_CONCRETE, 1);
	  ItemMeta denyMeta = denyItem.getItemMeta();
	  denyMeta.setDisplayName(Methods.color("&cDeny this ability"));
	  denyMeta.setLore(Methods.colorLore(Arrays.asList("&cYou can replace an old ability with this one later")));
	  denyItem.setItemMeta(denyMeta);
	  items.add(new GUIItem(denyItem, 16));

	  String path = abiltyToReplace.getGenericAbility().getName() + "Config.Item.";
	  ItemStack abilityItem = new ItemStack(Material.getMaterial(config.getString(path + "Material")),
		  config.getInt(path + "Amount"));
	  ItemMeta abilityMeta = abilityItem.getItemMeta();
	  abilityMeta.setDisplayName(Methods.color(config.getString(path + "DisplayName")));
	  List<String> lore = config.getStringList(path + "MenuLore");
	  lore.add(0, "&eAbility to be replaced");
	  abilityMeta.setLore(Methods.colorLore(lore));
	  abilityItem.setItemMeta(abilityMeta);
	  items.add(new GUIItem(abilityItem, 4));

	  String path1 = replaceAbility.getGenericAbility().getName() + "Config.Item.";
	  ItemStack abilityItem1 = new ItemStack(Material.getMaterial(config.getString(path1 + "Material")),
		  config.getInt(path1 + "Amount"));
	  ItemMeta abilityMeta1 = abilityItem1.getItemMeta();
	  abilityMeta1.setDisplayName(Methods.color(config.getString(path1 + "DisplayName")));
	  List<String> lore1 = config.getStringList(path1 + "MenuLore");
	  lore1.add(0, "&eAbility to be replace with");
	  abilityMeta1.setLore(Methods.colorLore(lore1));
	  abilityItem1.setItemMeta(abilityMeta1);
	  items.add(new GUIItem(abilityItem1, 22));

	  ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
	  ItemMeta fillerMeta = filler.getItemMeta();
	  fillerMeta.setDisplayName(" ");
	  filler.setItemMeta(fillerMeta);
	  inv = Methods.fillInventory(inv, filler, items);
	  return inv;
	};
	this.getGui().setBuildGUIFunction(buildGUIFunction);
	this.getGui().rebuildGUI();
  }
}