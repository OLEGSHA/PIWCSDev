/*
 * PIWCS Vrata Plugin
 * Copyright (C) 2019  PIWCS Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.windcorp.piwcs.vrata;

import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ru.windcorp.piwcs.vrata.cmd.VrataCommandHandler;
import ru.windcorp.piwcs.vrata.cmd.VrataListener;
import ru.windcorp.piwcs.vrata.crates.Packages;
import ru.windcorp.piwcs.vrata.users.VrataUsers;

public class VrataPlugin extends JavaPlugin {
	
	private static VrataPlugin inst;
	
	public static VrataPlugin getInst() {
		if (inst == null) {
			throw new IllegalStateException("Plugin not initialized yet");
		}
		return inst;
	}
	
	public static Path getDataPath(String name) {
		return getInst().getDataFolder().toPath().resolve(name);
	}
	
	@Override
	public void onLoad() {
		inst = this;
	}
	
	@Override
	public void onEnable() {
		try {
			VrataLogger.setup();
		} catch (Exception e) {
			e.printStackTrace();
			disable("Could not setup log");
			return;
		}
		
		try {
			VrataTemplates.load();
		} catch (Exception e) {
			e.printStackTrace();
			disable("Could not load message templates");
			return;
		}
		
		try {
			VrataUsers.load();
		} catch (Exception e) {
			e.printStackTrace();
			disable("Could not load user database");
			return;
		}
		
		try {
			Packages.load();
		} catch (IOException e) {
			e.printStackTrace();
			disable("Could not load packages");
			return;
		}
		
		Bukkit.getPluginManager().registerEvents(new VrataListener(), this);
		getCommand("vrata").setExecutor(new VrataCommandHandler());
	}
	
	@Override
	public void onDisable() {
		VrataListener.onStopping();
		save();
		VrataLogger.terminate();
	}
	
	public static void save() {
		try {
			VrataUsers.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Packages.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void disable(String message) {
		inst.getLogger().severe(message);
		inst.getLogger().severe("Plugin will be disabled");
		Bukkit.getPluginManager().disablePlugin(inst);
	}

}
