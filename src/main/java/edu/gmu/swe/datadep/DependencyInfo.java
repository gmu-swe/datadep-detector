package edu.gmu.swe.datadep;

import org.jdom2.Element;

public final class DependencyInfo {
	public static int CURRENT_TEST_COUNT = 1;

	private boolean ignored;
	private int crawledGen;
	private int writeGen;
	// private int readGen;
	private boolean conflict;
	public Element xmlEl;
	StaticField[] fields;

	private String value;

	public DependencyInfo() {

	}

	private StackTraceElement[] ex;

	public int getCrawledGen() {
		return crawledGen;
	}

	public void setCrawledGen() {
		this.crawledGen = CURRENT_TEST_COUNT;
	}

	public boolean isConflict() {
		return conflict;
	}

	public int getWriteGen() {
		return writeGen;
	}

	public void write() {
		writeGen = CURRENT_TEST_COUNT;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	public static boolean IN_CAPTURE = false;

	public void read() {
		if (IN_CAPTURE)
			return;
		if (ignored)
			return;
		if (conflict)
			return;
		if (writeGen != 0 && writeGen != CURRENT_TEST_COUNT) {
			conflict = true;
			// Snag the value of the field.
			if (fields != null) // could be null if only pointed to by ignored
								// heap roots
				for (StaticField sf : fields)
					if (sf != null) {
						if (sf.isConflict()) {
							// TODO(gyori): The xmlEl is somehow null. When can
							// this be null?
							if (xmlEl != null)
								xmlEl.setAttribute("dependsOn", HeapWalker.testNumToTestClass.get(getWriteGen()) + "." + HeapWalker.testNumToMethod.get(getWriteGen()));
						} else
							sf.markConflictAndSerialize(writeGen);
					}
		}
		// readGen = CURRENT_TEST_COUNT;
	}

	public static void write(Object obj) {
		if (obj instanceof DependencyInstrumented) {
			((DependencyInstrumented) obj).getDEPENDENCY_INFO().write();
		} else if (obj instanceof DependencyInfo) {
			((DependencyInfo) obj).write();
		} else if (obj != null) {
			TagHelper.getOrInitTag(obj).write();
		}
	}

	public static void read(Object obj) {
		if (obj instanceof DependencyInstrumented) {
			((DependencyInstrumented) obj).getDEPENDENCY_INFO().read();
		} else if (obj instanceof DependencyInfo) {
			((DependencyInfo) obj).read();
		} else if (obj != null) {
			TagHelper.getOrInitTag(obj).read();
		}
	}

	public void clearConflict() {
		this.conflict = false;
	}

	public void clearSFs() {
		if (fields != null)
			for (int i = 0; i < fields.length; i++)
				fields[i] = null;
	}
}
