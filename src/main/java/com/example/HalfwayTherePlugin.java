package com.halfwaythere;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Halfway There
 *
 * Level 92 sits at roughly the halfway point (by XP) between level 1 and level 99,
 * which is why the 90s can feel like a slog with no level-up hits. This plugin takes
 * your remaining XP progress from 92 -> 99 and rescales it onto the SAME xp curve used
 * for levels 1 -> 92. That means "virtual" leveling from 92-99 feels exactly like
 * leveling did the first time around, just compressed into the back half of the grind,
 * and you land exactly on virtual level 92 the moment you hit real level 99.
 */
@Slf4j
@PluginDescriptor(
	name = "Halfway There",
	description = "Turns levels 92-99 into a virtual 1-92 climb so the grind still feels like leveling up",
	tags = {"skilling", "levels", "xp", "virtual", "motivation", "grind"}
)
public class HalfwayTherePlugin extends Plugin
{
	static final int START_LEVEL = 92;
	static final int END_LEVEL = 99;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private HalfwayThereConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HalfwayThereOverlay overlay;

	@Inject
	private ChatMessageManager chatMessageManager;

	private final Map<Skill, Integer> virtualLevels = new EnumMap<>(Skill.class);

	private int xpAtStart;
	private int xpAtEnd;

	@Provides
	HalfwayThereConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HalfwayThereConfig.class);
	}

	@Override
	protected void startUp()
	{
		xpAtStart = Experience.getXpForLevel(START_LEVEL);
		xpAtEnd = Experience.getXpForLevel(END_LEVEL);

		virtualLevels.clear();
		overlayManager.add(overlay);

		// Seed current virtual levels quietly on startup so we don't fire a wave of
		// "level up" messages for progress the player already had.
		clientThread.invoke(() ->
		{
			for (Skill skill : Skill.values())
			{
				if (skill == Skill.OVERALL)
				{
					continue;
				}

				int realLevel = client.getRealSkillLevel(skill);
				if (realLevel >= START_LEVEL)
				{
					int xp = client.getSkillExperience(skill);
					virtualLevels.put(skill, calculateVirtualLevel(xp));
				}
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		virtualLevels.clear();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		if (skill == Skill.OVERALL)
		{
			return;
		}

		int realLevel = event.getLevel();
		if (realLevel < START_LEVEL)
		{
			// Not into the "halfway" stretch (yet) - nothing to track.
			virtualLevels.remove(skill);
			return;
		}

		int xp = event.getXp();
		int virtualLevel = calculateVirtualLevel(xp);
		Integer previous = virtualLevels.put(skill, virtualLevel);

		if (previous != null && virtualLevel > previous)
		{
			announceLevelUp(skill, virtualLevel, realLevel);
		}
	}

	/**
	 * Rescales xp earned between level 92 and level 99 onto the level 1-92 xp curve.
	 * At xpAtStart (real level 92) this returns 1. At xpAtEnd (real level 99) this
	 * returns exactly 92.
	 */
	int calculateVirtualLevel(int xp)
	{
		int clampedXp = Math.max(xpAtStart, Math.min(xp, xpAtEnd));
		double fraction = (double) (clampedXp - xpAtStart) / (double) (xpAtEnd - xpAtStart);
		int virtualXp = (int) Math.round(fraction * xpAtStart);
		return Experience.getLevelForXp(virtualXp);
	}

	private void announceLevelUp(Skill skill, int virtualLevel, int realLevel)
	{
		if (config.showChatMessage())
		{
			String skillName = skill.getName();
			String text = virtualLevel >= START_LEVEL
				? "Congratulations, your Halfway " + skillName + " has reached level " + virtualLevel
					+ " - you've made it all the way back! (Real level " + realLevel + ")"
				: "Congratulations, you've levelled Halfway " + skillName + " to level " + virtualLevel + ".";

			String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(text)
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(message)
				.build());
		}

		if (config.playSound())
		{
			// NOTE: verify this sound effect id against the RuneLite/OSRS sound table you're
			// building against - client.playSoundEffect(int) takes an OSRS sound effect id,
			// and different RuneLite versions expose different named constants for it.
			client.playSoundEffect(2396);
		}
	}

	Map<Skill, Integer> getVirtualLevels()
	{
		return Collections.unmodifiableMap(virtualLevels);
	}

	static final Color OVERLAY_TITLE_COLOR = Color.ORANGE;
}
