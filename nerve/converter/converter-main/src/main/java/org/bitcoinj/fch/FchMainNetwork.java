package org.bitcoinj.fch;

import org.bitcoinj.params.MainNetParams;

public class FchMainNetwork extends MainNetParams {

    public  FchMainNetwork(){
        addressHeader=35;
    }

    public  static FchMainNetwork MAINNETWORK=new FchMainNetwork();
}
