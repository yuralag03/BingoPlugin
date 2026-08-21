package ru.vgtu.bingo;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import ru.vgtu.bingo.model.CardTemplate;
import ru.vgtu.bingo.model.Goal;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages configuration loading including card templates.
 */
public class ConfigManager {

    private final BingoPlugin plugin;
    private final List<CardTemplate> cardTemplates = new ArrayList<>();

    public ConfigManager(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all card templates from config.yml.
     */
    public void loadCardTemplates() {
        cardTemplates.clear();
        
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("card-templates");
        if (templatesSection == null) {
            plugin.getLogger().severe("No card-templates section found in config.yml!");
            return;
        }

        for (String key : templatesSection.getKeys(false)) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(key);
            if (templateSection == null) continue;

            int id = templateSection.getInt("id", Integer.parseInt(key));
            List<Goal> goals = new ArrayList<>();

            ConfigurationSection goalsSection = templateSection.getConfigurationSection("goals");
            if (goalsSection == null) continue;

            for (String goalKey : goalsSection.getKeys(false)) {
                ConfigurationSection goalSection = goalsSection.getConfigurationSection(goalKey);
                if (goalSection == null) continue;

                String materialName = goalSection.getString("material");
                int amount = goalSection.getInt("amount", 1);
                int tier = goalSection.getInt("tier", 1);

                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    goals.add(new Goal(material, amount, tier));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material '" + materialName + "' in card template " + id);
                }
            }

            if (goals.size() != 16) {
                plugin.getLogger().warning("Card template " + id + " has " + goals.size() + " goals instead of 16!");
            } else {
                cardTemplates.add(new CardTemplate(id, goals));
                plugin.getLogger().info("Loaded card template " + id + " with " + goals.size() + " goals");
            }
        }

        plugin.getLogger().info("Total card templates loaded: " + cardTemplates.size());
    }

    public List<CardTemplate> getCardTemplates() {
        return cardTemplates;
    }
}
