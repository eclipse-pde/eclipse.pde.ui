/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This task can be used to convert the report generated by the
 * apitooling.apifreeze task into an HTML report.
 */
public class APIFreezeReportConversionTask extends Task {
	static final class ConverterDefaultHandler extends DefaultHandler {
		private static final String API_BASELINE_DELTAS = "Added and removed bundles"; //$NON-NLS-1$
		private String[] arguments;
		private List<String> argumentsList;
		private String componentID;
		private final boolean debug;
		private int flags;
		private String key;
		private String kind;
		private final Map<String, List<Entry>> map;
		private String typename;
		private int elementType;
		/**
		 * String component id to ArrayList of String resolver error messages
		 */
		private final Map<String, List<String>> resolverErrors;
		private boolean isResolverSection;

		public ConverterDefaultHandler(boolean debug) {
			this.map = new HashMap<>();
			this.resolverErrors = new HashMap<>();
			this.debug = debug;
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (name == null) {
				return;
			}
			switch (name)
				{
			case IApiXmlConstants.ELEMENT_RESOLVER_ERRORS:
				isResolverSection = false;
				break;
			case IApiXmlConstants.DELTA_ELEMENT_NAME:
				Entry entry = new Entry(this.flags, this.elementType, this.key, this.typename, this.arguments, this.kind);
				List<Entry> list = this.map.get(this.componentID);
				if (list != null) {
					list.add(entry);
				} else {
					ArrayList<Entry> value = new ArrayList<>();
					value.add(entry);
					this.map.put(componentID, value);
				}
				break;
			case IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS:
				if (this.argumentsList != null && this.argumentsList.size() != 0) {
					this.arguments = new String[this.argumentsList.size()];
					this.argumentsList.toArray(this.arguments);
				}
				break;
			default:
				break;
			}
		}

		public Map<String, List<Entry>> getEntries() {
			return this.map;
		}

		/**
		 * @return String component id to List of String resolver error messages
		 */
		public Map<String, List<String>> getResolverErrors() {
			return resolverErrors;
		}

		/*
		 * Only used in debug mode
		 */
		private void printAttribute(Attributes attributes, String name) {
			System.out.println("\t" + name + " = " + String.valueOf(attributes.getValue(name))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (isResolverSection) {
				// Fill in the map with resolver errors
				if (IApiXmlConstants.ELEMENT_API_TOOL_REPORT.equals(name)) {
					componentID = attributes.getValue((IApiXmlConstants.ATTR_COMPONENT_ID));
				} else if (IApiXmlConstants.ELEMENT_RESOLVER_ERROR.equals(name)) {
					List<String> errors = resolverErrors.get(componentID);
					if (errors == null) {
						errors = new ArrayList<>();
						resolverErrors.put(componentID, errors);
					}
					String message = attributes.getValue(IApiXmlConstants.ATTR_MESSAGE);
					errors.add(message);
					if (this.debug) {
						System.out.println("Resolver error : " + componentID + " : " + message); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} else if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				if (this.debug) {
					System.out.println("name : " + name); //$NON-NLS-1$
					/*
					 * <delta compatible="true"
					 * componentId="org.eclipse.equinox.p2.ui_0.1.0"
					 * element_type="CLASS_ELEMENT_TYPE" flags="25" key=
					 * "schedule(Lorg/eclipse/equinox/internal/provisional/p2/ui/operations/ProvisioningOperation;Lorg/eclipse/swt/widgets/Shell;I)Lorg/eclipse/core/runtime/jobs/Job;"
					 * kind="ADDED" oldModifiers="9" newModifiers="9"
					 * restrictions="0" type_name=
					 * "org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner"
					 * />
					 */
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPATIBLE);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE);
					printAttribute(attributes, IApiXmlConstants.ATTR_FLAGS);
					printAttribute(attributes, IApiXmlConstants.ATTR_KEY);
					printAttribute(attributes, IApiXmlConstants.ATTR_KIND);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_NEW_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_OLD_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_RESTRICTIONS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				}
				final String value = attributes.getValue(IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
				if (value == null) {
					// removed or added bundles
					this.componentID = API_BASELINE_DELTAS;
				} else {
					this.componentID = value;
				}
				this.flags = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_FLAGS));
				this.elementType = Util.getDeltaElementTypeValue(attributes.getValue(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE));
				this.typename = attributes.getValue(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				this.key = attributes.getValue(IApiXmlConstants.ATTR_KEY);
				this.kind = attributes.getValue(IApiXmlConstants.ATTR_KIND);
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList == null) {
					this.argumentsList = new ArrayList<>();
				} else {
					this.argumentsList.clear();
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENT.equals(name)) {
				this.argumentsList.add(attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			} else if (IApiXmlConstants.ELEMENT_RESOLVER_ERRORS.equals(name)) {
				isResolverSection = true;
				if (this.debug) {
					System.out.println("Reading resolver error section"); //$NON-NLS-1$
				}
			}
		}
	}

