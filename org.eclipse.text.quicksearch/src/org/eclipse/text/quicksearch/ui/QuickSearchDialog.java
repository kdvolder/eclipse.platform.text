/*******************************************************************************
 * Copyright (c) 2000, 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * Willian Mitsuda <wmitsuda@gmail.com>
 *    - Fix for bug 196553 - [Dialogs] Support IColorProvider/IFontProvider in FilteredItemsSelectionDialog
 * Peter Friese <peter.friese@gentleware.com>
 *    - Fix for bug 208602 - [Dialogs] Open Type dialog needs accessible labels
 * Simon Muschel <smuschel@gmx.de> - bug 258493
 * Kris De Volder Copied and modified from org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog
 *                to create QuickSearchDialog
 *******************************************************************************/
package org.eclipse.text.quicksearch.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.search.internal.ui.text.EditorOpener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.text.quicksearch.core.LineItem;
import org.eclipse.text.quicksearch.core.QuickTextQuery;
import org.eclipse.text.quicksearch.core.QuickTextSearchRequestor;
import org.eclipse.text.quicksearch.core.QuickTextSearcher;
import org.eclipse.text.quicksearch.core.QuickTextQuery.TextRange;
import org.eclipse.text.quicksearch.core.pathmatch.ResourceMatchers;
import org.eclipse.text.quicksearch.util.DocumentFetcher;
import org.eclipse.text.quicksearch.util.TableResizeHelper;
import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.progress.UIJob;

/**
 * Shows a list of items to the user with a text entry field for a string
 * pattern used to filter the list of items.
 *
 * @since 3.3
 */
@SuppressWarnings({ "rawtypes", "restriction", "unchecked" })
public class QuickSearchDialog extends SelectionStatusDialog {

	private static final int GO_BUTTON_ID = IDialogConstants.CLIENT_ID  + 1;
	private static final int REFRESH_BUTTON_ID = IDialogConstants.CLIENT_ID + 2;

	public static final Styler HIGHLIGHT_STYLE = org.eclipse.search.internal.ui.text.DecoratingFileSearchLabelProvider.HIGHLIGHT_STYLE;


//	public class ScrollListener implements SelectionListener {
//
//		ScrollBar scrollbar;
//
//		public ScrollListener(ScrollBar scrollbar) {
//			this.scrollbar = scrollbar;
//			scrollbar.addSelectionListener(this);
//		}
//
//		@Override
//		public void widgetDefaultSelected(SelectionEvent e) {
//			processEvent(e);
//		}
//
//		private int oldPercent = 0;
//
//		private void processEvent(SelectionEvent e) {
//			int min = scrollbar.getMinimum();
//			int max = scrollbar.getMaximum();
//			int val = scrollbar.getSelection();
//			int thumb = scrollbar.getThumb();
//
//			int total = max - min; //Total range of the scrollbar
//			int end = val+thumb; //The bottom of visible region
//			int belowEnd = max - end; //Size of area that is below the current visible area.
//			int percent = (belowEnd*100)/total; // size in percentage of total area that is below visible area.
//
//			System.out.println("==== scroll event ===");
//			System.out.println("min: "+min +"  max: "+max);
//			System.out.println("val: "+val +"  thum: "+thumb);
//			System.out.println("percent: "+percent);
//			if (percent <= 10) {
//				walker.requestMoreResults();
//			}
//			if (Math.abs(percent-oldPercent)>50) {
//				System.out.println("Big jump!");
//			}
//			oldPercent = percent;
//		}
//
//		@Override
//		public void widgetSelected(SelectionEvent e) {
//			processEvent(e);
//		}
//	}

