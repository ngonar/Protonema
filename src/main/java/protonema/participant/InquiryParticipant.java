package protonema.participant;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protonema.Constants;

import java.io.Serializable;

@ApplicationScoped
public class InquiryParticipant implements TransactionParticipant {

    private static final Logger LOG = LoggerFactory.getLogger(InquiryParticipant.class);

    @Override
    @SuppressWarnings("unchecked")
    public int prepare(long l, Serializable serializable) {
        Context ctx = (Context) serializable;
        ISOMsg reqMsg = (ISOMsg) ctx.get(Constants.REQUEST_KEY);

        String stan = reqMsg != null ? reqMsg.getString(11) : "UNKNOWN";
        String tag = "[" + stan + "] ";

        try {
            if (reqMsg != null) {
                LOG.info("{}Incoming : {}", tag, new String(reqMsg.pack()));

                /* product */
                String pan = reqMsg.getString(2);
                LOG.info("{}PAN : {}", tag, pan);

                /* partner cid */
                String cidSender = reqMsg.getString(33);
                LOG.info("{}CID Sender : {}", tag, cidSender);

                String systemCid = "4567898";
                String systemSpace = "transient:default";
                String systemOut = "biller-send";
                String systemIn = "biller-receive";

                reqMsg.set(33, systemCid);

                /* Sending message to specific mux channel */
                Space<String, Object> sp = SpaceFactory.getSpace(systemSpace);
                sp.out(systemOut, reqMsg, 10000);
                ISOMsg response = (ISOMsg) sp.in(systemIn, 10000);

                if (response != null) {
                    /* returning to partner */
                    response.set(33, cidSender);
                    LOG.info("{}Outgoing : {}", tag, new String(response.pack()));
                    ctx.put(Constants.RESPONSE_KEY, response);
                } else {
                    LOG.warn("{}No response received from biller queue (timeout)", tag);
                    ISOMsg timeoutResp = (ISOMsg) reqMsg.clone();
                    timeoutResp.setResponseMTI();
                    timeoutResp.set(39, "68");
                    timeoutResp.set(33, cidSender);
                    ctx.put(Constants.RESPONSE_KEY, timeoutResp);
                }
            }
        } catch (Exception e) {
            LOG.error("{}Error processing InquiryParticipant", tag, e);
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
