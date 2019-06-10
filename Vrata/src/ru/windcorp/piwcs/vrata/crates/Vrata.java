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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import ru.windcorp.piwcs.nestedcmd.NCComplaintException;
import ru.windcorp.piwcs.vrata.VrataTemplates;
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

	public static Crate importContainer(Inventory inv) throws VrataOperationException {
		Crate crate;
		
		if (inv.getType() == InventoryType.CRAFTING) {
			throw new IllegalStateException("Inventory is CRAFTING");
		}
		
		try {
			ByteArrayOutputStream nbtData = new ByteArrayOutputStream();
			DataOutput nbtSink = new DataOutputStream(nbtData);
			
			int slots = 0;
			StringBuilder description = new StringBuilder();
			
			for (ItemStack stack : inv) {
				if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) {
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
			crate = Crate.createNew(nbtData.toByteArray(), slots, description.toString());
		} catch (VrataOperationException e) {
			throw e;
		} catch (Exception e) {
			throw new VrataOperationException("Could not pack a crate", e);
		}
		
		return crate;
	}
	
	public static void exportContainer(Inventory inv, Crate crate) throws VrataOperationException, NCComplaintException {
		if (crate.isDeployed()) {
			throw new IllegalStateException("Crate " + crate + " has been deployed already");
		}
		
		if (inv.getType() == InventoryType.CRAFTING) {
			throw new IllegalStateException("Inventory is CRAFTING");
		}
		
		{
			int available = 0;
			for (int slot  = 0; slot < inv.getSize(); ++slot) {
				ItemStack stack = inv.getItem(slot);
				if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) {
					available++;
				}
			}
			
			if (available < crate.getSlots()) {
				throw new NCComplaintException(VrataTemplates.getf("cmd.deploy.problem.notEnoughSpace", crate.getSlots(), available));
			}
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
				
				inv.addItem(stack);
			}
			
			crate.setDeployed(true);
			
		} catch (VrataOperationException e) {
			throw e;
		} catch (Exception e) {
			throw new VrataOperationException("Could not unpack a crate", e);
		}
	}

	@SuppressWarnings("deprecation")
	private static String getDescriptionFor(ItemStack stack) {
		return stack.getTypeId() + ":" + stack.getDurability() + " " + stack.toString().substring("ItemStack".length());
	}
	
	public static String describeInventory(Inventory inv, Player backupData) {
		InventoryHolder holder = inv.getHolder();
		if (holder == null) {
			Location loc = backupData.getLocation();
			return "(no data, around " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ")";
		} else if (holder instanceof BlockState) {
			BlockState blockState = (BlockState) holder;
			return "Block[" + blockState.getData() + "@" + blockState.getX() + " " + blockState.getY() + " " + blockState.getZ() + "]";
		} else if (holder instanceof Entity) {
			Entity entity = (Entity) holder;
			Location loc = entity.getLocation();
			return "Entity[" + entity.getType() + ":" + entity.getEntityId()+ "@" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "]";
		} else {
			Location loc = backupData.getLocation();
			return "Unknown[" + holder + "] around " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
		}
	}
	
	public static boolean isAContainer(Inventory inv) {
		return true;
	}

}
