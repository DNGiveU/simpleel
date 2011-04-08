/**
 * Project: simple-el
 * 
 * File Created at 2010-12-2
 * $Id$
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.simpleEL.eval;

/**
 * 
 */

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.alibaba.simpleEL.Expr;

/**
 * @author shaojin.wensj
 *
 */
public final class CompiledExprWrapper implements Expr {
	private final String expr;
	private final Expr compiledExpr;
	private final AtomicLong evalCount = new AtomicLong();
	private final AtomicLong errorCount = new AtomicLong();
	private final AtomicLong evalTimeNano = new AtomicLong();
	private final Exception compileError;

	public CompiledExprWrapper(String expr, Expr compiledExpr) {
		this.expr = expr;
		this.compiledExpr = compiledExpr;
		this.compileError = null;
	}

	public CompiledExprWrapper(String expr, Exception compileError) {
		this.expr = expr;
		this.compileError = compileError;
		this.compiledExpr = null;
	}

	public Exception getCompileError() {
		return compileError;
	}

	public String getExpr() {
		return expr;
	}

	public Expr getCompiledExpr() {
		return compiledExpr;
	}

	public long getEvalCount() {
		return evalCount.get();
	}

	public long getEvalTimeNano() {
		return evalTimeNano.get();
	}

	public static CompositeType getCompositeType() throws OpenDataException {
		OpenType<?>[] indexTypes = new OpenType<?>[] { SimpleType.STRING, SimpleType.LONG, SimpleType.LONG,
				SimpleType.LONG };

		String[] indexNames = { "expr", "evalCount", "errorCount", "evalTimeNano" };
		String[] indexDescriptions = indexNames;

		return new CompositeType("CompiledExprStat", "CompiledExprStat", indexNames, indexDescriptions, indexTypes);
	}

	public CompositeData toCompositeData() throws OpenDataException {
		Map<String, Object> items = new HashMap<String, Object>();

		items.put("expr", expr);
		items.put("evalCount", getEvalCount());
		items.put("errorCount", getErrorCount());
		items.put("evalTimeNano", getEvalTimeNano());

		return new CompositeDataSupport(getCompositeType(), items);
	}

	public static TabularData toTabularData(Iterable<CompiledExprWrapper> itemList) throws JMException {
		TabularType tabularType = getTabularType();
		TabularDataSupport tabularData = new TabularDataSupport(tabularType);

		for (CompiledExprWrapper item : itemList) {
			tabularData.put(item.toCompositeData());
		}

		return tabularData;
	}

	private static TabularType tabularType = null;

	public static TabularType getTabularType() throws OpenDataException {
		if (tabularType != null) {
			return tabularType;
		}

		CompositeType rowType = getCompositeType();
		String[] indexNames = rowType.keySet().toArray(new String[rowType.keySet().size()]);

		tabularType = new TabularType("CompiledExprStatList", "CompiledExprStatList", rowType, indexNames);

		return tabularType;
	}

	public long getErrorCount() {
		return errorCount.get();
	}

	@Override
	public Object eval(Map<String, Object> ctx) throws Exception {
		evalCount.incrementAndGet();

		long startNano = System.nanoTime();

		try {
			if (this.compileError != null) {
				throw compileError;
			}

			return compiledExpr.eval(ctx);
		} catch (Exception ex) {
			errorCount.incrementAndGet();
			throw ex;
		} finally {
			evalTimeNano.addAndGet(System.nanoTime() - startNano);
		}
	}
}