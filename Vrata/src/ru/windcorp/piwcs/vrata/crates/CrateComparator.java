package ru.windcorp.piwcs.vrata.crates;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrateComparator implements Comparator<Crate> {
	
	public static final CrateComparator NULL_SORTER = new CrateComparator(null);
	
	private final Matcher batchSorter;

	public CrateComparator(String batchSorter) {
		if (batchSorter == null) {
			this.batchSorter = null;
		} else {
			this.batchSorter = Pattern.compile(batchSorter, Pattern.UNICODE_CASE).matcher("");
		}
	}
	
	private static final int A_FIRST = -1;
	private static final int B_FIRST = 1;
	private static final int EQUAL = 0;

	@Override
	public int compare(Crate crateA, Crate crateB) {
		if (crateA.equals(crateB)) {
			return EQUAL;
		}
		
		boolean moderatedA = crateA.isModerated(),
				moderatedB = crateB.isModerated();
		
		if (!moderatedA) {
			if (moderatedB) return B_FIRST;
		} else {
			if (!moderatedB) return A_FIRST;
		}
		
		String batchA = crateA.getBatch(),
				batchB = crateB.getBatch();
		
		if (batchA == null) {
			if (batchB != null) return B_FIRST;
		} else {
			if (batchB == null) return A_FIRST;
			
			if (batchSorter != null) {
				synchronized (batchSorter) {
					boolean matchesA = batchSorter.reset(batchA).matches();
					boolean matchesB = batchSorter.reset(batchB).matches();
					
					if (!matchesA) {
						if (matchesB) return B_FIRST;
					} else {
						if (!matchesB) return A_FIRST;
					}
				}
			}
			
			int result = batchA.compareTo(batchB);
			
			if (result != EQUAL) {
				return result;
			}
		}
		
		return crateA.getCreationTime().compareTo(crateB.getCreationTime());
	}

}
