/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.core.storage;

import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.BatchOperation;
import io.nuls.core.rockdb.service.RocksDBService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.nuls.core.rockdb.service.RocksDBService.batchPut;
import static io.nuls.core.rockdb.service.RocksDBService.createTable;
import static io.nuls.core.rockdb.service.RocksDBService.delete;
import static io.nuls.core.rockdb.service.RocksDBService.deleteKeys;
import static io.nuls.core.rockdb.service.RocksDBService.destroyTable;
import static io.nuls.core.rockdb.service.RocksDBService.entryList;
import static io.nuls.core.rockdb.service.RocksDBService.get;
import static io.nuls.core.rockdb.service.RocksDBService.keyList;
import static io.nuls.core.rockdb.service.RocksDBService.listTable;
import static io.nuls.core.rockdb.service.RocksDBService.multiGetValueList;
import static io.nuls.core.rockdb.service.RocksDBService.put;
import static io.nuls.core.rockdb.service.RocksDBService.valueList;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by qinyf on 2018/10/10.
 */
public class RocksDBTest {

    private static String table;
    private static String key;

    @Before
    public void test() throws Exception {
        table = "test-table";
        key = "test-key";
        initTest();
        //createTableTest();
        //existTableTest();
        //destroyTableTest();
        //listTableTest();
        //batchTest();
        //getTest();
        //deleteTest();
        //multiGetTest();
        //multiGetValueListTest();
        //keyListTest();
        //valueListTest();
        //entryListTest();
        //batchPutTest();
        //deleteKeysTest();
        //executeBatchTest();
    }

