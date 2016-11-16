package edu.gmu.swe.datadep.struct;

import edu.gmu.swe.datadep.DependencyInfo;

public class WrappedPrimitive {
	public WrappedPrimitive(Object value, DependencyInfo dep) {
		this.prim = value;
		this.inf = dep;
	}
	public Object prim;
	public DependencyInfo inf;
	@Override
	public String toString() {
		if(prim == null)
			return null;
		return prim.toString();
	}
}
