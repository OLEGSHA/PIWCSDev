package test.preview;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.piwcs.MGAPChunkProcessor;
import ru.windcorp.mineragenesis.piwcs.gen.BaseDeposit;
import ru.windcorp.mineragenesis.piwcs.gen.BlockWeightedList;
import ru.windcorp.mineragenesis.piwcs.gen.Deposit;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiome;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiomeType;
import ru.windcorp.mineragenesis.piwcs.gen.SimpliestDeposit;
import test.FakeLoader;

public class SingleDepositRenderer extends AbstractRenderer {

	private final DiscreteNoise<RockBiomeType> rbts;
	
	private final Map<String, BaseDeposit> deposits = new HashMap<>();
	
	private Deposit selected;
	
	public SingleDepositRenderer() {
		MGAPChunkProcessor pr = (MGAPChunkProcessor) MineraGenesis.getProcessor();
		rbts = pr.getRegoliths();
		
		for (RockBiomeType rbt : rbts.getValues()) {
			for (RockBiome rb : rbt.getBiomes().getValues()) {
				for (Deposit deposit : rb.getDeposits()) {
					deposits.put(getName(deposit), (BaseDeposit) deposit);
				}
			}
		}
	}

	@Override
	public void render(BufferedImage img, int mult, int xo, int yo) {
		for (int x = 0; x < img.getWidth(); ++x) {
			
			columnLoop:
			for (int y = 0; y < img.getHeight(); ++y) {
				if (mult == 1) {
					if (paintChunkGrid(img, x, y, x*mult + xo, y*mult + yo)) {
						continue;
					}
				} else {
					if (paintGrid(img, x, y, x*mult + xo, y*mult + yo, 1000)) {
						continue;
					}
				}
				
				int cx = (x*mult + xo) / 16, cz = (y*mult + yo) / 16;
				RockBiome biome = rbts.getValue(cx, cz).getBiomes().getValue(cx, cz);
				
				for (Deposit deposit : biome.getDeposits()) {
					BaseDeposit bd = (BaseDeposit) deposit;
					if (bd.getColumnDensity(x*mult + xo, y*mult + yo) > 0) {
						if (deposit == selected) {
							img.setRGB(x, y, 0x88AA44);
						} else {
							img.setRGB(x, y, 0x444444);
						}
						continue columnLoop;
					}
				}
				
				img.setRGB(x, y, 0x000000);
			}
		}
	}

	@Override
	public JComponent getComponent() {
		final JComboBox<String> cb = new JComboBox<>(deposits.keySet().toArray(new String[0]));
		cb.addActionListener(action -> {
			selected = deposits.get((String) cb.getSelectedItem());
			GenerationPreview.render();
		});
		selected = deposits.get((String) cb.getSelectedItem());
		return cb;
	}
	
	private static class WeightedBlock implements Comparable<WeightedBlock> {
		private final String name;
		private final float weight;
		
		public WeightedBlock(String name, float weight) {
			this.name = name;
			this.weight = weight;
		}
		
		@Override
		public int compareTo(WeightedBlock o) {
			return (int) (weight - o.weight);
		}
	}
	
	private String getName(Deposit deposit) {
		StringBuilder sb = new StringBuilder(deposit.getClass().getSimpleName());
		
		if (deposit instanceof SimpliestDeposit) {
			
			SortedSet<WeightedBlock> set = new TreeSet<>();
			BlockWeightedList bwl = ((SimpliestDeposit) deposit).getBlocks();
			for (int i = 0; i < bwl.getBlocks().length; ++i) {
				short mgid = bwl.getBlocks()[i];
				set.add(new WeightedBlock(FakeLoader.getName(mgid) + FakeLoader.getMeta(mgid), bwl.getWeights()[i]));
			}
			
			for (WeightedBlock block : set) {
				sb.append(" ");
				sb.append(block.name);
			}
		
		}
		
		return sb.toString();
	}

}
