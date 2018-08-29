package us.eunoians.mcmmox;

import com.cyr1en.mcutils.PluginUpdater;
import com.cyr1en.mcutils.config.ConfigManager;
import com.cyr1en.mcutils.logger.Logger;
import com.cyr1en.mcutils.utils.reflection.Initializable;
import com.cyr1en.mcutils.utils.reflection.annotation.Initialize;
import com.cyr1en.mcutils.utils.reflection.annotation.process.Initializer;
import lombok.Getter;
import lombok.var;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import us.eunoians.mcmmox.commands.McMMOStub;
import us.eunoians.mcmmox.configuration.MConfigManager;
import us.eunoians.mcmmox.configuration.files.GeneralConfig;
import us.eunoians.mcmmox.configuration.files.SwordsConfig;
import us.eunoians.mcmmox.events.vanilla.MoveEvent;
import us.eunoians.mcmmox.localization.Locale;
import us.eunoians.mcmmox.localization.LocalizationFiles;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class Mcmmox extends JavaPlugin implements Initializable {

    private static Mcmmox instance;

    @Getter private MConfigManager mConfigManager;
    @Getter private PluginUpdater pluginUpdater;
    @Getter private LocalizationFiles localizationFiles;
    @Override
    public void onEnable() {

        Bukkit.getScheduler().runTaskLater(this, () -> Initializer.initAll(this), 1L);
        getCommand("mcmmox").setExecutor(new McMMOStub());
        getServer().getPluginManager().registerEvents(new MoveEvent(), this.getInstance());
    }

    @Override
    public void onDisable() {
        if (!Initializer.finished())
            Initializer.interrupt();
    }

    @Initialize(priority = 0)
    private void preInit() {
        var configManager = new ConfigManager(this);
        mConfigManager = new MConfigManager(configManager);
        if (!mConfigManager.setupConfigs(
                GeneralConfig.class, SwordsConfig.class))
            getServer().shutdown();
        Logger.setDebugMode(mConfigManager.getGeneralConfig().isDebugMode());
        Locale.init(mConfigManager);
    }

    @Initialize(priority = 1)
    private void sanity() {
        if (ProxySelector.getDefault() == null) {
            ProxySelector.setDefault(new ProxySelector() {
                private final List<Proxy> DIRECT_CONNECTION = Collections.unmodifiableList(Collections.singletonList(Proxy.NO_PROXY));
                public void connectFailed(URI arg0, SocketAddress arg1, IOException arg2) {}
                public List<Proxy> select(URI uri) { return DIRECT_CONNECTION; }
            });
        }
        pluginUpdater = new PluginUpdater(this, "https://contents.cyr1en.com/mcmmox/plinfo");
        pluginUpdater.setOut(true);
        if(mConfigManager.getGeneralConfig().isAutoUpdate()) {
            if(pluginUpdater.needsUpdate())
                pluginUpdater.update();
            else
                Logger.info("No updates were found!");
        } else {
            Logger.info("New version of McMMOX is available: " + pluginUpdater.getVersion());
            Logger.info("Click to download new version: " + pluginUpdater.getDownloadURL());
        }
    }

    @Initialize(priority = 2)
    private void initPrimaryInstance() {
        localizationFiles = new LocalizationFiles(this, true);
        instance = this;
    }

    public static Mcmmox getInstance() {
        if(instance == null)
            return new Mcmmox();
        return instance;
    }

    public GeneralConfig getGeneralConfig() {
        return mConfigManager.getGeneralConfig();
    }

    public SwordsConfig getSwordsConfig(){ return mConfigManager.getSwordsConfig();}
}