/*
 * Copyright (c) 2017, Tyler Hardy <https://github.com/tylerthardy>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tylerthardy;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import com.google.inject.Binder;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.AnimationChanged;
import net.runelite.client.events.GameStateChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientUI;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.Toolkit;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static net.runelite.api.AnimationID.*;

@PluginDescriptor(
	name = "Idle beeper plugin"
)
public class WarningBeeperPlugin extends Plugin
{
	@Inject
	@Nullable
	Client client;

	@Inject
	RuneLite runelite;

	@Inject
	ClientUI gui;


	@Provides
	WarningBeeperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WarningBeeperConfig.class);
	}

	@Inject
	WarningBeeperConfig config;

	Player local;

	private boolean animStarted = false;
	private Instant animTime;
	private Point animPosition;

	private boolean notifyHitpoints = false;

	private boolean beeping = false;
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!config.isEnabled() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != event.getActor())
		{
			return;
		}

		int animation = localPlayer.getAnimation();
		switch (animation)
		{
			/* Woodcutting */
			case WOODCUTTING_BRONZE:
			case WOODCUTTING_IRON:
			case WOODCUTTING_STEEL:
			case WOODCUTTING_BLACK:
			case WOODCUTTING_MITHRIL:
			case WOODCUTTING_ADAMANT:
			case WOODCUTTING_RUNE:
			case WOODCUTTING_DRAGON:
			/* Cooking(Fire, Range) */
			case COOKING_FIRE:
			case COOKING_RANGE:
			/* Crafting(Gem Cutting, Glassblowing */
			case GEM_CUTTING_OPAL:
			case GEM_CUTTING_JADE:
			case GEM_CUTTING_REDTOPAZ:
			case GEM_CUTTING_SAPPHIRE:
			case GEM_CUTTING_EMERALD:
			case GEM_CUTTING_RUBY:
			case GEM_CUTTING_DIAMOND:
			case CRAFTING_GLASSBLOWING:
			/* Fletching(Cutting, Stringing) */
			case FLETCHING_BOW_CUTTING:
			case FLETCHING_STRING_NORMAL_SHORTBOW:
			case FLETCHING_STRING_OAK_SHORTBOW:
			case FLETCHING_STRING_WILLOW_SHORTBOW:
			case FLETCHING_STRING_MAPLE_SHORTBOW:
			case FLETCHING_STRING_YEW_SHORTBOW:
			case FLETCHING_STRING_MAGIC_SHORTBOW:
			case FLETCHING_STRING_NORMAL_LONGBOW:
			case FLETCHING_STRING_OAK_LONGBOW:
			case FLETCHING_STRING_WILLOW_LONGBOW:
			case FLETCHING_STRING_MAPLE_LONGBOW:
			case FLETCHING_STRING_YEW_LONGBOW:
			case FLETCHING_STRING_MAGIC_LONGBOW:
			/* Smithing(Anvil, Furnace, Cannonballs */
			case SMITHING_ANVIL:
			case SMITHING_SMELTING:
			case SMITHING_CANNONBALL:
			/* Fishing */
			case FISHING_NET:
			case FISHING_HARPOON:
			case FISHING_CAGE:
			case FISHING_POLE_CAST:
			/* Mining(Normal) */
			case MINING_BRONZE_PICKAXE:
			case MINING_IRON_PICKAXE:
			case MINING_STEEL_PICKAXE:
			case MINING_BLACK_PICKAXE:
			case MINING_MITHRIL_PICKAXE:
			case MINING_ADAMANT_PICKAXE:
			case MINING_RUNE_PICKAXE:
			case MINING_DRAGON_PICKAXE:
			/* Mining(Motherlode) */
			case MINING_MOTHERLODE_BRONZE:
			case MINING_MOTHERLODE_IRON:
			case MINING_MOTHERLODE_STEEL:
			case MINING_MOTHERLODE_BLACK:
			case MINING_MOTHERLODE_MITHRIL:
			case MINING_MOTHERLODE_ADAMANT:
			case MINING_MOTHERLODE_RUNE:
			case MINING_MOTHERLODE_DRAGON:
			/* Herblore */
			case HERBLORE_POTIONMAKING:
			/* Magic */
			case MAGIC_CHARGING_ORBS:
				animStarted = true;
				animTime = Instant.now();
				animPosition = local.getWorldLocation();
				break;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		//lastInteracting = null;
	}

	@Schedule(
		period = 1,
		unit = ChronoUnit.MILLIS
	)
	public void checkIdle()
	{
		if (!config.isEnabled() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (shouldBeep())
		{
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private boolean shouldBeep()
	{
		if (local == null)
		{
			local = client.getLocalPlayer();
			return false;
		}

		return isSkillIdle() || isHPLow() || isPrayerLow();
	}

	private boolean isSkillIdle()
	{
		Duration waitDuration = Duration.ofMillis(config.getTimeout());
		if (animStarted && local.getAnimation() == IDLE && Instant.now().compareTo(animTime.plus(waitDuration)) >= 0)
		{
			if (local.getWorldLocation().equals(animPosition))
			{
				return true;
			}
			else
			{
				animStarted = false;
			}
		}
		return false;
	}

	private boolean isHPLow()
	{
		return client.getBoostedSkillLevel(Skill.HITPOINTS) <= config.getHitpointsThreshold();
	}

	private boolean isPrayerLow()
	{
		return client.getBoostedSkillLevel(Skill.PRAYER) <= config.getPrayerThreshold();
	}
}
