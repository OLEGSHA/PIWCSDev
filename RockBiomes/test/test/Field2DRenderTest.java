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

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ru.windcorp.mineragenesis.noise.SimplexNoiseGenerator;
import ru.windcorp.mineragenesis.rb.fields.SumField2D;
import ru.windcorp.mineragenesis.rb.fields.Field2D;
import ru.windcorp.mineragenesis.rb.fields.Noise2D;

/**
 * @author Javapony
 *
 */
public class Field2DRenderTest extends RenderTest {
	
	private Field2D field = new SumField2D(new Noise2D[] {
			new Noise2D(new SimplexNoiseGenerator(100501), 100, 1, -0.75),
			new Noise2D(new SimplexNoiseGenerator(100500), 75, 0.7, 0),
			new Noise2D(new SimplexNoiseGenerator(100500), 20, 0.1, 0),
			new Noise2D(new SimplexNoiseGenerator(100500), 5, 0.05, 0)
	}, 0, true);
	
	@Override
	protected void render(BufferedImage img) {
		for (int x = 0; x < img.getWidth(); ++x) {
			for (int y = 0; y < img.getHeight(); ++y) {
				double z = field.get(x, y);
				
				if (z < 0) {
					if ((x & 0xF) == 0 || (y & 0xF) == 0) {
						img.setRGB(x, y, 0x222222);
					} else {
						img.setRGB(x, y, 0x000000);
					}
				} else if (z > 1) {
					img.setRGB(x, y, 0xFF0000);
				} else {
					img.setRGB(x, y, gray(z));
				}
			}
		}
	}
	
	private int gray(double z) {
		int comp = 255 - (int) (z * 255);
		return (comp / 2) | (comp << 8) | ((comp / 2) << 16);
	}

	@Override
	protected void populateControls(JPanel panel) {
		
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Field2DRenderTest());
	}

}
