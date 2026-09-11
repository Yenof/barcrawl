package com.barcrawl;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "Barcrawl",
		description = "A food and drink collection log, drink up!",
		tags = {"collection", "log", "clog", "food", "drink", "barcrawl", "eat", "unlock", "kalil"}
)

public class BarcrawlPlugin extends Plugin {
	private static final String CONFIG_GROUP = "barCrawl";
	private static final String CONFIG_KEY = "consumedItems";

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BarcrawlConfig config;

	private BarcrawlPanel panel;
	private NavigationButton navButton;

	private final Set<Integer> consumedItems = new HashSet<>();
	private final Set<Integer> allConsumables = new HashSet<>();

	private final Map<Integer, String> itemNames = new HashMap<>();
	private final Set<Integer> foodItems = new HashSet<>();
	private final Set<Integer> drinkItems = new HashSet<>();
	private final Set<String> consumedNames = new HashSet<>();


	private boolean consumablesLoaded;

	@Override
	protected void startUp() throws Exception {

		navBuilder();
		loadCharacterData();
		SwingUtilities.invokeLater(this::updatePanel);
		consumablesLoaded = false;

	}

	@Override
	protected void shutDown() throws Exception {
		if (navButton != null) {
			clientToolbar.removeNavigation(navButton);
		}
	}

	private void navBuilder() {
		panel = new BarcrawlPanel();
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/beericon.png");
		navButton = NavigationButton.builder()
				.tooltip("Barcrawl Extreme")
				.icon(icon)
				.priority(config.sidebarIcon())
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) {
			loadCharacterData();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN) {
			consumablesLoaded = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (consumablesLoaded) {
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		for (int itemId = 0; itemId < client.getItemCount(); itemId++) {
			ItemComposition item = client.getItemDefinition(itemId);

			String[] actions = item.getInventoryActions();

			if (actions == null) {
				continue;
			}

			boolean isFood = false;
			boolean isDrink = false;

			for (String action : actions) {
				if ("Eat".equalsIgnoreCase(action)) {
					isFood = true;
				}

				if ("Drink".equalsIgnoreCase(action)) {
					isDrink = true;
				}
			}

			if (!isFood && !isDrink) {
				continue;
			}

			if (item.getNote() != -1 || item.getPlaceholderTemplateId() != -1) {
				continue;
			}
			allConsumables.add(itemId);
			itemNames.put(itemId, item.getName());

			if (isFood) {
				foodItems.add(itemId);
			}

			if (isDrink) {
				drinkItems.add(itemId);
			}
		}

		for (int itemId : consumedItems) {
			String itemName = itemNames.get(itemId);
			if (itemName != null) {
				consumedNames.add(itemName);
			}
		}

		consumablesLoaded = true;
		updatePanel();

	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (!event.isItemOp()) {
			return;
		}

		String option = event.getMenuOption();

		if (!"Eat".equalsIgnoreCase(option) && !"Drink".equalsIgnoreCase(option))
		{
			return;
		}

		int itemId = event.getItemId();

		if (itemId <= 0) {
			return;
		}

		ItemComposition item = client.getItemDefinition(itemId);

		if (!allConsumables.contains(itemId)) {
			return;
		}
		if (!consumedItems.add(itemId)) {
			return;
		}


		String itemName = item.getName();
		boolean wasAlreadyCollected = !consumedNames.add(itemName);

		int unlocked = panel.getCompletedCount();
		int total = panel.getTotalCount();

		if (!wasAlreadyCollected) {
			unlocked++;
		}

		String consumeType = "Eat".equalsIgnoreCase(option) ? "Food" : "Drink";
		String message = "<col=" + getColorHex(config.overheadColor()) + ">" + consumeType + " collected: " + item.getName() + " (" + unlocked + "/" + total + ")" + "</col>";
		Player player = client.getLocalPlayer();
		if (player != null) {
			if (unlocked == total){
				message = message + "!!!";
			}
			if (config.overhead()){
				player.setOverheadText(message);
				player.setOverheadCycle(360);
			}
			if (config.chatMessage()){
				client.addChatMessage(ChatMessageType.CONSOLE, player.getName(), consumeType + " collected: " + item.getName() + " (" + unlocked + "/" + total + ")", null);
			}
		}
		saveCharacterData(itemId);
		updatePanel();
	}

	private void loadCharacterData() {
		consumedItems.clear();

		String savedItems = configManager.getRSProfileConfiguration(
				CONFIG_GROUP,
				CONFIG_KEY
		);

		if (savedItems != null && !savedItems.isEmpty()) {
			for (String id : savedItems.split(",")) {
				try {
					consumedItems.add(Integer.parseInt(id));
				}
				catch (NumberFormatException ignored) {
				}
			}
		}
		updatePanel();
	}

	private void saveCharacterData(int itemId) {
		String savedItems = configManager.getRSProfileConfiguration(
				CONFIG_GROUP,
				CONFIG_KEY
		);

		String data = savedItems == null || savedItems.isEmpty() ? String.valueOf(itemId) : savedItems + "," + itemId;

		configManager.setRSProfileConfiguration(
				CONFIG_GROUP,
				CONFIG_KEY,
				data
		);
	}

	private void updatePanel() {
		if (panel != null && !allConsumables.isEmpty()) {
			panel.updateList(
					allConsumables,
					consumedItems,
					itemNames,
					foodItems,
					drinkItems
			);
		}
	}

	private String getColorHex(Color color) {
		if (color == null) {
			return null;
		}
		return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	@Provides
	BarcrawlConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(BarcrawlConfig.class);
	}
}
