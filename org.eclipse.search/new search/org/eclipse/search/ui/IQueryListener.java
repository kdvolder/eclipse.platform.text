/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.search.ui;
/**
 * A listener for changes to the set of search queries. Queries are added by running
 * them via the appropriate methods in the <code>NewSearchUI</code> facade class.<br>
 * see {@link org.eclipse.search.ui.NewSearchUI#runQuery(ISearchQuery)}<br>
 * see {@link org.eclipse.search.ui.NewSearchUI#runQueryInForeground(IRunnableContext, ISearchQuery) }<br>
 * The search UI determines when queries are rerun, stopped or deleted (and will notify
 * interested parties via this interface).
 * This interface may be implemented by clients.
 * 
 * This API is preliminary and subject to change at any time.
 * 
 * @since 3.0
 */
public interface IQueryListener {
	/**
	 * Called when an query has been added to the system.
	 * 
	 * @param query The query that has been added
	 */
	void queryAdded(ISearchQuery query);
	/**
	 * Called when a query has been removed.
	 * 
	 * @param query The query that has been removed
	 */
	void queryRemoved(ISearchQuery query);
	
	/**
	 * Called before an <code>ISearchQuery</code> is starting.
	 * @param query the query about to start
	 */
	void queryStarting(ISearchQuery query);
	/**
	 * Called after an <code>ISearchQuery</code> has finished.
	 * @param query the query that has finished
	 */
	void queryFinished(ISearchQuery query);
}
