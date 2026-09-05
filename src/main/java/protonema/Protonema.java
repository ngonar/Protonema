package protonema;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Protonema implements ISORequestListener, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(Protonema.class);

    private Configuration configuration;

    @Override
    public void setConfiguration(Configuration configuration) throws ConfigurationException {
        this.configuration = configuration;
    }

    @Override
    public boolean process(ISOSource isoSource, ISOMsg isoMsg) {
        String spaceN = configuration != null ? configuration.get("space", "transient:default") : "transient:default";
        Long timeout = configuration != null ? configuration.getLong("spaceTimeout", 60000L) : 60000L;
        String queueN = configuration != null ? configuration.get("queue", "TXNQueue") : "TXNQueue";

        Context context = new Context();
        Space<String, Context> space = SpaceFactory.getSpace(spaceN);

        try {
            ISOMsg respMsg = (ISOMsg) isoMsg.clone();
            respMsg.setResponseMTI();
            respMsg.set(39, "00");

            context.put(Constants.REQUEST_KEY, isoMsg);
            context.put(Constants.RESPONSE_KEY, respMsg);
            context.put(Constants.RESOURCE_KEY, isoSource);

        } catch (Exception e) {
            LOG.error("Error setting up ISO response clone", e);
        }

        space.out(queueN, context, timeout);
        return false;
    }
}
