package test;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.noise.FractalNoise;
import ru.windcorp.mineragenesis.piwcs.MGAPChunkProcessor;
import ru.windcorp.mineragenesis.piwcs.gen.BlockList;
import ru.windcorp.mineragenesis.piwcs.gen.BlockWeightedList;
import ru.windcorp.mineragenesis.piwcs.gen.Deposit;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiome;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiomeType;
import ru.windcorp.mineragenesis.piwcs.gen.SimpliestDeposit;

import static test.FakeLoader.*;

public class ConfigurationTest {

	public static void main(String[] args) {
		load();
		
		System.out.println("\n\n\nDATA\n");
		
		MGAPChunkProcessor processor = (MGAPChunkProcessor) MineraGenesis.getProcessor();
		printFractalNoise("Bedrock Height", processor.getBedrockHeight(), 0);
		printRBT("Bedrock", processor.getBedrock(), 0);
		printDiscreteNoise("Regoliths", processor.getRegoliths(), ConfigurationTest::printRBT, 0);
	}
	
	public static int parseInt(char[] declar, int length) {
		int result = 0;
		
		int i;
		boolean isNegative = false;
		if (declar[0] == '-') {
			isNegative = true;
			i = 1;
		} else if (declar[0] == '+') {
			i = 1;
		} else {
			i = 0;
		}
		
		for (; i < length; ++i) {
			result += declar[i];
			result *= 10;
		}
		
		if (isNegative) {
			result = -result;
		}
		
		return result;
	}
	
	@FunctionalInterface
	private static interface Printer<T> {
		void print(String name, T value, int level);
	}

	private static void print(Object line, int level) {
		for (int i = 0; i < level; ++i) System.out.print('\t');
		System.out.println(line);
	}

	private static void printFractalNoise(String name, FractalNoise noise, int level) {
		name += " " + noise.getClass().getSimpleName();
		print(name + " {", level++);
		print("Bias: " + noise.getBias(), level);
		print("Bias (normalizing): " + noise.getNormalizingBias(), level);
		for (int i = 0; i < noise.getFrequencies().length; ++i) {
			print("N" + (i + 1) + ": amp: " + noise.getAmplitudes()[i] + "; freq: " + noise.getFrequencies()[i], level);
		}
		print("} " + name, --level);
	}
	
	private static void printRBT(String name, RockBiomeType rbt, int level) {
		name += " " + rbt.getClass().getSimpleName();
		print(name + " {", level++);
		printDiscreteNoise("RBs", rbt.getBiomes(), ConfigurationTest::printRB, level);
		print("} " + name, --level);
	}
	
	private static void printRB(String name, RockBiome rb, int level) {
		name += " " + rb.getClass().getSimpleName();
		print(name + " {", level++);
		printBlock("Rock", rb.getRockType(), level);
		printBlockList("Replaceables", rb.getReplaceables(), level);
		
		int i = 0;
		for (Deposit deposit : rb.getDeposits()) {
			printSimpliestDeposit("D" + i, (SimpliestDeposit) deposit, level);
			i++;
		}
		print("} " + name, --level);
	}

	private static void printSimpliestDeposit(String name, SimpliestDeposit deposit, int level) {
		name += " " + deposit.getClass().getSimpleName();
		print(name + " {", level++);
		
		printBlockWeightedList("Blocks", deposit.getBlocks(), level);
		print("Unit Thickness: " + deposit.getUnitThickness(), level);
		print("Unit Weight" + deposit.getUnitWeight(), level);
		printFractalNoise("Density Noise", deposit.getDensityNoise(), level);
		printFractalNoise("Height Noise", deposit.getHeightNoise(), level);
		
		print("} " + name, --level);
	}

	private static void printBlockWeightedList(String name, BlockWeightedList list, int level) {
		name += " " + list.getClass().getSimpleName();
		print(name + " {", level++);
		
		int i = 0;
		for (short mgid : list.getBlocks()) {
			print(getName(mgid) + getMeta(mgid) + ": " + list.getWeights()[i] + "w (" +
					((int) (list.getWeights()[i] / list.getSum() * 10000)) / 100.0 + "%)", level);
			++i;
		}
		
		print("} " + name, --level);
	}

	private static void printBlockList(String name, BlockList list, int level) {
		name += " " + list.getClass().getSimpleName();
		print(name + " {", level++);
		
		boolean[] metas;
		for (int id = 0; id < list.getFlags().length; ++id) {
			metas = list.getFlags()[id];
			if (metas != null) {
				if (metas == BlockList.getEverythingFlag()) {
					print(getName(id) + ":*", level);
					continue;
				}
				for (int meta = 0; meta < 16; ++meta) {
					if (metas[meta]) {
						print(getName(id) + ":" + meta, level);
					}
				}
			}
		}
		print("} " + name, --level);
	}

	private static void printBlock(String name, short mgid, int level) {
		print(name + ": " + getName(mgid) + getMeta(mgid), level);
	}

	private static <T> void printDiscreteNoise(String name, DiscreteNoise<T> noise, Printer<T> printer, int level) {
		name += " " + noise.getClass().getSimpleName();
		print(name + " {", level++);

		for (int i = 0; i < noise.getValues().length; ++i) {
			printFractalNoise("N" + i, noise.getNoises()[i], level);
			printer.print("V" + i, noise.getValues()[i], level);
		}
		
		print("} " + name, --level);
	}

}
