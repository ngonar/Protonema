package protonema;

import jakarta.enterprise.context.ApplicationScoped;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Protonema2 implements ISORequestListener {

    private static final Logger LOG = LoggerFactory.getLogger(Protonema2.class);

    @Override
    @SuppressWarnings("unchecked")
    public boolean process(ISOSource isos, ISOMsg isomsg) {
        try {
            Space<String, Object> sp = SpaceFactory.getSpace("tspace:umam");

            ISOMsg m = new ISOMsg();
            m.setMTI("0800");
            m.set(11, "000001");
            m.set(41, "29110001");
            m.set(70, "301");

            sp.out("umam-send", m);
            ISOMsg response = (ISOMsg) sp.in("umam-receive", 60000);

            if (response != null) {
                LOG.info("Received umam response: {}", new String(response.pack()));
                isomsg.setResponseMTI();
                isomsg.set(39, response.getString(39));
            } else {
                isomsg.setResponseMTI();
                isomsg.set(39, "68"); // Response timeout
            }

            isos.send(isomsg);
        } catch (Exception ex) {
            LOG.error("Error processing Protonema2 request", ex);
        }

        return true;
    }
}
