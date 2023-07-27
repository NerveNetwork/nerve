package io.nuls.transaction.sort;

import io.nuls.transaction.tx.OrphanTxSortTest;
import io.nuls.transaction.utils.NewSorter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eva
 */
public class TestSorter {

    private List<TestSortData> getData0() {
        List<TestSortData> dataList = new ArrayList<>();
        dataList.add(new TestSortData("4", "3"));
        dataList.add(new TestSortData("1", "0"));
        dataList.add(new TestSortData("3", "2"));
        dataList.add(new TestSortData("2", "1"));
        return dataList;
    }

    private List<TestSortData> getData1() {
        List<TestSortData> dataList = new ArrayList<>();
        dataList.add(new TestSortData("1", "0"));
        dataList.add(new TestSortData("2", "1", "0"));
        dataList.add(new TestSortData("3", "2", "1"));
        dataList.add(new TestSortData("4", "30"));
        dataList.add(new TestSortData("5", "3"));
        dataList.add(new TestSortData("6", "4", "5"));
        return dataList;
    }

    private List<TestSortData> getData2() {
        List<TestSortData> dataList = new ArrayList<>();
        dataList.add(new TestSortData("1", "0"));
        dataList.add(new TestSortData("2", "1"));
        dataList.add(new TestSortData("3", "2", "1"));
        dataList.add(new TestSortData("4", "3", "2", "1"));
        dataList.add(new TestSortData("5", "3", "2", "1"));
        dataList.add(new TestSortData("6", "3", "2", "1"));
        dataList.add(new TestSortData("7", "5", "6", "4"));

        return dataList;
    }

    private List<TestSortData> getData3() {
        List<TestSortData> dataList = new ArrayList<>();
        dataList.add(new TestSortData("1"));
        dataList.add(new TestSortData("2"));
        dataList.add(new TestSortData("3"));
        dataList.add(new TestSortData("4", "1", "3"));
        dataList.add(new TestSortData("5", "2"));
        dataList.add(new TestSortData("6", "4", "5"));
        dataList.add(new TestSortData("x", "1", "3","z"));
        dataList.add(new TestSortData("y", "2","x"));
        dataList.add(new TestSortData("z", "4", "y"));
        dataList.add(new TestSortData("7"));
        dataList.add(new TestSortData("8"));
        dataList.add(new TestSortData("9"));
        dataList.add(new TestSortData("a"));
        dataList.add(new TestSortData("b"));
        dataList.add(new TestSortData("c"));
        dataList.add(new TestSortData("d", "6", "7"));
        return dataList;
    }

    private List<TestSortData> getData4() {
        List<TestSortData> dataList = new ArrayList<>();
        dataList.add(new TestSortData("a", "9"));
        dataList.add(new TestSortData("c", "b"));
        dataList.add(new TestSortData("d", "c"));
        dataList.add(new TestSortData("2", "1"));
        dataList.add(new TestSortData("5", "4"));
        dataList.add(new TestSortData("f", "e"));
        dataList.add(new TestSortData("6", "5"));
        dataList.add(new TestSortData("b", "a"));
        dataList.add(new TestSortData("3", "2"));
        dataList.add(new TestSortData("4", "3"));
        dataList.add(new TestSortData("7", "6"));
        dataList.add(new TestSortData("8", "7"));
        dataList.add(new TestSortData("1"));
        dataList.add(new TestSortData("9", "8"));
        dataList.add(new TestSortData("e", "d"));
        return dataList;
    }


    @Test
    public void test0() {
        List<TestSortData> dataList = getData0();
        dataList = NewSorter.sort(dataList);
        String result = getResult(dataList);
        assertEquals(result, "1234");


    }

    @Test
    public void test1() {
        List<TestSortData> dataList = getData1();
        dataList = NewSorter.sort(dataList);
        String result = getResult(dataList);
        assertTrue(result.equals("123456") || result.equals("123546"));
    }

    @Test
    public void test2() {
        List<TestSortData> dataList = getData2();
        dataList = NewSorter.sort(dataList);
        String result = getResult(dataList);
        assertTrue(result.startsWith("123") && result.endsWith("7"));

    }

    @Test
    public void test3() {
        List<TestSortData> dataList = getData3();
        dataList = NewSorter.sort(dataList);
        String result = getResult(dataList);

        System.out.println(result);
//        1/3 在4之前
        assertTrue(result.indexOf("1") < result.indexOf("4") && result.indexOf("3") < result.indexOf("4"));

//        2 在5之前
        assertTrue(result.indexOf("2") < result.indexOf("5"));
//        4，5 在6之前
        assertTrue(result.indexOf("4") < result.indexOf("6") && result.indexOf("5") < result.indexOf("6"));
//            6，7在d之前
        assertTrue(result.indexOf("6") < result.indexOf("d") && result.indexOf("7") < result.indexOf("d"));
    }


    @Test
    public void test4() {
        List<TestSortData> dataList = getData4();
        List<TestSortData> txList = new ArrayList<>();
        List<Integer> ide = OrphanTxSortTest.randomIde(dataList.size());
        for (int i = 0; i < dataList.size(); i++) {
            txList.add(dataList.get(ide.get(i)));
        }
        System.out.println(getResult(txList));
        dataList = NewSorter.sort(txList);
        String result = getResult(dataList);
        System.out.println("1111:::  " + result);
        assertTrue(result.equals("123456789abcdef"));

    }

    public String getResult(List<TestSortData> list) {
        StringBuilder ss = new StringBuilder();
        for (TestSortData data : list) {
            ss.append(data.getId());
        }
        return ss.toString();
    }
}
