package org.verapdf.cli.multithread.reports.writer;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.valueOf;
import static org.verapdf.component.AuditDurationImpl.*;

public class ReportParserEventHandler extends DefaultHandler {
	private static final Logger LOGGER = Logger.getLogger(ReportParserEventHandler.class.getCanonicalName());

	private final Set<String> BATCH_SUMMARY_TAGS =
			new HashSet<>(Arrays.asList("batchSummary", "validationReports", "featureReports", "repairReports"));

	private String element;
	private Map<String, Map<String, Integer>> batchSummary = new LinkedHashMap<>();
	private Map<String, Map<String, Integer>> current = new LinkedHashMap<>();

	private boolean isPrinting = false;
	private long startTime;

	private boolean isAddReportToSummary = false;

	private XMLStreamWriter writer;

	// TODO: review and refactor whole class
	public ReportParserEventHandler(XMLStreamWriter writer) {
		this.writer = writer;
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() {
		if (current.size() > 0) {
			if (batchSummary.size() > 0) {
				Set<String> keySet = current.keySet();
				keySet.forEach(k -> {
					Map<String, Integer> summaryAttributesAndValues = batchSummary.get(k);
					Map<String, Integer> currentAttributesAndValues = current.get(k);
					currentAttributesAndValues.forEach((key, v) -> summaryAttributesAndValues.merge(key, v, Integer::sum));
				});
			} else {
				batchSummary.putAll(current);
			}
			current.clear();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (isAddReportToSummary
				&& BATCH_SUMMARY_TAGS.contains(qName)
				&& !current.containsKey(qName)) {
			addReportToSummary(qName, attributes);
		}

		if (element.equals(qName)) {
			isPrinting = true;
		}

		if (isPrinting) {
			print(qName, attributes);
		}
	}

	private void print(String qName, Attributes attributes) {
		try {
			writer.writeStartElement(qName);
			for (int i = 0; i < attributes.getLength(); i++) {
				writer.writeAttribute(attributes.getQName(i), attributes.getValue(i));
			}
		} catch (XMLStreamException e) {
			LOGGER.log(Level.SEVERE, "Can't write the element", e);
		}
	}

	private void addReportToSummary(String qName, Attributes attributes) {
		Map<String, Integer> attributesAndValues = new LinkedHashMap<>();
		for (int i = 0; i < attributes.getLength(); i++) {
			String attribute = attributes.getQName(i);
			Integer value = Integer.valueOf(attributes.getValue(i));
			attributesAndValues.put(attribute, value);
		}
		current.put(qName, attributesAndValues);
	}

	public void printSummary() {
		try {
			String batchSummaryTag = "batchSummary";

			writeStartBatchSummaryTag(batchSummaryTag);

			batchSummary.remove(batchSummaryTag);

			writeTagsInsideBatchSummary(batchSummary.keySet());

			writer.writeEndElement();
		} catch (XMLStreamException e) {
			LOGGER.log(Level.SEVERE, "Can't write the element", e);
		}
	}

	private void writeTagsInsideBatchSummary(Set<String> batchSummaryTags) throws XMLStreamException {
		batchSummaryTags.forEach(k -> {
			try {
				writer.writeStartElement(k);

				Map<String, Integer> attributesAndValues = this.batchSummary.get(k);
				int sum = attributesAndValues.values().stream().mapToInt(Number::intValue).sum();
				attributesAndValues.forEach((attribute, value) -> {
					try {
						writer.writeAttribute(attribute, valueOf(attributesAndValues.get(attribute)));
					} catch (XMLStreamException e) {
						LOGGER.log(Level.SEVERE, "Can't write the element", e);
					}
				});
				writer.writeCharacters(valueOf(sum));
				writer.writeEndElement();
			} catch (XMLStreamException e) {
				LOGGER.log(Level.SEVERE, "Can't write the element", e);
			}
		});

		writeDurationTag();
	}

	private void writeDurationTag() throws XMLStreamException {
		long finishTime = System.currentTimeMillis();
		String stringDuration = getStringDuration(finishTime - this.startTime);

		writer.writeStartElement("duration");
		writer.writeAttribute("start", valueOf(this.startTime));
		writer.writeAttribute("finish", valueOf(finishTime));
		writer.writeCharacters(stringDuration);
		writer.writeEndElement();
	}

	private void writeStartBatchSummaryTag(String batchSummaryTag) throws XMLStreamException {
		writer.writeStartElement(batchSummaryTag);
		Map<String, Integer> batchSummaryAttributesAndValues = this.batchSummary.get(batchSummaryTag);
		batchSummaryAttributesAndValues.forEach((attribute, value) -> {
			try {
				writer.writeAttribute(attribute, valueOf(value));
			} catch (XMLStreamException e) {
				LOGGER.log(Level.SEVERE, "Can't write the element", e);
			}
		});
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (isPrinting) {
			try {
				writer.writeEndElement();
			} catch (XMLStreamException e) {
				LOGGER.log(Level.SEVERE, "Can't write the element", e);
			}
		}
		if (element.equals(qName)) {
			isPrinting = false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if (isPrinting) {
			try {
				writer.writeCharacters(new String(ch, start, length));
			} catch (XMLStreamException e) {
				LOGGER.log(Level.SEVERE, "Can't write the element", e);
			}
		}
	}

	public void setElement(String element) {
		this.element = element;
	}

	public void setIsAddReportToSummary(boolean addReportToSummary) {
		this.isAddReportToSummary = addReportToSummary;
	}
}
