package edu.gmu.swe.datadep;

import org.jdom2.Element;

import com.thoughtworks.xstream.io.xml.JDom2Writer;

public class JDomHackWriter extends JDom2Writer {
	public Element recentNode;

	public JDomHackWriter(Element root) {
		super(root);
	}

	@Override
	protected Object createNode(String name) {
		Object ret = super.createNode(name);
		recentNode = (Element) ret;
		return ret;
	}
}
