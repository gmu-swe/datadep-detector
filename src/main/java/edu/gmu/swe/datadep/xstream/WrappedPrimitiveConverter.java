package edu.gmu.swe.datadep.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.gmu.swe.datadep.DependencyInfo;
import edu.gmu.swe.datadep.HeapWalker;
import edu.gmu.swe.datadep.struct.WrappedPrimitive;

public class WrappedPrimitiveConverter implements Converter {
	@Override
	public boolean canConvert(Class type) {
		return type == WrappedPrimitive.class;
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		return null;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		DependencyInfo dep = ((WrappedPrimitive) source).inf;
		if (dep != null && dep.isConflict())
			writer.addAttribute("dependsOn", HeapWalker.testNumToTestClass.get(dep.getWriteGen()) + "." + HeapWalker.testNumToMethod.get(dep.getWriteGen()));
		writer.setValue(((WrappedPrimitive) source).prim.toString());
	}
}