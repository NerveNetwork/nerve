/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsHash;
import io.nuls.block.model.GenesisBlock;
import io.nuls.block.test.BlockGenerator;
import io.nuls.core.crypto.HexUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BlockGeneratorTest {

    /**
     * 测试区块生成器生成区块的连续性
     * @throws Exception
     */
    @Test
    public void generate() throws Exception {
        int start = 1;
        int count = 10;
        List<Block> blocks = new ArrayList<>();

        GenesisBlock genesisBlock = GenesisBlock.getInstance(0, 0);
        blocks.add(genesisBlock);

        Block preBlock = genesisBlock;
        do{
            Block block = BlockGenerator.generate(preBlock);
            blocks.add(block);
            preBlock = block;
            start++;
        } while (start < count);

        for (int i = 0; i < blocks.size()-1; i++) {
            NulsHash prehash = blocks.get(i).getHeader().getHash();
            NulsHash hash = blocks.get(i+1).getHeader().getPreHash();
            Assert.assertEquals(prehash, hash);
        }
    }

    /**
     * 测试区块生成器生成区块的分叉
     * @throws Exception
     */
    @Test
    public void fork() throws Exception {
        Block root = BlockGenerator.generate(null);
        Block block1 = BlockGenerator.generate(root, 1, "1");
        Block block2 = BlockGenerator.generate(root, 2, "1");
        Assert.assertEquals(root.getHeader().getHash(), block1.getHeader().getPreHash());
        Assert.assertEquals(block1.getHeader().getPreHash(), block2.getHeader().getPreHash());
        Assert.assertNotEquals(block1.getHeader().getHash(), block2.getHeader().getHash());
    }

    @Test
    public void desBlock() throws Exception {
        String blockHex = "24436aec96f6a739788b3fd09a0cb7922956cbcc53b3fe9250951b7230b50be9451580f40e920256e0bedcda7b78158b74279e0293cd03421920f090df80e5ac751f2d5f76531200020000001437d203000600731f2d5f0200010001003c640000210369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c546304402202034c420a3b7acd131ad9f51f1b070beed79204a6460fcb21702112f3843f90302203a275ddb4716c3357ab1d93f1f0df249f0723a648c8a7c78feb892ec8156a9c10100751f2d5f0000020000003200711f2d5f0053f16ab29448150e01b3e597096c32a49f70a3db60f0bea4cad99a7cff08f06e1965001204454e56542a30783863643665323964333638366432346433633230313863656535343632316561306638393331336200fda701210369865ab23a1e4f3434f85cc704723991dbec1cb9c33e93aa02ed75151dfe49c54630440220165ca414c31d3b22b557f0396ed2f911f3934dacb889820774087592837f67d002203b57efaf1a85f82c811ad28c82d8f33d0eedc434f7641daba2cca4fe15ea97bb2102db1a62c168ac3e34d30c6e6beaef0918d39d448fe2a85aed24982e7368e2414d473045022100cceba4691e3cbc6afce90761517e7bb5b2fddd9fc3cff107d413700a95570a6a022077e44888eb814c11fd2d313bfb2e3c492a4d051e72d5334457b852664ee162ab210308ad97a2bf08277be771fc5450b6a0fa26fbc6c1e57c402715b9135d5388594b473045022100ef27566329def8ce2287679218832647f455e0dffba263a677fdac66844b788a02207e5ad6273daf5cf9d215632eeceecf93f6bb3288e306d57c6b40192e2764802621020c60dd7e0016e174f7ba4fc0333052bade8c890849409de7b6f3d26f0ec64528473045022100932263cfb4138eb2c4862c77c5d7ee12f61cb1ae1c4e6f483a60a9d8ff24cc7202200a7a5781c6077ca4f2af6ce8ef941cbb5ed8f5b77a3344826f987e8b4efd103a";
        Block block = new Block();
        block.parse(HexUtil.decode(blockHex), 0);
        System.out.println();
    }
 }