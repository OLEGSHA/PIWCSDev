package test.preview;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.piwcs.MGAPChunkProcessor;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiome;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiomeType;

public class BiomeRenderer extends AbstractRenderer {
	
	private final Map<RockBiome, Integer> colorMap = new HashMap<>();
	private final DiscreteNoise<RockBiomeType> rbts;
	
	public BiomeRenderer() {
		MGAPChunkProcessor pr = (MGAPChunkProcessor) MineraGenesis.getProcessor();
		rbts = pr.getRegoliths();
		
		int biomes = 0;
		for (RockBiomeType rbt : rbts.getValues()) {
			biomes += rbt.getBiomes().getValues().length;
		}
		
		float i = 0;
		for (RockBiomeType rbt : rbts.getValues()) {
			for (RockBiome rb : rbt.getBiomes().getValues()) {
				colorMap.put(rb, Color.HSBtoRGB(i / biomes, 1, 1));
				++i;
			}
		}
	}

	@Override
	public void render(BufferedImage img, int mult, int xo, int yo) {
		for (int x = 0; x < img.getWidth(); ++x) {
			for (int y = 0; y < img.getHeight(); ++y) {
				if (paintGrid(img, x, y, x*mult + xo, y*mult + yo, 100)) {
					continue;
				}
				
				img.setRGB(x, y, colorMap.get(rbts.getValue(x*mult + xo, y*mult + yo).getBiomes().getValue(x*mult + xo, y*mult + yo)));
			}
		}
	}

	@Override
	public JComponent getComponent() {
		return null;
	}
	
	@Override
	public int getDefaultZoomLevel() {
		return +4;
	}

}
