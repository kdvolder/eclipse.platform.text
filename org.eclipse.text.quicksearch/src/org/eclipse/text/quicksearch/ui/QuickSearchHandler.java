/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.text.quicksearch.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("restriction")
public class QuickSearchHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public QuickSearchHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		doQuickSearch(window);
		return null;
	}

	public static void doQuickSearch(IWorkbenchWindow window) {
		QuickSearchDialog dialog = new QuickSearchDialog(window);
		initializeFromSelection(window, dialog);
		dialog.open();
	}

	/**
	 * Based on the current active selection initialize the priority function and/or
	 * the initial contents of the search box.
	 */
	 static private void initializeFromSelection(IWorkbenchWindow workbench, QuickSearchDialog dialog) {
		if (workbench!=null) {
			ISelectionService selectionService = workbench.getSelectionService();
			ISelection selection = selectionService.getSelection();
			if (selection!=null && selection instanceof ITextSelection) {
				//Use text selection to set initial search pattern.
				String text = ((ITextSelection) selection).getText();
				if (text!=null && !"".equals(text)) {
					dialog.setInitialPattern(text, QuickSearchDialog.FULL_SELECTION);
				}
			} 
		}
//		IEditorPart editor = HandlerUtil.getActiveEditor(event);
//		if (editor!=null && editor instanceof ITextEditor) {
//			ITextEditor textEditor = (ITextEditor)editor;
//			ISelection selection = textEditor.getSelectionProvider().getSelection();
//			if (selection!=null && selection instanceof ITextSelection) {
//				String text = ((ITextSelection) selection).getText();
//				if (text!=null && !"".equals(text)) {
//					dialog.setInitialPattern(text, QuickSearchDialog.FULL_SELECTION);
//				}
//			}
//		}
	}
}
