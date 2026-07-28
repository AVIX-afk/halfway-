package com.halfwaythere;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("halfwaythere")
public interface HalfwayThereConfig extends Config
{
	@ConfigItem(
		keyName = "showChatMessage",
		name = "Show chat message",
		description = "Show a game message when you gain a virtual Halfway level",
		position = 1
	)
	default boolean showChatMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show a panel listing virtual levels for any skill at level 92+",
		position = 2
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "playSound",
		name = "Play level-up sound",
		description = "Play a level up sound effect on a virtual level up",
		position = 3
	)
	default boolean playSound()
	{
		return false;
	}
}