	private UIJob refreshJob = new UIJob("Refresh") {
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			refreshWidgets();
			return Status.OK_STATUS;
		}
	};
	
	protected void openSelection() {
		try {
			LineItem item = (LineItem) this.getFirstResult();
			if (item!=null) {
				QuickTextQuery q = this.getQuery();
				TextRange range = q.findFirst(item.getText());
				EditorOpener opener = new EditorOpener();
				IWorkbenchPage page = window.getActivePage();
				if (page!=null) {
					opener.openAndSelect(page, item.getFile(), range.getOffset()+item.getOffset(), 
						range.getLength(), true);
				}
			}
		} catch (PartInitException e) {
			QuickSearchActivator.log(e);
		}
	}
	
	/**
	 * Job that shows a simple busy indicator while a search is active.
	 * The job must be scheduled when a search starts/resumes.
	 */
	private UIJob progressJob =  new UIJob("Refresh") {
		int animate = 0; // number of dots to display.

		protected String dots(int animate) {
			char[] chars = new char[animate];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = '.';
			}
			return new String(chars);
		}

		protected String currentFileInfo(IFile currentFile, int animate) {
			if (currentFile!=null) {
				String path = currentFile.getFullPath().toString();
				if (path.length()<=30) {
					return path;
				}
				return "..."+path.substring(path.length()-30);
			}
			return dots(animate);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor mon) {
			if (!mon.isCanceled() && progressLabel!=null && !progressLabel.isDisposed()) {
				if (searcher==null || searcher.isDone()) {
					progressLabel.setText("");
				} else {
					progressLabel.setText("Searching"+currentFileInfo(searcher.getCurrentFile(), animate));
					animate = (animate+1)%4;
					this.schedule(333);
				}
			}
			return Status.OK_STATUS;
		}
	};

	public final StyledCellLabelProvider LINE_NUMBER_LABEL_PROVIDER = new StyledCellLabelProvider() {
		@Override
		public void update(ViewerCell cell) {
			LineItem item = (LineItem) cell.getElement();
			if (item!=null) {
				cell.setText(""+item.getLineNumber());
			} else {
				cell.setText("?");
			}
			cell.setImage(getBlankImage());
		};
	};

	private static final Color GREY = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

	private final StyledCellLabelProvider LINE_TEXT_LABEL_PROVIDER = new StyledCellLabelProvider() {
		
		@Override
		public void update(ViewerCell cell) {
			LineItem item = (LineItem) cell.getElement();
			if (item!=null) {
				StyledString text = highlightMatches(item.getText());
				cell.setText(text.getString());
				cell.setStyleRanges(text.getStyleRanges());
			} else {
				cell.setText("");
				cell.setStyleRanges(null);
			}
			cell.setImage(getBlankImage());
			super.update(cell);
		}
	};

	private Image blankImage;

	private Image getBlankImage() {
		if (blankImage==null) {
			blankImage = new Image(Display.getDefault(), 1, 1);
//			GC gc = new GC(blankImage);
//			gc.fillRectangle(0, 0, 16, 16);
//			gc.dispose();
		}
		return blankImage;
	}

	private final StyledCellLabelProvider LINE_FILE_LABEL_PROVIDER = new StyledCellLabelProvider() {

		@Override
		public void update(ViewerCell cell) {
			LineItem item = (LineItem) cell.getElement();
			if (item!=null) {
				IPath path = item.getFile().getFullPath();
				String name = path.lastSegment();
				String dir = path.removeLastSegments(1).toString();
				cell.setText(name + " - "+dir);
				StyleRange[] styleRanges = new StyleRange[] {
						new StyleRange(name.length(), dir.length()+3, GREY, null)
				};
				cell.setStyleRanges(styleRanges);
			} else {
				cell.setText("");
				cell.setStyleRanges(null);
			}
			cell.setImage(getBlankImage());
			super.update(cell);
		}
		
//		public String getToolTipText(Object element) {
//			LineItem item = (LineItem) element;
//			if (item!=null) {
//				return ""+item.getFile().getFullPath();
//			}
//			return "";
//		};

//		public String getText(Object _item) {
//			if (_item!=null) {
//				LineItem item = (LineItem) _item;
//				return item.getFile().getName().toString();
//			}
//			return "?";
//		};
	};

	private static final String DIALOG_SETTINGS = QuickSearchDialog.class.getName()+".DIALOG_SETTINGS";

	private static final String DIALOG_BOUNDS_SETTINGS = "DialogBoundsSettings"; //$NON-NLS-1$

	private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$
	private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$
	private static final String DIALOG_COLUMNS = "COLUMN_WIDTHS";
	private static final String DIALOG_SASH_WEIGHTS = "SASH_WEIGHTS";

	private static final String DIALOG_LAST_QUERY = "LAST_QUERY";
	private static final String DIALOG_PATH_FILTER = "PATH_FILTER";
	private static final String CASE_SENSITIVE = "CASE_SENSITIVE";
	private static final boolean CASE_SENSITIVE_DEFAULT = true;

	private static final String KEEP_OPEN = "KEEP_OPEN";
	private static final boolean KEEP_OPEN_DEFAULT = false;

	/**
	 * Represents an empty selection in the pattern input field (used only for
	 * initial pattern).
	 */
	public static final int NONE = 0;

	/**
	 * Pattern input field selection where caret is at the beginning (used only
	 * for initial pattern).
	 */
	public static final int CARET_BEGINNING = 1;

	/**
	 * Represents a full selection in the pattern input field (used only for
	 * initial pattern).
	 */
	public static final int FULL_SELECTION = 2;

	private Text pattern;

	private TableViewer list;

	private MenuManager menuManager;

	private MenuManager contextMenuManager;

	private boolean multi;

	private ToolBar toolBar;

	private ToolItem toolItem;

	private Label progressLabel;

	private ContentProvider contentProvider;

	private String initialPatternText;

	private int selectionMode;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private final int MAX_LINE_LEN;

	private IHandlerActivation showViewHandler;

	private QuickTextSearcher searcher;

	private StyledText details;

	private DocumentFetcher documents;


	private ToggleCaseSensitiveAction toggleCaseSensitiveAction;
	private ToggleKeepOpenAction toggleKeepOpenAction;
	

	private QuickSearchContext context;


	private SashForm sashForm;

	private Label headerLabel;

	private IWorkbenchWindow window;
	private Text searchIn;

	/**
	 * Creates a new instance of the class.
	 *
	 * @param window.getShell()
	 *           shell to parent the dialog on
	 * @param multi
	 *           indicates whether dialog allows to select more than one
	 *           position in its list of items
	 */
	public QuickSearchDialog(IWorkbenchWindow window) {
		super(window.getShell());
		this.window = window;
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
		setBlockOnOpen(false);
		this.setTitle("Quick Text Search");
		this.context = new QuickSearchContext(window);
		this.multi = false;
		contentProvider = new ContentProvider();
		selectionMode = NONE;
		MAX_LINE_LEN = QuickSearchActivator.getDefault().getPreferences().getMaxLineLen();
	}

