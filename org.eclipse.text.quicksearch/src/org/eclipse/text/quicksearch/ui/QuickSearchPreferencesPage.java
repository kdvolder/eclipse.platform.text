/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.text.quicksearch.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.quicksearch.core.preferences.QuickSearchPreferences;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class QuickSearchPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public QuickSearchPreferencesPage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(QuickSearchActivator.getDefault().getPreferenceStore());
		QuickSearchPreferences.initializeDefaults();
	}

	@Override
	public void init(IWorkbench arg0) {
	}

	private static final String[] prefsKeys = {
			QuickSearchPreferences.IGNORED_EXTENSIONS, 
			QuickSearchPreferences.IGNORED_PREFIXES, 
			QuickSearchPreferences.IGNORED_NAMES
	};

	private static final String[] fieldNames = {
			"Extensions", "Prefixes", "Names"
	};

	private static final String[] toolTips = {
			"Enter a list of file extensions. Elements in the list can be separated by commas or newlines." +
			"Any file or folder ending with one of the extensions will be ignored."
		,
			"Enter a list of file prefixes. Elements in the list can be separated by commas or newlines." +
			"Any file or folder who's name begins with one of the extensions will be ignored."
		,
			"Enter a list of file names. Elements in the list can be separated by commas or newlines." +
			"Any file or folder who's name equals one of the extensions will be ignored."
	};

	@Override
	protected void createFieldEditors() {
		IntegerFieldEditor field_maxLineLen = new IntegerFieldEditor(QuickSearchPreferences.MAX_LINE_LEN, "Max Line Length", getFieldEditorParent());
		field_maxLineLen.getTextControl(getFieldEditorParent()).setToolTipText(
				"When QuickSearch encounters a line of text longer than 'Max Line Length' it stops " + 
				"searching the current file. This is meant to avoid searching in machine generated text " + 
				"files, such as, minified javascript.");
		addField(field_maxLineLen);
		
		for (int i = 0; i < fieldNames.length; i++) {
			final String tooltip = toolTips[i];
			StringFieldEditor field = new StringFieldEditor(prefsKeys[i], "Ignore "+fieldNames[i], 45, 5, StringFieldEditor.VALIDATE_ON_FOCUS_LOST, getFieldEditorParent()) {
				@Override
				protected Text createTextWidget(Composite parent) {
					Text w = super.createTextWidget(parent);
					w.setToolTipText(tooltip);
					return w;
				}
			};
			addField(field);
		}
	}
}
