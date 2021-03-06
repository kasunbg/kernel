/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.store.PageStore;
import org.h2.table.Column;
import org.h2.value.Value;
import org.h2.value.ValueString;

/**
 * This class represents the statement
 * EXPLAIN
 */
public class Explain extends Prepared {

    private Prepared command;
    private LocalResult result;
    private boolean executeCommand;

    public Explain(Session session) {
        super(session);
    }

    public void setCommand(Prepared command) {
        this.command = command;
    }

    public void prepare() {
        command.prepare();
    }

    public void setExecuteCommand(boolean executeCommand) {
        this.executeCommand = executeCommand;
    }

    public ResultInterface queryMeta() {
        return query(-1);
    }

    public ResultInterface query(int maxrows) {
        Column column = new Column("PLAN", Value.STRING);
        ExpressionColumn expr = new ExpressionColumn(session.getDatabase(), column);
        Expression[] expressions = { expr };
        result = new LocalResult(session, expressions, 1);
        if (maxrows >= 0) {
            String plan;
            if (executeCommand) {
                PageStore store = session.getDatabase().getPageStore();
                store.statisticsStart();
                if (command.isQuery()) {
                    command.query(maxrows);
                } else {
                    command.update();
                }
                plan = command.getPlanSQL();
                Map<String, Integer> statistics = store.statisticsEnd();
                if (statistics != null) {
                    int total = 0;
                    for (Entry<String, Integer> e : statistics.entrySet()) {
                        total += e.getValue();
                    }
                    if (total > 0) {
                        statistics = new TreeMap<String, Integer>(statistics);
                        StringBuilder buff = new StringBuilder();
                        buff.append("total: ").append(total).append('\n');
                        for (Entry<String, Integer> e : statistics.entrySet()) {
                            int value = e.getValue();
                            int percent = (int) (100L * value / total);
                            buff.append(e.getKey()).append(": ").append(value).append(" (").append(percent).append("%)\n");
                        }
                        plan += "\n/*\n" + buff.toString() + "*/";
                    }
                }
            } else {
                plan = command.getPlanSQL();
            }
            add(plan);
        }
        result.done();
        return result;
    }

    private void add(String text) {
        Value[] row = { ValueString.get(text) };
        result.addRow(row);
    }

    public boolean isQuery() {
        return true;
    }

    public boolean isTransactional() {
        return true;
    }

    public boolean isReadOnly() {
        return true;
    }
}
