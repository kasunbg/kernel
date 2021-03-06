/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.message.DbException;
import org.h2.util.IntIntHashMap;
import org.h2.util.New;
import org.h2.util.ValueHashMap;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDouble;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;

/**
 * Data stored while calculating an aggregate.
 */
class AggregateData {
    private final int aggregateType;
    private final int dataType;
    private long count;
    private IntIntHashMap distinctHashes;
    private ValueHashMap<AggregateData> distinctValues;
    private Value value;
    private double sum, vpn;
    private ArrayList<Value> list;

    AggregateData(int aggregateType, int dataType) {
        this.aggregateType = aggregateType;
        this.dataType = dataType;
    }

    /**
     * Add a value to this aggregate.
     *
     * @param database the database
     * @param distinct if the calculation should be distinct
     * @param v the value
     */
    void add(Database database, boolean distinct, Value v) {
        if (aggregateType == Aggregate.SELECTIVITY) {
            count++;
            if (distinctHashes == null) {
                distinctHashes = new IntIntHashMap();
            }
            int size = distinctHashes.size();
            if (size > Constants.SELECTIVITY_DISTINCT_COUNT) {
                distinctHashes = new IntIntHashMap();
                sum += size;
            }
            int hash = v.hashCode();
            // the value -1 is not supported
            distinctHashes.put(hash, 1);
            return;
        }
        if (aggregateType == Aggregate.COUNT_ALL) {
            count++;
            return;
        }
        if (v == ValueNull.INSTANCE) {
            return;
        }
        count++;
        if (distinct) {
            if (distinctValues == null) {
                distinctValues = ValueHashMap.newInstance();
            }
            distinctValues.put(v, this);
            return;
        }
        switch (aggregateType) {
        case Aggregate.COUNT:
            return;
        case Aggregate.SUM:
            if (value == null) {
                value = v.convertTo(dataType);
            } else {
                v = v.convertTo(value.getType());
                value = value.add(v);
            }
            break;
        case Aggregate.AVG:
            if (value == null) {
                value = v.convertTo(DataType.getAddProofType(dataType));
            } else {
                v = v.convertTo(value.getType());
                value = value.add(v);
            }
            break;
        case Aggregate.MIN:
            if (value == null || database.compare(v, value) < 0) {
                value = v;
            }
            break;
        case Aggregate.MAX:
            if (value == null || database.compare(v, value) > 0) {
                value = v;
            }
            break;
        case Aggregate.GROUP_CONCAT: {
            if (list == null) {
                list = New.arrayList();
            }
            list.add(v);
            break;
        }
        case Aggregate.STDDEV_POP:
        case Aggregate.STDDEV_SAMP:
        case Aggregate.VAR_POP:
        case Aggregate.VAR_SAMP: {
            // http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
            // http://www.johndcook.com/standard_deviation.html
            double x = v.getDouble();
            if (count == 1) {
                sum = x;
                vpn = 0;
            } else {
                double xs = sum - (x * (count - 1));
                vpn += (xs * xs) / count / (count - 1);
                sum += x;
            }
            break;
        }
        case Aggregate.BOOL_AND:
            v = v.convertTo(Value.BOOLEAN);
            if (value == null) {
                value = v;
            } else {
                value = ValueBoolean.get(value.getBoolean().booleanValue() && v.getBoolean().booleanValue());
            }
            break;
        case Aggregate.BOOL_OR:
            v = v.convertTo(Value.BOOLEAN);
            if (value == null) {
                value = v;
            } else {
                value = ValueBoolean.get(value.getBoolean().booleanValue() || v.getBoolean().booleanValue());
            }
            break;
        default:
            DbException.throwInternalError("type=" + aggregateType);
        }
    }

    ArrayList<Value> getList() {
        return list;
    }

    /**
     * Get the aggregate result.
     *
     * @param database the database
     * @param distinct if distinct is used
     * @return the value
     */
    Value getValue(Database database, boolean distinct) {
        if (distinct) {
            count = 0;
            groupDistinct(database);
        }
        Value v = null;
        switch (aggregateType) {
        case Aggregate.SELECTIVITY: {
            int s = 0;
            if (count == 0) {
                s = 0;
            } else {
                sum += distinctHashes.size();
                sum = 100 * sum / count;
                s = (int) sum;
                s = s <= 0 ? 1 : s > 100 ? 100 : s;
            }
            v = ValueInt.get(s);
            break;
        }
        case Aggregate.COUNT:
        case Aggregate.COUNT_ALL:
            v = ValueLong.get(count);
            break;
        case Aggregate.SUM:
        case Aggregate.MIN:
        case Aggregate.MAX:
        case Aggregate.BOOL_OR:
        case Aggregate.BOOL_AND:
            v = value;
            break;
        case Aggregate.AVG:
            if (value != null) {
                v = divide(value, count);
            }
            break;
        case Aggregate.GROUP_CONCAT:
            return null;
        case Aggregate.STDDEV_POP: {
            if (count < 1) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(Math.sqrt(vpn / count));
            break;
        }
        case Aggregate.STDDEV_SAMP: {
            if (count < 2) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(Math.sqrt(vpn / (count - 1)));
            break;
        }
        case Aggregate.VAR_POP: {
            if (count < 1) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(vpn / count);
            break;
        }
        case Aggregate.VAR_SAMP: {
            if (count < 2) {
                return ValueNull.INSTANCE;
            }
            v = ValueDouble.get(vpn / (count - 1));
            break;
        }
        default:
            DbException.throwInternalError("type=" + aggregateType);
        }
        return v == null ? ValueNull.INSTANCE : v.convertTo(dataType);
    }

    private Value divide(Value a, long by) {
        if (by == 0) {
            return ValueNull.INSTANCE;
        }
        int type = Value.getHigherOrder(a.getType(), Value.LONG);
        Value b = ValueLong.get(by).convertTo(type);
        a = a.convertTo(type).divide(b);
        return a;
    }

    private void groupDistinct(Database database) {
        if (distinctValues == null) {
            return;
        }
        if (aggregateType == Aggregate.COUNT) {
            count = distinctValues.size();
        } else {
            count = 0;
            for (Value v : distinctValues.keys()) {
                add(database, false, v);
            }
        }
    }

}
