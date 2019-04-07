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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;

import ru.windcorp.piwcs.vrata.crates.VrataAstralInterface;

public class VrataAstralMedium implements VrataAstralInterface {
	
	@Override
	public void writeItemStack(org.bukkit.inventory.ItemStack itemStack, DataOutput output) throws IOException {
//		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		dh             nbtTagCompound = new dh();
		
//		CraftItemStack.asNMSCopy(itemStack).writeToNBT(nbtTagCompound);
		CraftItemStack.asNMSCopy(itemStack).b         (nbtTagCompound);
		
//		nbtTagCompound.write(output);
		nbtTagCompound.a    (output);
	}
	
	@Override
	public org.bukkit.inventory.ItemStack readItemStack(DataInput input) {
//		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		dh             nbtTagCompound = new dh();
		
		// 0 means we are not recursing (yet)
//		nbtTagCompound.read(input, 0, NBTSizeTracker.DO_NOT_TRACK);
		nbtTagCompound.a   (input, 0, ds            .a           );
		
//		return CraftItemStack.asBukkitCopy(net.minecraft.inventory.ItemStack.loadItemStackFromNBT(nbtTagCompound));
		return CraftItemStack.asBukkitCopy(add                              .a                   (nbtTagCompound));
	}

}
