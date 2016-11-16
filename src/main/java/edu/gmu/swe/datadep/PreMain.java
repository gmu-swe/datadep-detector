package edu.gmu.swe.datadep;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class PreMain {
	private static Instrumentation instrumentation;

	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

	public static boolean IS_RUNTIME_INST = true;

	public static void premain(String args, Instrumentation inst) {
		instrumentation = inst;
		ClassFileTransformer transformer = new RWDependencyClassFileTransformer();
		inst.addTransformer(transformer);

	}
}
