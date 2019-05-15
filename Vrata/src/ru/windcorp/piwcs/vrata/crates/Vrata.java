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

package ru.windcorp.piwcs.vrata.crates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import ru.windcorp.piwcs.nestedcmd.NCComplaintException;
import ru.windcorp.piwcs.vrata.exceptions.VrataOperationException;

public class Vrata {
	
	public static final VrataAstralInterface ASTRAL;
	
	static {
		VrataAstralInterface tmp = null;
		try {
			tmp = VrataAstralInterface.class.cast(Class.forName("VrataAstralMedium").newInstance());
		} catch (Exception impossible) {}
		
		ASTRAL = tmp;
	}

	public static Crate importContainer(Player player, Package pkg) throws VrataOperationException {
		Crate crate;
		
		if (player.getOpenInventory().getType() == InventoryType.CRAFTING) {
			throw new IllegalStateException("Player " + player + " has no inventory open");
		}
		
		try {
			ByteArrayOutputStream nbtData = new ByteArrayOutputStream();
			DataOutput nbtSink = new DataOutputStream(nbtData);
			
			int slots = 0;
			StringBuilder description = new StringBuilder();
			
			for (ItemStack stack : player.getOpenInventory().getTopInventory()) {
				if (stack.getType() == Material.AIR || stack.getAmount() == 0) {
					continue;
				}
				
				slots++;
				description.append(getDescriptionFor(stack));
				description.append("\n");
				
				try {
					ASTRAL.writeItemStack(stack, nbtSink);
				} catch (Exception e) {
					throw new VrataOperationException("Could not pack a crate: could not write NBT", e);
				}
			}
			
			crate = Crate.createNew(pkg, nbtData.toByteArray(), slots, description.toString());
		} catch (VrataOperationException e) {
			throw e;
		} catch (Exception e) {
			throw new VrataOperationException("Could not pack a crate", e);
		}
		
		player.getOpenInventory().getTopInventory().clear();
		return crate;
	}
	
	public static void exportContainer(Player player, Crate crate) throws VrataOperationException, NCComplaintException {
		if (crate.isDeployed()) {
			throw new IllegalStateException("Crate " + crate + " has been deployed already");
		}
		
		if (player.getOpenInventory().getType() == InventoryType.CRAFTING) {
			throw new IllegalStateException("Player " + player + " has no inventory open");
		}
		
		Inventory inventory = player.getOpenInventory().getTopInventory();
		
		if (inventory.getSize() - inventory.getContents().length < crate.getSlots()) {
			throw new NCComplaintException("Too little space: " + crate.getSlots() +
					" required, " + (inventory.getSize() - inventory.getContents().length) + " available");
		}
		
		try {
			ByteArrayInputStream nbtData = new ByteArrayInputStream(crate.getNbtData());
			DataInput nbtSource = new DataInputStream(nbtData);
			
			for (int i = 0; i < crate.getSlots(); ++i) {
				ItemStack stack = null;
				
				try {
					stack = ASTRAL.readItemStack(nbtSource);
				} catch (Exception e) {
					throw new VrataOperationException("Could not unpack a crate: could not read NBT", e);
				}
				
				inventory.addItem(stack);
			}
			
			crate.setDeployed(true);
			
		} catch (VrataOperationException e) {
			throw e;
		} catch (Exception e) {
			throw new VrataOperationException("Could not unpack a crate", e);
		}
	}

	private static String getDescriptionFor(ItemStack stack) {
		return stack.toString();
	}

}
