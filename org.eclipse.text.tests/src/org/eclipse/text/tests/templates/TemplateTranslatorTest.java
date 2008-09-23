/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.text.tests.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * @since 3.3
 */
public class TemplateTranslatorTest extends TestCase {
	public static Test suite() {
		return new TestSuite(TemplateTranslatorTest.class);
	}

	private TemplateTranslator fTranslator;

	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.3
	 */
	protected void setUp() throws Exception {
		super.setUp();
		fTranslator= new TemplateTranslator();
	}

	public void testNullTemplate() throws Exception {
		try {
			fTranslator.translate((String) null);
			fail();
		} catch (NullPointerException x) {
		}
	}

	public void testEmptyTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("");
		assertNull(fTranslator.getErrorMessage());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(0, vars.length);
		assertEquals("", buffer.getString());
	}

	public void testNoVarTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo bar");
		assertNull(fTranslator.getErrorMessage());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(0, vars.length);
		assertEquals("foo bar", buffer.getString());
	}

	public void testSimpleTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var} bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("var", vars[0].getType());
	}

	public void testMultiTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var} bar ${var} end");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar var end", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(2, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(12, vars[0].getOffsets()[1]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("var", vars[0].getType());
	}

	public void testIllegalSyntax1() throws Exception {
		ensureFailure("foo ${var");
	}

	private void ensureFailure(String template) {
		try {
			fTranslator.translate(template);
			fail();
		} catch (TemplateException e) {
		}
	}

	public void testIllegalSyntax2() throws Exception {
		ensureFailure("foo $");
	}

	public void testIllegalSyntax3() throws Exception {
		ensureFailure("foo ${] } bar");
	}

	public void testDollar() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo $$ bar");
		assertNull(fTranslator.getErrorMessage());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(0, vars.length);
		assertEquals("foo $ bar", buffer.getString());
	}

	public void testEmptyVariable() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${} bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo  bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(0, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("", vars[0].getType());
	}

	/* 3.3 typed template variables */

	public void testTypedTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var:type} bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals("type", vars[0].getType());
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
	}

	public void testParameterizedTypeTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var:type(param)} bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("type", vars[0].getType());
		assertEquals(Collections.singletonList("param"), vars[0].getVariableType().getParams());
	}

	public void testMultiParameterizedTypeTemplate1() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var:type(param)} bar ${var:type(param)} end");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar var end", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(2, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(12, vars[0].getOffsets()[1]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("type", vars[0].getType());
		assertEquals(Collections.singletonList("param"), vars[0].getVariableType().getParams());
	}

	public void testMultiParameterizedTypeTemplate2() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${var:type(param)} bar ${var} end");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar var end", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(2, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(12, vars[0].getOffsets()[1]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("type", vars[0].getType());
		assertEquals(Collections.singletonList("param"), vars[0].getVariableType().getParams());
	}

	public void testIllegallyParameterizedTypeTemplate() throws Exception {
		ensureFailure("foo ${var:type(param)} bar ${var:type(other)} end");
		ensureFailure("foo ${var:type(param)} bar ${var:type} end");
	}

	public void testParameterizedTypeTemplateWithWhitespace() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${ var : type ( param1 , param2 , param3 ) } bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("type", vars[0].getType());
		List params= new ArrayList(2);
		params.add("param1");
		params.add("param2");
		params.add("param3");
		assertEquals(params, vars[0].getVariableType().getParams());
	}

	public void testQualifiedTypeTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${ var : qual.type ( qual.param1, qual.param2 ) } bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("qual.type", vars[0].getType());
		List params= new ArrayList(2);
		params.add("qual.param1");
		params.add("qual.param2");
		assertEquals(params, vars[0].getVariableType().getParams());
	}

	public void testTextParameterTemplate() throws Exception {
		TemplateBuffer buffer= fTranslator.translate("foo ${ var : qual.type ( 'a parameter 1', qual.param2, 'a parameter ''3' ) } bar");
		assertNull(fTranslator.getErrorMessage());
		assertEquals("foo var bar", buffer.getString());
		TemplateVariable[] vars= buffer.getVariables();
		assertEquals(1, vars.length);
		assertEquals("var", vars[0].getName());
		assertEquals(1, vars[0].getOffsets().length);
		assertEquals(4, vars[0].getOffsets()[0]);
		assertEquals(3, vars[0].getLength());
		assertEquals(false, vars[0].isUnambiguous());
		assertEquals("var", vars[0].getDefaultValue());
		assertEquals(1, vars[0].getValues().length);
		assertEquals(vars[0].getDefaultValue(), vars[0].getValues()[0]);
		assertEquals("qual.type", vars[0].getType());
		List params= new ArrayList(3);
		params.add("a parameter 1");
		params.add("qual.param2");
		params.add("a parameter '3");
		assertEquals(params, vars[0].getVariableType().getParams());
	}

	public void testIllegalSyntax4() throws Exception {
		ensureFailure("foo ${var:} bar");
	}

	public void testIllegalSyntax5() throws Exception {
		ensureFailure("foo ${var:type(} bar");
	}

	public void testIllegalSyntax6() throws Exception {
		ensureFailure("foo ${var:type(] )} bar");
	}

	public void testIllegalSyntax7() throws Exception {
		ensureFailure("foo ${var:type((} bar");
	}

}