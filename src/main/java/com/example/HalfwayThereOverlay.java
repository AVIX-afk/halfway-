package com.halfwaythere;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class HalfwayThereOverlay extends OverlayPanel
{
	private final HalfwayTherePlugin plugin;
	private final HalfwayThereConfig config;

	@Inject
	private HalfwayThereOverlay(HalfwayTherePlugin plugin, HalfwayThereConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		Map<Skill, Integer> virtualLevels = plugin.getVirtualLevels();
		if (virtualLevels.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Halfway There")
			.color(HalfwayTherePlugin.OVERLAY_TITLE_COLOR)
			.build());

		for (Map.Entry<Skill, Integer> entry : virtualLevels.entrySet())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(entry.getKey().getName())
				.right(String.valueOf(entry.getValue()))
				.build());
		}

		return super.render(graphics);
	}
}
