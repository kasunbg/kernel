/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import org.h2.test.TestBase;
import org.h2.util.JdbcUtils;
import org.h2.util.New;

/**
 * A multi-threaded test case.
 */
public class TestMultiThreadedKernel extends TestBase {

    /**
     * Stop the current thread.
     */
    volatile boolean stop;

    /**
     * The exception that occurred in the thread.
     */
    Exception exception;

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    public void test() throws Exception {
        if (config.mvcc) {
            return;
        }
        deleteDb("multiThreadedKernel");
        testCache();
        deleteDb("multiThreadedKernel");
        final String url = getURL("multiThreadedKernel;DB_CLOSE_DELAY=-1;MULTI_THREADED=1", true);
        final String user = getUser(), password = getPassword();
        int len = 3;
        Thread[] threads = new Thread[len];
        for (int i = 0; i < len; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    Connection conn = null;
                    try {
                        for (int j = 0; j < 100 && !stop; j++) {
                            conn = DriverManager.getConnection(url, user, password);
                            Statement stat = conn.createStatement();
                            stat.execute("create local temporary table temp(id identity)");
                            stat.execute("insert into temp values(1)");
                            conn.close();
                        }
                    } catch (Exception e) {
                        exception = e;
                    } finally {
                        JdbcUtils.closeSilently(conn);
                    }
                }
            });
        }
        for (int i = 0; i < len; i++) {
            threads[i].start();
        }
        Thread.sleep(1000);
        stop = true;
        for (int i = 0; i < len; i++) {
            threads[i].join();
        }
        Connection conn = DriverManager.getConnection(url, user, password);
        conn.createStatement().execute("shutdown");
        conn.close();
        if (exception != null) {
            throw exception;
        }
        deleteDb("multiThreadedKernel");
    }

    private void testCache() throws Exception {
        ArrayList<Thread> list = New.arrayList();
        int size = 3;
        final int count = 100;
        final boolean[] stopped = { false };
        String url = getURL("multiThreadedKernel;MULTI_THREADED=TRUE;CACHE_SIZE=1", true);
        for (int i = 0; i < size; i++) {
            final Connection conn = DriverManager.getConnection(url, getUser(), getPassword());
            if (i == 0) {
                Statement stat = conn.createStatement();
                stat.execute("create table test(id int primary key, name varchar) "
                        + "as select x, space(3000) from system_range(1, " + count + ")");
            }
            final Random random = new Random(i);
            Thread t = new Thread() {
                public void run() {
                    try {
                        PreparedStatement prep = conn.prepareStatement(
                                "select * from test where id = ?");
                        while (!stopped[0]) {
                            prep.setInt(1, random.nextInt(count));
                            prep.execute();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
            list.add(t);
        }
        Thread.sleep(1000);
        stopped[0] = true;
        for (Thread t : list) {
            t.join();
        }
    }

}
