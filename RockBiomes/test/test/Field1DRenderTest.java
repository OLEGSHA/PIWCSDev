/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2019  Javapony/OLEGSHA
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
package test;

import java.awt.image.BufferedImage;
import java.util.function.DoubleUnaryOperator;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ru.windcorp.mineragenesis.rb.gen.Dimension;


/**
 * @author Javapony
 *
 */
public class Field1DRenderTest extends RenderTest {
	
	private DoubleUnaryOperator function = x -> {
		return 0.8 * Dimension.getMultiplier(
				(int) Math.floor(x/800 * 3) - 1,
				15 - ((int) Math.floor(x/800 * 16 * 3) % 16)
		);

//		x = (x - 400) / 100;
//		return 0.1 * (x*x + x + 1) / (x + 1);
	};
	
	@Override
	protected void render(BufferedImage img) {
		try {
			for (int x = 0; x < img.getWidth(); ++x) {
				if ((x & 0xF) == 0) for (int y = 0; y < img.getHeight(); ++y) img.setRGB(x, y, 0x222222);
				else for (int y = 0; y < img.getHeight(); ++y) img.setRGB(x, y, 0x000000);
				
				img.setRGB(x, img.getHeight() / 2, 0x222200);
				
				int y = (int) (-function.applyAsDouble(x) * img.getHeight() / 2.0 + img.getHeight() / 2);
				if (y < 0) {
					img.setRGB(x, 0, 0x2222FF);
				} else if (y > img.getHeight() - 1) {
					img.setRGB(x, img.getHeight() - 1, 0xFF2222);
				} else {
					img.setRGB(x, y, 0xFFFFFF);
				}
			}
		} catch (Exception e) {
			
		}
	}
	
	@Override
	protected void populateControls(JPanel panel) {
		
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Field1DRenderTest());
	}

}
