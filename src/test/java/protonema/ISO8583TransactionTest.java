package protonema;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.transaction.Context;
import org.junit.jupiter.api.Test;
import protonema.participant.NetworkParticipant;
import protonema.participant.SenderResponseParticipant;
import protonema.participant.TransactionResponseParticipant;
import protonema.selector.Switch;
import protonema.service.ISO8583ServerService;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ISO8583TransactionTest {

    @Inject
    ISO8583ServerService isoServerService;

    @Inject
    NetworkParticipant networkParticipant;

    @Inject
    TransactionResponseParticipant transactionResponseParticipant;

    @Inject
    SenderResponseParticipant senderResponseParticipant;

    @Inject
    Switch switchSelector;

    @Test
    public void testPackagerLoading() throws Exception {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("packager/iso87ascii.xml");
        assertNotNull(is, "Packager resource iso87ascii.xml must be present");
        GenericPackager packager = new GenericPackager(is);
        assertNotNull(packager);

        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0800");
        msg.set(11, "123456");
        msg.set(70, "301");

        byte[] packed = msg.pack();
        assertNotNull(packed);
        assertTrue(packed.length > 0);
    }

    @Test
    public void testSwitchSelector() throws Exception {
        SimpleConfiguration config = new SimpleConfiguration();
        config.put("0100", "Inquiry");
        config.put("0200", "Transaction");
        config.put("0800", "Network");
        switchSelector.setConfiguration(config);

        Context ctx = new Context();
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100");
        ctx.put(Constants.REQUEST_KEY, msg);

        String selectedGroup = switchSelector.select(100L, ctx);
        assertEquals("Inquiry", selectedGroup);

        msg.setMTI("0800");
        assertEquals("Network", switchSelector.select(101L, ctx));
    }

    @Test
    public void testNetworkParticipant() throws Exception {
        Context ctx = new Context();
        ISOMsg respMsg = new ISOMsg();
        respMsg.setMTI("0810");
        ctx.put(Constants.RESPONSE_KEY, respMsg);

        int result = networkParticipant.prepare(1L, ctx);
        assertEquals(NetworkParticipant.PREPARED, result);

        ISOMsg updatedResp = (ISOMsg) ctx.get(Constants.RESPONSE_KEY);
        assertEquals("00", updatedResp.getString(39));
    }

    @Test
    public void testTransactionResponseParticipant() throws Exception {
        Context ctx = new Context();
        ISOMsg respMsg = new ISOMsg();
        respMsg.setMTI("0210");
        ctx.put(Constants.RESPONSE_KEY, respMsg);

        int result = transactionResponseParticipant.prepare(1L, ctx);
        assertEquals(TransactionResponseParticipant.PREPARED, result);

        ISOMsg updatedResp = (ISOMsg) ctx.get(Constants.RESPONSE_KEY);
        assertEquals("89", updatedResp.getString(39));
    }

    @Test
    public void testSenderResponseParticipantPrepare() throws Exception {
        Context ctx = new Context();
        ISOMsg respMsg = new ISOMsg();
        respMsg.setMTI("0210");
        ctx.put(Constants.RESPONSE_KEY, respMsg);

        int result = senderResponseParticipant.prepare(1L, ctx);
        assertEquals(SenderResponseParticipant.PREPARED, result);

        ISOMsg updatedResp = (ISOMsg) ctx.get(Constants.RESPONSE_KEY);
        assertEquals("06", updatedResp.getString(39));
    }
}
