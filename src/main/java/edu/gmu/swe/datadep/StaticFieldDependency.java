package edu.gmu.swe.datadep;

import java.lang.reflect.Field;

public class StaticFieldDependency {
	public Field field;
	public String value;
	public int depGen;
	@Override
	public String toString() {
		return field.toString() + ", dependsOn " + depGen +", value: " + value;
	}
}
