package org.verapdf.gui.utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PolicyHandler extends DefaultHandler {

	private int policyNonCompliantJobCount = 0;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ((qName.equals("policyReport") && (Integer.parseInt(attributes.getValue("failedChecks")) > 0))) {
			this.policyNonCompliantJobCount++;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	public int getPolicyNonCompliantJobCount() {
		return this.policyNonCompliantJobCount;
	}
}
