package com.barcrawl;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import java.awt.*;

@ConfigGroup("barcrawl")
public interface BarcrawlConfig extends Config
{
	@ConfigItem(
			keyName = "chatMessage",
			name = "Chat Message",
			description = "Show unlock messages in the chat. (Uses 'Game Message' Chat Color')",
			position = 0
	)
	default boolean chatMessage() {
		return true;
	}

	@ConfigItem(
			keyName = "overHead",
			name = "Overhead Message",
			description = "Show unlock messages overhead",
			position = 1
	)
	default boolean overhead() {
		return true;
	}

	@ConfigItem(
		keyName = "overHeadColor",
		name = "Overhead Color",
		description = "Overhead message color",
		position = 5
	)
	default Color overheadColor() {
		return null;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(
			keyName = "sidebarIcon",
			name = "Sidebar Icon Position",
			description = "Position on the sidebar",
			position = 5
	)
	default int sidebarIcon() {
		return 2;
	}
}
