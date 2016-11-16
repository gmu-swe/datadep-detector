package edu.gmu.swe.datadep;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class StaticField {
	public Field field;
	private boolean conflict;
	public Element value;
	public int dependsOn;

	public StaticField(Field f) {
		this.field = f;
	}

	public boolean isConflict() {
		return conflict;
	}

	public void markConflictAndSerialize(int writeGen) {
		conflict = true;
		if (writeGen > dependsOn)
			dependsOn = writeGen;
		// if (value != null)
		// return;
		try {
			field.setAccessible(true);
			value = HeapWalker.serialize(field.get(null));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public String getValue() {
		StringWriter sw = new StringWriter();
		XMLOutputter out = new XMLOutputter();

		try {
			Element e = (Element) value.getContent().get(0);
			out.output(new Document(e.detach()), sw);
			out.setFormat(Format.getPrettyFormat());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	@Override
	public String toString() {
		return "StaticField [field=" + field + ", conflict=" + conflict + ", value=" + value + ", dependsOn=" + dependsOn + "]";
	}

	public void clearConflict() {
		conflict = false;
		value = null;
		dependsOn = 0;
	}
}