//	/**
//	 * Returns the label decorator for selected items in the list.
//	 *
//	 * @return the label decorator for selected items in the list
//	 */
//	private ILabelDecorator getListSelectionLabelDecorator() {
//		return getItemsListLabelProvider().getSelectionDecorator();
//	}
//
//	/**
//	 * Sets the label decorator for selected items in the list.
//	 *
//	 * @param listSelectionLabelDecorator
//	 *           the label decorator for selected items in the list
//	 */
//	public void setListSelectionLabelDecorator(
//			ILabelDecorator listSelectionLabelDecorator) {
//		getItemsListLabelProvider().setSelectionDecorator(
//				listSelectionLabelDecorator);
//	}

//	/**
//	 * Returns the item list label provider.
//	 *
//	 * @return the item list label provider
//	 */
//	private ItemsListLabelProvider getItemsListLabelProvider() {
//		if (itemsListLabelProvider == null) {
//			itemsListLabelProvider = new ItemsListLabelProvider(
//					new LabelProvider(), null);
//		}
//		return itemsListLabelProvider;
//	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		pattern.setFocus();
	}

	/**
	 * Restores dialog using persisted settings.
	 */
	protected void restoreDialog(IDialogSettings settings) {
		try {
			if (initialPatternText==null) {
				String lastSearch = settings.get(DIALOG_LAST_QUERY);
				if (lastSearch==null) {
					lastSearch = "";
				}
				pattern.setText(lastSearch);
				pattern.setSelection(0, lastSearch.length());
			}
			if (settings.get(DIALOG_PATH_FILTER)!=null) {
				String filter = settings.get(DIALOG_PATH_FILTER);
				searchIn.setText(filter);
			}

			if (settings.getArray(DIALOG_COLUMNS)!=null) {
				String[] columnWidths = settings.getArray(DIALOG_COLUMNS);
				Table table = list.getTable();
				int cols = table.getColumnCount();
				for (int i = 0; i < cols; i++) {
					TableColumn col = table.getColumn(i);
					try {
						if (col!=null) {
							col.setWidth(Integer.valueOf(columnWidths[i]));
						}
					} catch (Throwable e) {
						QuickSearchActivator.log(e);
					}
				}
			}

			if (settings.getArray(DIALOG_SASH_WEIGHTS)!=null) {
				String[] _weights = settings.getArray(DIALOG_SASH_WEIGHTS);
				int[] weights = new int[_weights.length];
				for (int i = 0; i < weights.length; i++) {
					weights[i] = Integer.valueOf(_weights[i]);
				}
				sashForm.setWeights(weights);
			}
		} catch (Throwable e) {
			//None of this stuff is critical so shouldn't stop opening dialog if it fails!
			QuickSearchActivator.log(e);
		}
	}

	private class ToggleKeepOpenAction extends Action {
		public ToggleKeepOpenAction(IDialogSettings settings) {
			super(
					"Keep Open",
					IAction.AS_CHECK_BOX
			);
			if (settings.get(KEEP_OPEN)==null) {
				setChecked(KEEP_OPEN_DEFAULT);
			} else{
				setChecked(settings.getBoolean(KEEP_OPEN));
			}
		}

		public void run() {
			//setChecked(!isChecked());
		}

	}


	private class ToggleCaseSensitiveAction extends Action {

		public ToggleCaseSensitiveAction(IDialogSettings settings) {
			super(
					"Case Sensitive",
					IAction.AS_CHECK_BOX
			);
			if (settings.get(CASE_SENSITIVE)==null) {
				setChecked(CASE_SENSITIVE_DEFAULT);
			} else{
				setChecked(settings.getBoolean(CASE_SENSITIVE));
			}
		}

		public void run() {
			//setChecked(!isChecked());
			refreshHeaderLabel();
			applyFilter(false);
		}
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		this.progressJob.cancel();
		this.progressJob = null;
//		this.refreshProgressMessageJob.cancel();
		if (showViewHandler != null) {
			IHandlerService service = (IHandlerService) PlatformUI
					.getWorkbench().getService(IHandlerService.class);
			service.deactivateHandler(showViewHandler);
			showViewHandler.getHandler().dispose();
			showViewHandler = null;
		}
		if (menuManager != null)
			menuManager.dispose();
		if (contextMenuManager != null)
			contextMenuManager.dispose();
		storeDialog(getDialogSettings());
		if (searcher!=null) {
			searcher.cancel();
		}
		return super.close();
	}

	/**
	 * Stores dialog settings.
	 *
	 * @param settings
	 *           settings used to store dialog
	 */
	protected void storeDialog(IDialogSettings settings) {
		String currentSearch = pattern.getText();
		settings.put(DIALOG_LAST_QUERY, currentSearch);
		settings.put(DIALOG_PATH_FILTER, searchIn.getText());
		if (toggleCaseSensitiveAction!=null) {
			settings.put(CASE_SENSITIVE, toggleCaseSensitiveAction.isChecked());
		}
		if (toggleKeepOpenAction!=null) {
			settings.put(KEEP_OPEN, toggleKeepOpenAction.isChecked());
		}
		Table table = list.getTable();
		if (table.getColumnCount()>0) {
			String[] columnWidths = new String[table.getColumnCount()];
			for (int i = 0; i < columnWidths.length; i++) {
				columnWidths[i] = ""+table.getColumn(i).getWidth();
			}
			settings.put(DIALOG_COLUMNS, columnWidths);
		}
		if (sashForm.getWeights()!=null) {
			int[] w = sashForm.getWeights();
			String[] ws = new String[w.length];
			for (int i = 0; i < ws.length; i++) {
				ws[i] = ""+w[i];
			}
			settings.put(DIALOG_SASH_WEIGHTS, ws);
		}
	}

	/**
	 * Create a new header which is labelled by headerLabel.
	 *
	 * @param parent
	 * @return Label the label of the header
	 */
	private Label createHeader(Composite parent) {
		Composite header = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		header.setLayout(layout);

		headerLabel = new Label(header, SWT.NONE);
		headerLabel.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit) {
					e.detail = SWT.TRAVERSE_NONE;
					pattern.setFocus();
				}
			}
		});

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		headerLabel.setLayoutData(gd);

		createViewMenu(header);
		header.setLayoutData(gd);

		refreshHeaderLabel();
		return headerLabel;
	}

	private void refreshHeaderLabel() {
		String msg = toggleCaseSensitiveAction.isChecked() ? "Case SENSITIVE" : "Case INSENSITIVE";
		msg += " Pattern (? = any character, * = any string)";
		headerLabel.setText(msg);
	}

	/**
	 * Create the labels for the list and the progress. Return the list label.
	 *
	 * @param parent
	 * @return Label
	 */
	private Label createLabels(Composite parent) {
		Composite labels = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		labels.setLayout(layout);

		Label listLabel = new Label(labels, SWT.NONE);
		listLabel
				.setText(WorkbenchMessages.FilteredItemsSelectionDialog_listLabel);

		listLabel.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit) {
					e.detail = SWT.TRAVERSE_NONE;
					list.getTable().setFocus();
				}
			}
		});

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		listLabel.setLayoutData(gd);

		progressLabel = new Label(labels, SWT.RIGHT);
		progressLabel.setLayoutData(gd);

		labels.setLayoutData(gd);
		return listLabel;
	}

	private void createViewMenu(Composite parent) {
		toolBar = new ToolBar(parent, SWT.FLAT);
		toolItem = new ToolItem(toolBar, SWT.PUSH, 0);

		GridData data = new GridData();
		data.horizontalAlignment = GridData.END;
		toolBar.setLayoutData(data);

		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				showViewMenu();
			}
		});

		toolItem.setImage(WorkbenchImages
				.getImage(IWorkbenchGraphicConstants.IMG_LCL_VIEW_MENU));
		toolItem
				.setToolTipText(WorkbenchMessages.FilteredItemsSelectionDialog_menu);
		toolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});

		menuManager = new MenuManager();

		fillViewMenu(menuManager);

		IHandlerService service = (IHandlerService) PlatformUI.getWorkbench()
				.getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) {
				showViewMenu();
				return null;
			}
		};
		showViewHandler = service.activateHandler(
				IWorkbenchCommandConstants.WINDOW_SHOW_VIEW_MENU, handler,
				new ActiveShellExpression(getShell()));
	}

	/**
	 * Fills the menu of the dialog.
	 *
	 * @param menuManager
	 *           the menu manager
	 */
	protected void fillViewMenu(IMenuManager menuManager) {
		IDialogSettings settings = getDialogSettings();
		toggleCaseSensitiveAction = new ToggleCaseSensitiveAction(settings);
		menuManager.add(toggleCaseSensitiveAction);
		toggleKeepOpenAction = new ToggleKeepOpenAction(settings);
		menuManager.add(toggleKeepOpenAction);
	}

	private void showViewMenu() {
		Menu menu = menuManager.createContextMenu(getShell());
		Rectangle bounds = toolItem.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

    /**
     * Hook that allows to add actions to the context menu.
	 * <p>
	 * Subclasses may extend in order to add other actions.</p>
     *
     * @param menuManager the context menu manager
     * @since 3.5
     */
	protected void fillContextMenu(IMenuManager menuManager) {
//		List selectedElements= ((StructuredSelection)list.getSelection()).toList();
//
//		Object item= null;
//
//		for (Iterator it= selectedElements.iterator(); it.hasNext();) {
//			item= it.next();
//			if (item instanceof ItemsListSeparator || !isHistoryElement(item)) {
//				return;
//			}
//		}

//		if (selectedElements.size() > 0) {
//			removeHistoryItemAction.setText(WorkbenchMessages.FilteredItemsSelectionDialog_removeItemsFromHistoryAction);
//
//			menuManager.add(removeHistoryActionContributionItem);
//
//		}
	}

	private void createPopupMenu() {
//		removeHistoryItemAction = new RemoveHistoryItemAction();
//		removeHistoryActionContributionItem = new ActionContributionItem(
//				removeHistoryItemAction);

		contextMenuManager = new MenuManager();
		contextMenuManager.setRemoveAllWhenShown(true);
		contextMenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Table table = list.getTable();
		Menu menu= contextMenuManager.createContextMenu(table);
		table.setMenu(menu);
	}

//	/**
//	 * Creates an extra content area, which will be located above the details.
//	 *
//	 * @param parent
//	 *           parent to create the dialog widgets in
//	 * @return an extra content area
//	 */
//	protected abstract Control createExtendedContentArea(Composite parent);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);
		
		dialogArea.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				QuickSearchDialog.this.dispose();
			}
		});

		Composite content = createNestedComposite(dialogArea, 1, false);
		GridData gd = new GridData(GridData.FILL_BOTH);
		content.setLayoutData(gd);
		
		final Label headerLabel = createHeader(content);
		
		Composite inputRow = createNestedComposite(content, 10, true);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(inputRow);
		pattern = new Text(inputRow, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		pattern.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result = LegacyActionTools.removeMnemonics(headerLabel
						.getText());
			}
		});
		GridDataFactory.fillDefaults().span(6,1).grab(true, false).applyTo(pattern);

		Composite searchInComposite = createNestedComposite(inputRow, 2, false);
		GridDataFactory.fillDefaults().span(4,1).grab(true, false).applyTo(searchInComposite);
		Label searchInLabel = new Label(searchInComposite, SWT.NONE);
		searchInLabel.setText(" In: ");
		searchIn = new Text(searchInComposite, SWT.SINGLE | SWT.BORDER | SWT.ICON_CANCEL);
		searchIn.setToolTipText("Search in (comma-separated list of '.gitignore' style inclusion patterns)");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(searchIn);

		final Label listLabel = createLabels(content);

		sashForm = new SashForm(content, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);

		list = new TableViewer(sashForm, (multi ? SWT.MULTI : SWT.SINGLE) |
				SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
//		ColumnViewerToolTipSupport.enableFor(list, ToolTip.NO_RECREATE);

		list.getTable().setHeaderVisible(true);
		list.getTable().setLinesVisible(true);
		list.getTable().getAccessible().addAccessibleListener(
				new AccessibleAdapter() {
					public void getName(AccessibleEvent e) {
						if (e.childID == ACC.CHILDID_SELF) {
							e.result = LegacyActionTools
									.removeMnemonics(listLabel.getText());
						}
					}
				});
		list.setContentProvider(contentProvider);
//		new ScrollListener(list.getTable().getVerticalBar());
//		new SelectionChangedListener(list);
		
		TableViewerColumn col = new TableViewerColumn(list, SWT.RIGHT);
		col.setLabelProvider(LINE_NUMBER_LABEL_PROVIDER);
		col.getColumn().setText("Line");
		col.getColumn().setWidth(40);
		col = new TableViewerColumn(list, SWT.LEFT);
		col.getColumn().setText("Text");
		col.setLabelProvider(LINE_TEXT_LABEL_PROVIDER);
		col.getColumn().setWidth(400);
		col = new TableViewerColumn(list, SWT.LEFT);
		col.getColumn().setText("Path");
		col.setLabelProvider(LINE_FILE_LABEL_PROVIDER);
		col.getColumn().setWidth(150);

		new TableResizeHelper(list).enableResizing();

		//list.setLabelProvider(getItemsListLabelProvider());
		list.setInput(new Object[0]);
		list.setItemCount(contentProvider.getNumberOfElements());
		gd = new GridData(GridData.FILL_BOTH);
		applyDialogFont(list.getTable());
		gd.heightHint= list.getTable().getItemHeight() * 15;
		list.getTable().setLayoutData(gd);

		createPopupMenu();

		pattern.addModifyListener(e -> {
			applyFilter(false);
		});
		
		searchIn.addModifyListener(e -> {
			applyPathMatcher();
		});

		pattern.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (list.getTable().getItemCount() > 0) {
						list.getTable().setFocus();
						list.getTable().select(0);
						//programatic selection may not fire selection events so...
						refreshDetails();
					}
				}
			}
		});

		list.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event
						.getSelection();
				handleSelected(selection);
			}
		});

		list.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});

		list.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {

				if (e.keyCode == SWT.ARROW_UP && (e.stateMask & SWT.SHIFT) == 0
						&& (e.stateMask & SWT.CTRL) == 0) {
					StructuredSelection selection = (StructuredSelection) list
							.getSelection();

					if (selection.size() == 1) {
						Object element = selection.getFirstElement();
						if (element.equals(list.getElementAt(0))) {
							pattern.setFocus();
						}
						list.getTable().notifyListeners(SWT.Selection,
								new Event());

					}
				}

				if (e.keyCode == SWT.ARROW_DOWN
						&& (e.stateMask & SWT.SHIFT) != 0
						&& (e.stateMask & SWT.CTRL) != 0) {

					list.getTable().notifyListeners(SWT.Selection, new Event());
				}

			}
		});

		createDetailsArea(sashForm);
		sashForm.setWeights(new int[] {5,1});

		applyDialogFont(content);

		restoreDialog(getDialogSettings());

		if (initialPatternText != null) {
			pattern.setText(initialPatternText);
		}

		switch (selectionMode) {
		case CARET_BEGINNING:
			pattern.setSelection(0, 0);
			break;
		case FULL_SELECTION:
			pattern.setSelection(0, initialPatternText.length());
			break;
		}

		// apply filter even if pattern is empty (display history)
		applyFilter(false);

		return dialogArea;
	}

	private Composite createNestedComposite(Composite parent, int numRows, boolean equalRows) {
		Composite nested = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout(numRows, equalRows);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.marginLeft = 0;
			layout.marginRight = 0;
			layout.horizontalSpacing = 0;
			nested.setLayout(layout);
		}
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nested);
		return nested;
	}

	protected void dispose() {
		if (blankImage!=null) {
			blankImage.dispose();
			blankImage = null;
		}
	}

	private void createDetailsArea(Composite parent) {
		details = new StyledText(parent, SWT.MULTI+SWT.READ_ONLY+SWT.BORDER+SWT.H_SCROLL);
		details.setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(details);


//		details = new SourceViewer(parent, null, SWT.READ_ONLY+SWT.MULTI+SWT.BORDER);
//		details.getTextWidget().setText("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
//		details.setEditable(false);
//
//		IPreferenceStore prefs = EditorsPlugin.getDefault().getPreferenceStore();
//		TextSourceViewerConfiguration sourceViewerConf = new TextSourceViewerConfiguration(prefs);
//		details.configure(sourceViewerConf);
//
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(details);
//		Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
//		details.getTextWidget().setFont(font);


		list.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				refreshDetails();
			}
		});
		details.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				refreshDetails();
			}
		});
	}


	// Dumber version just using the a 'raw' StyledText widget.
	private void refreshDetails() {
		if (details!=null && list!=null && !list.getTable().isDisposed()) {
			if (documents==null) {
				documents = new DocumentFetcher();
			}
			IStructuredSelection sel = (IStructuredSelection) list.getSelection();
			if (sel==null || sel.isEmpty()) {
				details.setText("");
			} else {
				//Not empty selection
				int numLines = computeLines();
				if (numLines > 0) {
					LineItem item = (LineItem) sel.getFirstElement();
					IDocument document = documents.getDocument(item.getFile());
					try {
						int line = item.getLineNumber()-1; //in document lines are 0 based. In search 1 based.
						int start = document.getLineOffset(Math.max(line-(numLines-1)/2, 0));
						int end = document.getLength();
						try {
							end = document.getLineOffset(start+numLines);
						} catch (BadLocationException e) {
							//Presumably line number is past the end of document.
							//ignore.
						}

						StyledString styledString = highlightMatches(document.get(start, end-start));
						details.setText(styledString.getString());
						details.setStyleRanges(styledString.getStyleRanges());

						return;
					} catch (BadLocationException e) {
					}
				}
			}
			//empty selection or some error:
			details.setText("");
		}
	}

	/**
	 * Computes how much lines of text can be displayed in the details section based on
	 * its current height and font metrics.
	 */
	private int computeLines() {
		if (details!=null && !details.isDisposed()) {
			GC gc = new GC(details);
			try {
				FontMetrics fm = gc.getFontMetrics();
				int itemH = fm.getHeight();
				int areaH = details.getClientArea().height;
				return (areaH+itemH-1) / itemH;
			} finally {
				gc.dispose();
			}
		}
		return 0;
	}

	/**
	 * Helper function to highlight all the matches for the current query in a given piece
	 * of text.
	 *
	 * @return StyledString instance.
	 */
	private StyledString highlightMatches(String visibleText) {
		StyledString styledText = new StyledString(visibleText);
		List<TextRange> matches = getQuery().findAll(visibleText);
		for (TextRange m : matches) {
			styledText.setStyle(m.getOffset(), m.getLength(), HIGHLIGHT_STYLE);
		}
		return styledText;
	}

