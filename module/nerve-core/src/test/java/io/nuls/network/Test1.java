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
package io.nuls.network;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.model.ByteUtils;
import io.nuls.network.model.Node;
import io.nuls.network.model.VersionMessageBodyTest;
import io.nuls.network.model.dto.IpAddress;
import io.nuls.network.model.message.body.VersionMessageBody;
import io.nuls.network.utils.LoggerUtil;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Test1 {

    public static void main(String[] args) throws Exception {
        testa();
        testb();
    }

    private static void testb() throws Exception {
        VersionMessageBodyTest body = new VersionMessageBodyTest();
        body.setAddrMe(new IpAddress("127.0.0.1", 1234));
        body.setAddrYou(new IpAddress("127.0.0.124", 1234));
        body.setBlockHash("abcdefh");
        body.setBlockHeight(10);
        body.setProtocolVersion(1);
        body.setExtend("testb ok");
        byte[] bytes = body.serialize();

        VersionMessageBody test = new VersionMessageBody();
        test.parse(bytes,0);
        System.out.println(test.getExtend());
    }
    private static void testa() throws Exception {
        VersionMessageBody body = new VersionMessageBody();
        body.setAddrMe(new IpAddress("127.0.0.1", 1234));
        body.setAddrYou(new IpAddress("127.0.0.124", 1234));
        body.setBlockHash("abcdefh");
        body.setBlockHeight(10);
        body.setProtocolVersion(1);
        body.setReverseCheck((byte) 1);
        body.setExtend("testa ok");
        byte[] bytes = body.serialize();

        VersionMessageBodyTest test = new VersionMessageBodyTest();
        test.parse(bytes,0);
        System.out.println(test.getExtend());
    }


    @Test
    public void test1() {

        String ip = "ssf_dsfANfsf";
        System.out.println(ip.getBytes().length);
        try {
            InetAddress addr = InetAddress.getByAddress(new byte[]{1, 15, 1, 32});
            System.out.println(addr.getHostAddress());// This is fast

            InetAddress addr2 = InetAddress.getByName("AD80:0000:0000:0000:ABAA:0000:00C2:0002");
            System.out.println(addr2.getHostAddress());// This is fast

            InetAddress addr3 = InetAddress.getByName("0.0.0.0");
            System.out.println(addr3.getHostAddress());// This is fast

//            System.out.println(addr.getHostName());// This is slow
//            System.out.println(new String(addr.getAddress()));
        } catch (UnknownHostException e) {
            LoggerUtil.COMMON_LOG.error(e);
        }

    }

    @Test
    public void test2() {
        byte a[] = {-114, 32, 56, -113};
        System.out.println(ByteUtils.byteToLong(a));

    }

    @Test
    public void test3() {


    }

    @Test
    public void test4() {
//        ThreadUtils.createAndRunThread("mytest",TI.getInstance());

    }

    @Test
    public void test5() {
        Map<String, Node> a = new ConcurrentHashMap<>();
        Node node1 = new Node(1L, "1.1.1.1", 80, 0, 1, false);
        a.put(node1.getId(), node1);
        Map<String, Node> b = new ConcurrentHashMap<>();
        Node node2 = new Node(1L, "3.1.1.1", 80, 0, 1, false);
        b.put(node2.getId(), node2);
        b.put(node1.getId(), node1);
        a.clear();
        Collection<Node> c = b.values();
        for (Node i : c) {
            System.out.println(i.getId());

        }
    }

    @Test
    public void test6() {
        List<Node> list = new ArrayList<>();
        Node node1 = new Node(1L, "1.1.1.1", 80, 0, 1, false);
        list.add(node1);
        Node node2 = new Node(1L, "3.1.1.1", 80, 0, 1, false);
        list.add(node2);
        List<Node> list2 = new ArrayList<>();
        list2.add(node1);
        list2.add(node2);
        list.clear();
        for (Node i : list2) {
            System.out.println(i.getId());
        }
    }

    @Test
    public void test7() {
        String key = "dfs";
        Map<String, Integer> cacheConnectGroupIpInMap = new ConcurrentHashMap<>();
        cacheConnectGroupIpInMap.merge(key, 1, (a, b) -> a + b);
        System.out.println(cacheConnectGroupIpInMap.get(key));
        cacheConnectGroupIpInMap.merge(key, 1, (a, b) -> a + b);
        System.out.println(cacheConnectGroupIpInMap.get(key));


    }

    @Test
    public void test8() {
        VersionMessageBodyTest body = new VersionMessageBodyTest();
        body.setAddrMe(new IpAddress("127.0.0.1", 1234));
        body.setAddrYou(new IpAddress("127.0.0.124", 1234));
        body.setBlockHash("abcdefh");
        body.setBlockHeight(10);
        body.setProtocolVersion(1);

        try {
            byte[] bytes = body.serialize();

            VersionMessageBody body1 = new VersionMessageBody();
            body1.parse(new NulsByteBuffer(bytes));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
