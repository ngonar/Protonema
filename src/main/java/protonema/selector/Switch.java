package protonema.selector;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.GroupSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protonema.Constants;

import java.io.Serializable;

@ApplicationScoped
public class Switch implements GroupSelector, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(Switch.class);
    private Configuration configuration;

    @Override
    public void setConfiguration(Configuration configuration) throws ConfigurationException {
        this.configuration = configuration;
    }

    @Override
    public String select(long l, Serializable serializable) {
        Context ctx = (Context) serializable;
        ISOMsg resIsoMsg = (ISOMsg) ctx.get(Constants.REQUEST_KEY);
        String selector = "";
        try {
            if (resIsoMsg != null && configuration != null) {
                selector = configuration.get(resIsoMsg.getMTI(), "");
            }
        } catch (Exception e) {
            LOG.error("Error retrieving MTI in Switch selector", e);
        }
        return selector;
    }

    @Override
    public int prepare(long l, Serializable serializable) {
        return PREPARED | ABORTED | NO_JOIN;
    }

    @Override
    public void commit(long l, Serializable serializable) {
    }

    @Override
    public void abort(long l, Serializable serializable) {
    }
}