    @Ignore
    @Test
    public void batchTest() throws Exception {
        String value = "testvalue";
        try {
            put(table, "1".getBytes(UTF_8), (value + "1").getBytes(UTF_8));
            put(table, "2".getBytes(UTF_8), (value + "2").getBytes(UTF_8));
            put(table, "3".getBytes(UTF_8), (value + "3").getBytes(UTF_8));
            put(table, "4".getBytes(UTF_8), (value + "4").getBytes(UTF_8));
            put(table, "5".getBytes(UTF_8), (value + "5").getBytes(UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<byte[]> keys = new ArrayList<>();
        keys.add("1".getBytes(UTF_8));
        keys.add("2".getBytes(UTF_8));
        keys.add("6".getBytes(UTF_8));
        keys.add("4".getBytes(UTF_8));
        keys.add("5".getBytes(UTF_8));
        List<byte[]> list = RocksDBManager.getTable(table).multiGetAsList(keys);
        System.out.println(new String(list.get(1), "utf8"));
        System.out.println(list.get(2));
        System.out.println(list.size());
    }

    @Ignore
    @Test
    public void initTest() throws Exception {
        String dataPath = "/Users/pierreluo/IdeaProjects/nerve-network/common/nuls-core-rockdb/src/test/resources/dbpath";
        long start = System.currentTimeMillis();
        RocksDBService.init(dataPath);
        long end = System.currentTimeMillis();
        System.out.println("Database connection initialization test time consumption：" + (end - start) + "ms");
    }

    /**
     * Determine if the data table exists
     */
    @Ignore
    @Test
    public void existTableTest() {
        String tableName = table;//account chain
        boolean result = false;
        try {
            result = RocksDBService.existTable(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(result);
        Assert.assertEquals(true, result);
    }

    /**
     * Create a data table
     */
    @Ignore
    @Test
    public void createTableTest() {
        String tableName = table;//account chain
        boolean result = false;
        try {
            result = RocksDBService.createTable(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(result);
        Assert.assertEquals(true, result);
    }

    /**
     * Delete Data Table
     */
    @Ignore
    @Test
    public void destroyTableTest() {
        String tableName = "user";
        boolean result = false;
        try {
            result = RocksDBService.destroyTable(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(true, result);
    }

    /**
     * Query all table names
     */
    @Ignore
    @Test
    public void listTableTest() {
        String testTable = "testListTable";
        try {
            createTable(testTable);
            String[] tables = listTable();
            boolean exist = false;
            for (String table : tables) {
                if (table.equals(testTable)) {
                    exist = true;
                    //break;
                }
                System.out.println("table: " + table);
            }
            Assert.assertTrue("create - list tables failed.", exist);
            put(testTable, key.getBytes(UTF_8), "testListTable".getBytes(UTF_8));
            String getValue = new String(get(testTable, key.getBytes(UTF_8)), UTF_8);
            Assert.assertEquals("testListTable", getValue);
            destroyTable(testTable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void putTest() {
        String value = "testvalue";
        try {
            put(table, key.getBytes(UTF_8), value.getBytes(UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String getValue = new String(get(table, key.getBytes(UTF_8)), UTF_8);
        Assert.assertEquals(value, getValue);
    }

    @Ignore
    @Test
    public void getTest() {
        String value = "testvalue";
        byte[] getByte = get(table, key.getBytes(UTF_8));
        if (getByte != null) {
            String getValue = new String(getByte, UTF_8);
            System.out.println(getValue);
            Assert.assertEquals(value, getValue);
        }
    }

    @Ignore
    @Test
    public void deleteTest() {
        try {
            delete(table, key.getBytes(UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNull(get(table, key.getBytes(UTF_8)));
    }

    @Ignore
    @Test
    public void multiGetTest() {
        //String value = "testvalue";
        //String getValue = new String(get(table, key.getBytes(UTF_8)), UTF_8);
        try {
            put(table, "key1".getBytes(), "value1".getBytes());
            put(table, "key2".getBytes(), "value2".getBytes());
            put(table, "key3".getBytes(), "value3".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<byte[]> keyBytes = new ArrayList<>();
        Map<String, String> result = new HashMap<>();
        keyBytes.add("key1".getBytes());
        keyBytes.add("key2".getBytes());
        keyBytes.add("key3".getBytes());
        //keyBytes sizeCannot be greater than65536Otherwise, the query result will be empty
        //Map<byte[], byte[]> map = multiGet(table, keyBytes);
        //for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
        //    result.put(new String(entry.getKey()), new String(entry.getValue()));
        //    System.out.println(new String(entry.getKey()) + "==" + new String(entry.getValue()));
        //}
    }

    @Ignore
    @Test
    public void multiGetValueListTest() {
        try {
            put(table, "key1".getBytes(), "value11".getBytes());
            put(table, "key2".getBytes(), "value22".getBytes());
            put(table, "key3".getBytes(), "value33".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<byte[]> keyBytes = new ArrayList<>();
        Map<String, String> result = new HashMap<>();
        keyBytes.add("key1".getBytes());
        keyBytes.add("key2".getBytes());
        keyBytes.add("key3".getBytes());
        List<byte[]> list = multiGetValueList(table, keyBytes);
        for (byte[] value : list) {
            System.out.println(new String(value));
        }
    }

    @Ignore
    @Test
    public void keyListTest() {
        long start = System.currentTimeMillis();
        List<byte[]> list = keyList(table);
        long end = System.currentTimeMillis();
        if (list != null) {
            System.out.println(list.size() + "Query testing time：" + (end - start) + "ms");
            for (byte[] value : list) {
                System.out.println(new String(value));
            }
        }
    }

    @Ignore
    @Test
    public void valueListTest() {
        List<byte[]> list = valueList(table);
        for (byte[] value : list) {
            System.out.println(new String(value));
        }
    }

    @Ignore
    @Test
    public void entryListTest() {
        List<Entry<byte[], byte[]>> list = entryList(table);
        for (Entry<byte[], byte[]> entry : list) {
            System.out.println(new String(entry.getKey()) + "===" + new String(entry.getValue()));
        }
    }

    @Ignore
    @Test
    public void batchPutTest() {
        List<byte[]> list = new ArrayList<>();
        Map<byte[], byte[]> insertMap = new HashMap<>();
        Map<byte[], byte[]> updateMap = new HashMap<>();
        for (int i = 0; i < 65536; i++) {
            list.add(randomstr().getBytes());
            insertMap.put(list.get(i), ("rocksDBBatch addition testing-" + i + "-" + System.currentTimeMillis()).getBytes());
            updateMap.put(list.get(i), ("rocksDBBatch modification testing-" + i + "-" + System.currentTimeMillis()).getBytes());
        }
        //Batch Add Test
        {
            long start = System.currentTimeMillis();
            try {
                batchPut(table, insertMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            System.out.println(list.size() + "Time consumption for batch addition testing：" + (end - start) + "ms");
            //System.out.println("last insert entity======" + new String(get(table,list.get(list.size() - 1))));
        }
        //Batch modification testing
        {
            long start = System.currentTimeMillis();
            try {
                batchPut(table, updateMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            System.out.println(list.size() + "Time consumption for batch modification testing：" + (end - start) + "ms");
        }
//        //Batch query testing
//        {
//            long start = System.currentTimeMillis();
//            Map<byte[], byte[]> map = multiGet(table, list);
//            long end = System.currentTimeMillis();
//            System.out.println(map.size() + "Time consumption for batch query testing：" + (end - start) + "ms");
//        }
        //Batch deletion test
        {
            long start = System.currentTimeMillis();
            try {
                deleteKeys(table, list);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            System.out.println(list.size() + "Time consumption for batch deletion testing：" + (end - start) + "ms");
        }
    }

    @Ignore
    @Test
    public void deleteKeysTest() {
        List<byte[]> list = keyList(table);
        if (list != null) {
            long start = System.currentTimeMillis();
            try {
                deleteKeys(table, list);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            System.out.println(list.size() + "Time consumption for batch deletion testing：" + (end - start) + "ms");
            list = keyList(table);
            Assert.assertEquals(0, list.size());
        }
    }

    @Ignore
    @Test
    public void executeBatchTest() {
        //Batch operation testing
        {
            try {
                BatchOperation batch = RocksDBService.createWriteBatch(table);
                batch.put("key111".getBytes(), "value111".getBytes());
                batch.put("key222".getBytes(), "value222".getBytes());
                batch.put("key333".getBytes(), "value333".getBytes());
                batch.put("key444".getBytes(), "value444".getBytes());
                batch.put("key222".getBytes(), "value22222".getBytes());
                batch.delete("key444".getBytes());
                batch.executeBatch();
                System.out.println("query entity======" + new String(get(table, "key222".getBytes())));
                System.out.println("query deleted entity======" + get(table, "key444".getBytes()));

                RocksDBService.createTable("account");
                BatchOperation batch2 = RocksDBService.createWriteBatch("account");
                batch2.put("key111".getBytes(), "value111".getBytes());
                batch2.executeBatch();
                System.out.println("query entity======" + new String(get("account", "key111".getBytes())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Randomly generate non repeatingkey
     *
     * @return
     */
    private static String randomstr() {
        Random ran1 = new Random();
        char buf[] = new char[255];
        int r1 = ran1.nextInt(100);
        for (int i = 0; i < 255; ++i) {
            buf[i] = (char) ('A' + (ran1.nextInt(100) % 26));
        }
        return new String(buf);
    }
}