	static class Entry {
		String[] arguments;
		int flags;
		int elementType;
		String key;
		String typeName;
		String kind;

		private static final String ADDED = "ADDED"; //$NON-NLS-1$
		private static final String REMOVED = "REMOVED"; //$NON-NLS-1$

		public Entry(int flags, int elementType, String key, String typeName, String[] arguments, String kind) {
			this.flags = flags;
			this.key = key.replace('/', '.');
			if (typeName != null) {
				this.typeName = typeName.replace('/', '.');
			}
			this.arguments = arguments;
			this.kind = kind;
			this.elementType = elementType;
		}

		public String getDisplayString() {
			StringBuilder buffer = new StringBuilder();
			if (this.typeName != null && this.typeName.length() != 0) {
				buffer.append(this.typeName);
				switch (this.flags) {
					case IDelta.API_METHOD_WITH_DEFAULT_VALUE:
					case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE:
					case IDelta.API_METHOD:
					case IDelta.METHOD:
					case IDelta.METHOD_WITH_DEFAULT_VALUE:
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE:
						int indexOf = this.key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						int index = indexOf;
						String selector = key.substring(0, index);
						String descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, true));
						break;
					case IDelta.API_CONSTRUCTOR:
					case IDelta.CONSTRUCTOR:
						indexOf = key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						index = indexOf;
						selector = key.substring(0, index);
						descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, false));
						break;
					case IDelta.FIELD:
					case IDelta.API_FIELD:
					case IDelta.ENUM_CONSTANT:
					case IDelta.API_ENUM_CONSTANT:
						buffer.append('#');
						buffer.append(this.key);
						break;
					case IDelta.TYPE_MEMBER:
					case IDelta.API_TYPE:
					case IDelta.REEXPORTED_TYPE:
					case IDelta.REEXPORTED_API_TYPE:
						buffer.append('.');
						buffer.append(this.key);
						break;
					case IDelta.INCREASE_ACCESS:
						indexOf = this.key.indexOf('(');
						if (indexOf == -1) {
							// increase access for non-methods (fields etc)
							buffer.append('#');
							buffer.append(this.key);
						} else {
							index = indexOf;
							selector = key.substring(0, index);
							descriptor = key.substring(index, key.length());
							buffer.append('#');
							buffer.append(Signature.toString(descriptor, selector, null, false, true));
						}
						break;
					case IDelta.DEPRECATION:
						switch (this.elementType) {
							case IDelta.ANNOTATION_ELEMENT_TYPE:
							case IDelta.INTERFACE_ELEMENT_TYPE:
							case IDelta.ENUM_ELEMENT_TYPE:
							case IDelta.CLASS_ELEMENT_TYPE:
								buffer.append('.');
								buffer.append(this.key);
								break;
							case IDelta.CONSTRUCTOR_ELEMENT_TYPE:
								indexOf = key.indexOf('(');
								if (indexOf == -1) {
									return null;
								}
								index = indexOf;
								selector = key.substring(0, index);
								descriptor = key.substring(index, key.length());
								buffer.append('#');
								buffer.append(Signature.toString(descriptor, selector, null, false, false));
								break;
							case IDelta.METHOD_ELEMENT_TYPE:
								indexOf = key.indexOf('(');
								if (indexOf == -1) {
									return null;
								}
								index = indexOf;
								selector = key.substring(0, index);
								descriptor = key.substring(index, key.length());
								buffer.append('#');
								buffer.append(Signature.toString(descriptor, selector, null, false, true));
								break;
							case IDelta.FIELD_ELEMENT_TYPE:
								buffer.append('#');
								buffer.append(this.key);
								break;
							default:
								break;
						}
						break;
					default:
						break;
				}
			} else {
				switch (this.flags) {
					case IDelta.MAJOR_VERSION:
						buffer.append(NLS.bind(Messages.deltaReportTask_entry_major_version, this.arguments));
						break;
					case IDelta.MINOR_VERSION:
						buffer.append(NLS.bind(Messages.deltaReportTask_entry_minor_version, this.arguments));
						break;
					case IDelta.API_BASELINE_ELEMENT_TYPE:
						buffer.append(this.key);
						break;
					default:
						break;
				}
			}
			return CommonUtilsTask.convertToHtml(String.valueOf(buffer));
		}

		public String getDisplayKind() {
			if (ADDED.equals(this.kind)) {
				return Messages.AddedElement;
			} else if (REMOVED.equals(this.kind)) {
				return Messages.RemovedElement;
			}
			return Messages.ChangedElement;
		}
	}

	boolean debug;

	private String htmlFileLocation;
	private String xmlFileLocation;

	private void dumpEndEntryForComponent(StringBuilder buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deltaReportTask_endComponentEntry, componentID));
	}

	private void dumpEntries(Map<String, List<Entry>> entries, Map<String, List<String>> resolverErrors, StringBuilder buffer) {
		dumpHeader(buffer);
		List<Map.Entry<String, List<Entry>>> allEntries = new ArrayList<>();
		allEntries.addAll(entries.entrySet());
		Collections.sort(allEntries, (o1, o2) -> {
			return o1.getKey().compareTo(o2.getKey());
		});
		for (Map.Entry<String, List<Entry>> mapEntry : allEntries) {
			String key = mapEntry.getKey();
			// TODO The component id used in the xml contains the version
			// "foo(1.1.2)", for now just remove based on brackets
			int index = key.indexOf('(');
			String componentName = index >= 0 ? key.substring(0, index) : key;
			dumpEntryForComponent(buffer, key);
			if (resolverErrors.containsKey(componentName)) {
				dumpResolverErrorSummary(buffer, componentName, resolverErrors.get(componentName));
			}
			List<Entry> values = mapEntry.getValue();
			Collections.sort(values, (entry1, entry2) -> {
				String typeName1 = entry1.typeName;
				String typeName2 = entry2.typeName;
				if (typeName1 == null) {
					if (typeName2 == null) {
						return entry1.key.compareTo(entry2.key);
					}
					return -1;
				} else if (typeName2 == null) {
					return 1;
				}
				if (!typeName1.equals(typeName2)) {
					return typeName1.compareTo(typeName2);
				}
				return entry1.key.compareTo(entry2.key);
			});
			if (debug) {
				System.out.println("Entries for " + key); //$NON-NLS-1$
			}
			for (Entry entry : values) {
				if (debug) {
					if (entry.typeName != null) {
						System.out.print(entry.typeName);
						System.out.print('#');
					}
					System.out.println(entry.key);
				}
				dumpEntry(buffer, entry);
			}
			dumpEndEntryForComponent(buffer, key);
			if (resolverErrors.containsKey(componentName)) {
				dumpResolverErrorTable(buffer, componentName, resolverErrors.get(componentName));
			}
		}
		dumpFooter(buffer);
	}

	private void dumpEntry(StringBuilder buffer, Entry entry) {
		buffer.append(NLS.bind(Messages.deltaReportTask_entry, entry.getDisplayKind(), entry.getDisplayString()));
	}

	private void dumpEntryForComponent(StringBuilder buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deltaReportTask_componentEntry, componentID));
	}

	private void dumpResolverErrorSummary(StringBuilder buffer, String componentID, List<String> resolverErrors) {
		int size = resolverErrors.size();
		if (size == 1) {
			buffer.append(NLS.bind(Messages.APIFreezeReportConversionTask_resolverErrorWarningSingle, new String[] {
					componentID, String.valueOf(size) }));
		} else if (size > 1) {
			buffer.append(NLS.bind(Messages.APIFreezeReportConversionTask_resolverErrorWarningMultiple, new String[] {
					componentID, String.valueOf(size) }));
		}
	}

	private void dumpResolverErrorTable(StringBuilder buffer, String componentID, List<String> resolverErrors) {
		buffer.append(NLS.bind(Messages.APIFreezeReportConversionTask_resolverErrorTableStart, componentID));
		for (String message : resolverErrors) {
			buffer.append(NLS.bind(Messages.APIFreezeReportConversionTask_resolverErrorTableEntry, message));
		}
		buffer.append(Messages.APIFreezeReportConversionTask_resolverErrorTableEnd);
	}

	private void dumpFooter(StringBuilder buffer) {
		buffer.append(Messages.deltaReportTask_footer);
	}

	private void dumpHeader(StringBuilder buffer) {
		buffer.append(Messages.deltaReportTask_header);
	}

	/**
	 * Run the ant task
	 */
	@Override
	public void execute() throws BuildException {
		if (this.xmlFileLocation == null) {
			throw new BuildException(Messages.deltaReportTask_missingXmlFileLocation);
		}
		if (this.debug) {
			System.out.println("xmlFileLocation : " + this.xmlFileLocation); //$NON-NLS-1$
			System.out.println("htmlFileLocation : " + this.htmlFileLocation); //$NON-NLS-1$
		}
		File file = new File(this.xmlFileLocation);
		if (!file.exists()) {
			throw new BuildException(NLS.bind(Messages.deltaReportTask_missingXmlFile, this.xmlFileLocation));
		}
		if (file.isDirectory()) {
			throw new BuildException(NLS.bind(Messages.deltaReportTask_xmlFileLocationMustBeAFile, this.xmlFileLocation));
		}
		File outputFile = null;
		if (this.htmlFileLocation == null) {
			int index = this.xmlFileLocation.lastIndexOf('.');
			if (index == -1 || !this.xmlFileLocation.substring(index).equalsIgnoreCase(".xml")) { //$NON-NLS-1$
				throw new BuildException(Messages.deltaReportTask_xmlFileLocationShouldHaveAnXMLExtension);
			}
			this.htmlFileLocation = extractNameFromXMLName(index);
			if (this.debug) {
				System.out.println("output name :" + this.htmlFileLocation); //$NON-NLS-1$
			}
			outputFile = new File(this.htmlFileLocation);
		} else {
			// check if the htmlFileLocation is a file and not a directory
			int index = this.htmlFileLocation.lastIndexOf('.');
			if (index == -1 || !this.htmlFileLocation.substring(index).equalsIgnoreCase(".html")) { //$NON-NLS-1$
				throw new BuildException(Messages.deltaReportTask_htmlFileLocationShouldHaveAnHtmlExtension);
			}
			outputFile = new File(this.htmlFileLocation);
			if (outputFile.exists()) {
				// if the file already exist, we check that this is a file
				if (outputFile.isDirectory()) {
					throw new BuildException(NLS.bind(Messages.deltaReportTask_hmlFileLocationMustBeAFile, outputFile.getAbsolutePath()));
				}
			} else {
				File parentFile = outputFile.getParentFile();
				if (!parentFile.exists()) {
					if (!parentFile.mkdirs()) {
						throw new BuildException(NLS.bind(Messages.errorCreatingParentReportFile, parentFile.getAbsolutePath()));
					}
				}
			}
		}
		SAXParser parser = AnalysisReportConversionTask.createSAXParser();
		try {
			ConverterDefaultHandler defaultHandler = new ConverterDefaultHandler(this.debug);
			parser.parse(file, defaultHandler);
			StringBuilder buffer = new StringBuilder();
			dumpEntries(defaultHandler.getEntries(), defaultHandler.getResolverErrors(), buffer);
			writeOutput(buffer);
		} catch (SAXException e) {
			// if xml file is empty, create an empty html file
			StringBuilder buffer = new StringBuilder();
			try {
				writeOutput(buffer);
			} catch (IOException e1) {
				// ignore
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private String extractNameFromXMLName(int index) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(this.xmlFileLocation.substring(0, index)).append(".html"); //$NON-NLS-1$
		return String.valueOf(buffer);
	}

	/**
	 * Set the debug value.
	 * <p>
	 * The possible values are: <code>true</code>, <code>false</code>
	 * </p>
	 * <p>
	 * Default is <code>false</code>.
	 * </p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue);
	}

	/**
	 * Set the path of the html file to generate.
	 *
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 *
	 * <p>
	 * This is optional. If not set, the html file name is retrieved from the
	 * xml file name by replacing ".xml" in ".html".
	 * </p>
	 *
	 * @param htmlFilePath the path of the html file to generate
	 */
	public void setHtmlFile(String htmlFilePath) {
		this.htmlFileLocation = htmlFilePath;
	}

	/**
	 * Set the path of the xml file to convert to html.
	 *
	 * <p>
	 * The path is set using an absolute path.
	 * </p>
	 *
	 * @param xmlFilePath the path of the xml file to convert to html
	 */
	public void setXmlFile(String xmlFilePath) {
		this.xmlFileLocation = xmlFilePath;
	}

	private void writeOutput(StringBuilder buffer) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.htmlFileLocation))) {
			bufferedWriter.write(String.valueOf(buffer));
		}
	}
}
