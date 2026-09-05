package protonema.participant;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protonema.Constants;

import java.io.IOException;
import java.io.Serializable;

@ApplicationScoped
public class SenderResponseParticipant implements TransactionParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(SenderResponseParticipant.class);

    @Override
    public int prepare(long l, Serializable serializable) {
        Context ctx = (Context) serializable;
        ISOMsg respMsg = (ISOMsg) ctx.get(Constants.RESPONSE_KEY);
        if (respMsg != null) {
            String bit39 = respMsg.getString(39);
            if (bit39 == null) {
                try {
                    respMsg.set(39, "06");
                } catch (Exception e) {
                    LOG.error("Error setting default bit 39 in SenderResponseParticipant", e);
                }
            }
            ctx.put(Constants.RESPONSE_KEY, respMsg);
        }
        return PREPARED;
    }

    @Override
    public void commit(long l, Serializable serializable) {
        sendMessage((Context) serializable);
    }

    @Override
    public void abort(long l, Serializable serializable) {
        sendMessage((Context) serializable);
    }

    private void sendMessage(Context context) {
        ISOSource source = (ISOSource) context.get(Constants.RESOURCE_KEY);
        ISOMsg msgResp = (ISOMsg) context.get(Constants.RESPONSE_KEY);
        if (source != null && msgResp != null) {
            try {
                source.send(msgResp);
            } catch (IOException | ISOException e) {
                LOG.error("Error sending response in SenderResponseParticipant", e);
            }
        }
    }
}
