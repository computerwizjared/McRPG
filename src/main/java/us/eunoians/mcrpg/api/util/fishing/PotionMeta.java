package us.eunoians.mcrpg.api.util.fishing;

import lombok.Getter;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

import static us.eunoians.mcrpg.api.util.fishing.FishingItem.getFishingLootConfig;

public class PotionMeta {

    @Getter private List<PotionSubMeta> effects = new ArrayList<>();
    @Getter private boolean isSplash;

    public PotionMeta(String filePath){
        for(String s : getFishingLootConfig().getStringList(filePath + "Effects")){
            effects.add(new PotionSubMeta(s));
        }
        isSplash = getFishingLootConfig().getBoolean(filePath + "Splash", false);
    }

    private class PotionSubMeta{

        @Getter private PotionEffectType effectType;
        @Getter private int duration;
        @Getter private int level;

        PotionSubMeta(String effect){
            String[] potionData = effect.split(":");
            this.effectType = PotionEffectType.getByName(potionData[0]);
            this.duration = Integer.parseInt(potionData[2]);
            this.level = Integer.parseInt(potionData[1]);
        }
    }
}
