package protonema.participant;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protonema.Constants;

import java.io.Serializable;

@ApplicationScoped
public class NetworkParticipant implements TransactionParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkParticipant.class);

    @Override
    public int prepare(long l, Serializable serializable) {
        Context ctx = (Context) serializable;
        ISOMsg respMsg = (ISOMsg) ctx.get(Constants.RESPONSE_KEY);
        try {
            if (respMsg != null) {
                respMsg.set(39, "00");
                ctx.put(Constants.RESPONSE_KEY, respMsg);
            }
        } catch (Exception e) {
            LOG.error("Error setting response code in NetworkParticipant", e);
        }
        return PREPARED;
    }

    @Override
    public void commit(long l, Serializable serializable) {
    }

    @Override
    public void abort(long l, Serializable serializable) {
    }
}
