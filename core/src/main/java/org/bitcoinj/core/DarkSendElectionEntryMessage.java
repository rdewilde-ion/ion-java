package org.bitcoinj.core;

import org.bitcoinj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Hash Engineering on 2/10/2015.
 */
public class DarkSendElectionEntryMessage extends Message {
    private static final Logger log = LoggerFactory.getLogger(DarkSendElectionEntryMessage.class);

    TransactionInput vin;
    MasternodeAddress addr;
    PublicKey pubkey;
    PublicKey pubkey2;
    byte [] vchSig;
    long sigTime;
    int count;
    int current;
    long lastUpdated;
    int protocolVersion;
    public int donationPercentage = 0;
    public Script donationAddress;

    private transient int optimalEncodingMessageSize;


    DarkSendElectionEntryMessage()
    {
        super();
    }

    DarkSendElectionEntryMessage(NetworkParameters params, byte[] payloadBytes)
    {
        super(params, payloadBytes, 0, false, false, payloadBytes.length);
    }
// TODO rdw PeerAddress
    DarkSendElectionEntryMessage(NetworkParameters params, TransactionInput vin, MasternodeAddress addr, byte [] vchSig,  long sigTime, PublicKey pubkey, PublicKey pubkey2, int count, int current, long lastTimeSeen, int protocolVersion)
    {
        super(params);
        this.vin = vin;
        this.addr = addr;
        this.vchSig = vchSig;
        this.sigTime = sigTime;
        this.pubkey = pubkey;
        this.pubkey2 = pubkey2;
        this.count = count;
        this.current = current;
        this.protocolVersion = protocolVersion;

    }


    @Override
    protected void parseLite() throws ProtocolException {
        if (parseLazy && length == UNKNOWN_LENGTH) {
            //If length hasn't been provided this tx is probably contained within a block.
            //In parseRetain mode the block needs to know how long the transaction is
            //unfortunately this requires a fairly deep (though not total) parse.
            //This is due to the fact that transactions in the block's list do not include a
            //size header and inputs/outputs are also variable length due the contained
            //script so each must be instantiated so the scriptlength varint can be read
            //to calculate total length of the transaction.
            //We will still persist will this semi-light parsing because getting the lengths
            //of the various components gains us the ability to cache the backing bytearrays
            //so that only those subcomponents that have changed will need to be reserialized.

            //parse();
            //parsed = true;
            length = calcLength(payload, offset);
            cursor = offset + length;
        }
    }
    protected static int calcLength(byte[] buf, int offset) {
        VarInt varint;
        // jump past version (uint32)
        int cursor = offset;// + 4;
        //vin TransactionInput
        cursor += 36;
        varint = new VarInt(buf, cursor);
        long scriptLen = varint.value;
        // 4 = length of sequence field (unint32)
        cursor += scriptLen + 4 + varint.getOriginalSizeInBytes();

        varint = new VarInt(buf, cursor);
        cursor += varint.getOriginalSizeInBytes() + varint.value;

        varint = new VarInt(buf, cursor);
        cursor += varint.getOriginalSizeInBytes() + varint.value;

        varint = new VarInt(buf, cursor);
        cursor += varint.getOriginalSizeInBytes() + varint.value;

        cursor += 8 + 4 + 4 + 8 + 4;

        return cursor - offset;
    }
    @Override
    void parse() throws ProtocolException {
        if(parsed)
            return;

        cursor = offset;

        optimalEncodingMessageSize = 0;

//        TransactionOutPoint outpoint = new TransactionOutPoint(params, payload, cursor, this, parseLazy, parseRetain);
//        cursor += outpoint.getMessageSize();
//        int scriptLen = (int) readVarInt();
//        byte [] scriptBytes = readBytes(scriptLen);
////        long sequence = readUint32();
//        vin = new TransactionInput(params, null, scriptBytes, outpoint);
        vin = new TransactionInput(params, null, payload, cursor);
        cursor += vin.getMessageSize();

        addr = new MasternodeAddress(params, payload, cursor, CoinDefinition.protocolVersion);
        cursor += addr.getMessageSize();

//        optimalEncodingMessageSize += outpoint.getMessageSize() + scriptLen + VarInt.sizeOf(scriptLen) +4;

        vchSig = readByteArray();

        sigTime = readInt64();

        pubkey = new PublicKey(params, payload, cursor, this, parseLazy, parseRetain);
        cursor += pubkey.getMessageSize();

        pubkey2 = new PublicKey(params, payload, cursor, this, parseLazy, parseRetain);
        cursor += pubkey2.getMessageSize();

        count = (int)readUint32();
        current = (int)readUint32();

        lastUpdated = readInt64();

        protocolVersion = (int)readUint32();
        // DonationAddress CScript
        int scriptLen = (int) readVarInt();
        byte [] scriptBytes = readBytes(scriptLen);
        donationAddress = new Script(scriptBytes);
        cursor += scriptBytes.length;
//
//        // DonationPercentage
        donationPercentage = (int) readUint32();

        length = cursor - offset;


    }
    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {

        vin.bitcoinSerialize(stream);
        pubkey.bitcoinSerialize(stream);
        pubkey2.bitcoinSerialize(stream);

        stream.write(new VarInt(vchSig.length).encode());
        stream.write(vchSig);

        Utils.int64ToByteStreamLE(sigTime, stream);
        Utils.uint32ToByteStreamLE(count, stream);
        Utils.uint32ToByteStreamLE(current, stream);
        Utils.int64ToByteStreamLE(lastUpdated, stream);
        Utils.uint32ToByteStreamLE(protocolVersion, stream);
    }

    long getOptimalEncodingMessageSize()
    {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        maybeParse();
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        optimalEncodingMessageSize = getMessageSize();
        return optimalEncodingMessageSize;
    }

    public String toString()
    {
        return "dsee Message:  " +
                "vin: " + vin.toString() + " - " + addr.toString();

    }
}
