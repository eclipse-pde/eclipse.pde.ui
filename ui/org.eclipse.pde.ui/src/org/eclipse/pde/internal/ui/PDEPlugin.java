package org.eclipse.pde.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.ui.editor.feature.FeatureAdapterFactory;
import org.eclipse.pde.internal.ui.editor.manifest.PluginAdapterFactory;
import org.eclipse.pde.internal.ui.editor.schema.SchemaAdapterFactory;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.launcher.LaunchListener;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.view.PluginsViewAdapterFactory;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PDEPlugin extends AbstractUIPlugin implements IPDEUIConstants {
	private static final String KEY_RUNNING = "RunningEclipse.message";

	// Shared instance
	private static PDEPlugin inst;
	// Resource bundle
	private ResourceBundle resourceBundle;
	// Shared label labelProvider
	private PDELabelProvider labelProvider;
	// Launches listener
	private LaunchListener launchListener;

	private java.util.Hashtable counters;

	public PDEPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle("org.eclipse.pde.internal.ui.pderesources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	public static PDEPlugin getDefault() {
		return inst;
	}
	public Hashtable getDefaultNameCounters() {
		if (counters == null)
			counters = new Hashtable();
		return counters;
	}
	public static String getFormattedMessage(String key, String[] args) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, args);
	}
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	static IPath getInstallLocation() {
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = PDEPlugin.getDefault().getResourceBundle();
		if (bundle != null) {
			try {
				String bundleString = bundle.getString(key);
				//return "$"+bundleString;
				return bundleString;
			} catch (MissingResourceException e) {
				// default actions is to return key, which is OK
			}
		}
		return key;
	}
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}

	public static void logException(
		Throwable e,
		final String title,
		String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = SWTUtil.getStandardDisplay();
		final IStatus fstatus = status;
		display.asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, title, null, fstatus);
			}
		});
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else
			status =
				new Status(IStatus.ERROR, getPluginId(), IStatus.OK, e.getMessage(), e);
		log(status);
	}

	public void startup() throws CoreException {
		super.startup();

		IAdapterManager manager = Platform.getAdapterManager();
		SchemaAdapterFactory schemaFactory = new SchemaAdapterFactory();
		manager.registerAdapters(schemaFactory, ISchemaObject.class);
		manager.registerAdapters(schemaFactory, ISchemaObjectReference.class);
		manager.registerAdapters(new PluginAdapterFactory(), IPluginObject.class);
		manager.registerAdapters(new FeatureAdapterFactory(), IFeatureObject.class);
		PluginsViewAdapterFactory factory = new PluginsViewAdapterFactory();
		manager.registerAdapters(factory, ModelEntry.class);
		manager.registerAdapters(factory, FileAdapter.class);
		// set eclipse home variable if not sets
	}
	
	public void shutdown() throws CoreException {
		if (launchListener!=null)
			launchListener.shutdown();
		super.shutdown();
	}

	public static File getFileInPlugin(IPath path) {
		try {
			URL installURL =
				new URL(getDefault().getDescriptor().getInstallURL(), path.toString());
			URL localURL = Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	public PDELabelProvider getLabelProvider() {
		if (labelProvider == null)
			labelProvider = new PDELabelProvider();
		return labelProvider;
	}
	
	public LaunchListener getLaunchesListener() {
		if (launchListener == null)
			launchListener = new LaunchListener();
		return launchListener;
	}
	
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		ColorManager.initializeDefaults(store);
	}
}