// Version using sourceviewer
//	private void refreshDetails() {
//		if (details!=null && list!=null && !list.getTable().isDisposed()) {
//			if (documents==null) {
//				documents = new DocumentFetcher();
//			}
//			IStructuredSelection sel = (IStructuredSelection) list.getSelection();
//			if (sel!=null && !sel.isEmpty()) {
//				//Not empty selection
//				LineItem item = (LineItem) sel.getFirstElement();
//				IDocument document = documents.getDocument(item.getFile());
//				try {
//					int line = item.getLineNumber()-1; //in document lines are 0 based. In search 1 based.
//					int start = document.getLineOffset(Math.max(line-2, 0));
//					int end = document.getLength();
//					try {
//						end = document.getLineOffset(line+3);
//					} catch (BadLocationException e) {
//						//Presumably line number is past the end of document.
//						//ignore.
//					}
//					details.setDocument(document, start, end-start);
//
//					String visibleText = document.get(start, end-start);
//					List<TextRange> matches = getQuery().findAll(visibleText);
//					Region visibleRegion = new Region(start, end-start);
//					TextPresentation presentation = new TextPresentation(visibleRegion, 20);
//					presentation.setDefaultStyleRange(new StyleRange(0, document.getLength(), null, null));
//					for (TextRange m : matches) {
//						presentation.addStyleRange(new StyleRange(m.start+start, m.len, null, YELLOW));
//					}
//					details.changeTextPresentation(presentation, true);
//
//					return;
//				} catch (BadLocationException e) {
//				}
//			}
//			details.setDocument(null);
//		}
//	}

	/**
	 * Handle selection in the items list by updating labels of selected and
	 * unselected items and refresh the details field using the selection.
	 *
	 * @param selection
	 *           the new selection
	 */
	protected void handleSelected(StructuredSelection selection) {
		IStatus status = new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
				IStatus.OK, EMPTY_STRING, null);

		updateStatus(status);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_BOUNDS_SETTINGS);
		if (section == null) {
			section = settings.addNewSection(DIALOG_BOUNDS_SETTINGS);
			section.put(DIALOG_HEIGHT, 500);
			section.put(DIALOG_WIDTH, 600);
		}
		return section;
	}

	/**
	 * Returns the dialog settings. Returned object can't be null.
	 *
	 * @return return dialog settings for this dialog
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = IDEWorkbenchPlugin.getDefault()
				.getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = IDEWorkbenchPlugin.getDefault().getDialogSettings()
					.addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	/**
	 * Has to be called in UI thread.
	 */
	public void refreshWidgets() {
		if (list != null && !list.getTable().isDisposed()) {
//			ScrollBar sb = list.getTable().getVerticalBar();
//			int oldScroll = sb.getSelection();
			int itemCount = contentProvider.getNumberOfElements();
			list.setItemCount(itemCount);
			list.refresh(true, false);
			Button goButton = getButton(GO_BUTTON_ID);
			if (goButton!=null && !goButton.isDisposed()) {
				//Even if no element is selected. The dialog should be have as if the first
				//element in the list is selected. So the button is enabled if any
				//element is available in the list.
				goButton.setEnabled(itemCount>0);
			}
			
//			int newScroll = sb.getSelection();
//			if (oldScroll!=newScroll) {
//				System.out.println("Scroll moved in refresh: "+oldScroll+ " => " + newScroll);
//			}
			//sb.setSelection((int) Math.floor(oldScroll*sb.getMaximum()));
		}
//
// The code below attempts to preserve selection, but it also messes up the
// scroll position (reset to 0) in the common case where selection is first element
// and more elements are getting added as I scroll down to bottom of list.
//
//
//			list.getTable().deselectAll();
//
//			list.setItemCount(contentProvider.getNumberOfElements());
//			list.refresh(/*updateLabels*/true, /*reveal*/false);
//
//			if (list.getTable().getItemCount() > 0) {
//				// preserve previous selection
//				if (lastRefreshSelection != null) {
//					if (lastRefreshSelection.size() > 0) {
//						list.setSelection(new StructuredSelection(lastRefreshSelection), false);
//					}
//				} else {
//					list.setSelection(StructuredSelection.EMPTY, false);
//				}
//			} else {
//				list.setSelection(StructuredSelection.EMPTY);
//			}
//		}
	}

	/**
	 * Schedule refresh job.
	 */
	public void scheduleRefresh() {
		refreshJob.schedule();
//		list.re
//		refreshCacheJob.cancelAll();
//		refreshCacheJob.schedule();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		List objectsToReturn = ((StructuredSelection) list.getSelection())
				.toList();
		if (objectsToReturn.isEmpty()) {
			//Pretend that the first element is selected.
			Object first = list.getElementAt(0);
			if (first!=null) {
				objectsToReturn = Arrays.asList(first);
			}
		}
		setResult(objectsToReturn);
	}

	
	
	/**
	 * Handles double-click of items, but *also* by pressing the 'enter' key. 
	 */
	protected void handleDoubleClick() {
		goButtonPressed();
	}

	protected void refreshButtonPressed() {
		applyFilter(true);
	}

	/**
	 * Handles directly clicking the 'go' button.
	 */
	protected void goButtonPressed() {
		computeResult();
		openSelection();
		if (!toggleKeepOpenAction.isChecked()) {
			close();
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int buttonId = (int) ((Button)e.widget).getData();
				if (buttonId==GO_BUTTON_ID) {
					goButtonPressed();
				} else if (buttonId==REFRESH_BUTTON_ID) {
					refreshButtonPressed();
				}
			}
		};
		createButton(parent, GO_BUTTON_ID, "Go!", false).addSelectionListener(listener);
		createButton(parent, REFRESH_BUTTON_ID, "Refresh", false).addSelectionListener(listener);
		refreshWidgets();
	}

	/**
	 * Sets the initial pattern used by the filter. This text is copied into the
	 * selection input on the dialog. A full selection is used in the pattern
	 * input field.
	 *
	 * @param text
	 *           initial pattern for the filter
	 * @see QuickSearchDialog#FULL_SELECTION
	 */
	public void setInitialPattern(String text) {
		setInitialPattern(text, FULL_SELECTION);
	}

	/**
	 * Sets the initial pattern used by the filter. This text is copied into the
	 * selection input on the dialog. The <code>selectionMode</code> is used
	 * to choose selection type for the input field.
	 *
	 * @param text
	 *           initial pattern for the filter
	 * @param selectionMode
	 *           one of: {@link QuickSearchDialog#NONE},
	 *           {@link QuickSearchDialog#CARET_BEGINNING},
	 *           {@link QuickSearchDialog#FULL_SELECTION}
	 */
	public void setInitialPattern(String text, int selectionMode) {
		this.initialPatternText = text;
		this.selectionMode = selectionMode;
	}

	/**
	 * Gets initial pattern.
	 *
	 * @return initial pattern, or <code>null</code> if initial pattern is not
	 *        set
	 */
	protected String getInitialPattern() {
		return this.initialPatternText;
	}

	/**
	 * Returns the current selection.
	 *
	 * @return the current selection
	 */
	protected StructuredSelection getSelectedItems() {

		StructuredSelection selection = (StructuredSelection) list
				.getSelection();

		List selectedItems = selection.toList();

		return new StructuredSelection(selectedItems);
	}

	/**
	 * Validates the item. When items on the items list are selected or
	 * deselected, it validates each item in the selection and the dialog status
	 * depends on all validations.
	 *
	 * @param item
	 *           an item to be checked
	 * @return status of the dialog to be set
	 */
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, QuickSearchActivator.PLUGIN_ID, "fine");
	}

	/**
	 * Creates an instance of a filter.
	 *
	 * @return a filter for items on the items list. Can be <code>null</code>,
	 *        no filtering will be applied then, causing no item to be shown in
	 *        the list.
	 */
	protected QuickTextQuery createFilter() {
		return new QuickTextQuery(pattern.getText(), toggleCaseSensitiveAction.isChecked());
	}

	/**
	 * Applies the filter created by <code>createFilter()</code> method to the
	 * items list. When new filter is different than previous one it will cause
	 * refiltering.
	 * <p>
	 * The 'force' parameter forces a full refresh of the search results / filter even
	 * when the filter is unchanged, or when a incremenal filtering optimisation could be
	 * applied based on query structure. (The use case for this, is to trigger forced refresh
	 * because the underlying resources may have changed).
	 */
	protected void applyFilter(boolean force) {
		QuickTextQuery newFilter = createFilter();
		if (this.searcher==null) {
			if (!newFilter.isTrivial()) {
				//Create the QuickTextSearcher with the inital query.
				this.searcher = new QuickTextSearcher(newFilter, context.createPriorityFun(), MAX_LINE_LEN, new QuickTextSearchRequestor() {
					@Override
					public void add(LineItem match) {
						contentProvider.add(match);
						contentProvider.refresh();
					}
					@Override
					public void clear() {
						contentProvider.reset();
						contentProvider.refresh();
					}
					@Override
					public void revoke(LineItem match) {
						contentProvider.remove(match);
						contentProvider.refresh();
					}
					@Override
					public void update(LineItem match) {
						contentProvider.refresh();
					}
				});
				applyPathMatcher();
				refreshWidgets();
			}
//			this.list.setInput(input)
		} else {
			//The QuickTextSearcher is already active update the query
			this.searcher.setQuery(newFilter, force);
		}
		if (progressJob!=null) {
			progressJob.schedule();
		}
	}

	private void applyPathMatcher() {
		if (this.searcher!=null) {
			this.searcher.setPathMatcher(ResourceMatchers.commaSeparatedPaths(searchIn.getText()));
		}
	}



	/**
	 * Returns name for then given object.
	 *
	 * @param item
	 *           an object from the content provider. Subclasses should pay
	 *           attention to the passed argument. They should either only pass
	 *           objects of a known type (one used in content provider) or make
	 *           sure that passed parameter is the expected one (by type
	 *           checking like <code>instanceof</code> inside the method).
	 * @return name of the given item
	 */
	public String getElementName(Object item) {
		return ""+item;
//		return (String)item; // Assuming the items are strings for now
	}

	/**
	 * Collects filtered elements. Contains one synchronized, sorted set for
	 * collecting filtered elements.
	 * Implementation of <code>ItemsFilter</code> is used to filter elements.
	 * The key function of filter used in to filtering is
	 * <code>matchElement(Object item)</code>.
	 * <p>
	 * The <code>ContentProvider</code> class also provides item filtering
	 * methods. The filtering has been moved from the standard TableView
	 * <code>getFilteredItems()</code> method to content provider, because
	 * <code>ILazyContentProvider</code> and virtual tables are used. This
	 * class is responsible for adding a separator below history items and
	 * marking each items as duplicate if its name repeats more than once on the
	 * filtered list.
	 */
	private class ContentProvider implements IStructuredContentProvider, ILazyContentProvider {

		private List items;

		/**
		 * Creates new instance of <code>ContentProvider</code>.
		 */
		public ContentProvider() {
			this.items = Collections.synchronizedList(new ArrayList(2048));
//			this.duplicates = Collections.synchronizedSet(new HashSet(256));
//			this.lastSortedItems = Collections.synchronizedList(new ArrayList(
//					2048));
		}

		public void remove(LineItem match) {
			this.items.remove(match);
		}

		/**
		 * Removes all content items and resets progress message.
		 */
		public void reset() {
			this.items.clear();
		}

		/**
		 * Adds filtered item.
		 *
		 * @param match
		 */
		public void add(LineItem match) {
			this.items.add(match);
		}

		/**
		 * Refresh dialog.
		 */
		public void refresh() {
			scheduleRefresh();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return items.toArray();
		}

		public int getNumberOfElements() {
			return items.size();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
		 *     java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.viewers.ILazyContentProvider#updateElement(int)
		 */
		public void updateElement(int index) {

			QuickSearchDialog.this.list.replace((items
					.size() > index) ? items.get(index) : null,
					index);

		}

	}

	/**
	 * Get the control where the search pattern is entered. Any filtering should
	 * be done using an {@link ItemsFilter}. This control should only be
	 * accessed for listeners that wish to handle events that do not affect
	 * filtering such as custom traversal.
	 *
	 * @return Control or <code>null</code> if the pattern control has not
	 *        been created.
	 */
	public Control getPatternControl() {
		return pattern;
	}

	public QuickTextQuery getQuery() {
		return searcher.getQuery();
	}

}
